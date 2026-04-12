package top.ilovemyhome.dagtask.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.agent.TaskFactory;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.agent.TaskResultReport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the entire task execution lifecycle across three queues:
 * <ol>
 *   <li>pending - Tasks waiting for an available worker thread (bounded)</li>
 *   <li>running - Tasks currently executing</li>
 *   <li>finished - Completed tasks (success/failure/cancelled/forced-ok)</li>
 * </ol>
 *
 * Handles automatic queue processing: when a worker thread becomes available,
 * the next task is taken from the pending queue and executed.
 */
public class TaskExecutionEngine {

    private final AgentConfiguration config;
    private final AgentSchedulerClient agentSchedulerClient;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;

    // Three state queues + index for O(1) duplicate check
    private final ArrayBlockingQueue<PendingTask> pendingQueue;
    private final Set<Long> pendingTaskIds = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Long, RunningTask> runningTasks;
    private final ConcurrentLinkedQueue<FinishedTask> finishedTasks;
    private final AtomicInteger maxFinishedSize;

    // Background queue processor
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread queueProcessor;

    // Background result reporter thread
    private final AtomicBoolean reporterRunning = new AtomicBoolean(true);
    private final BlockingQueue<TaskResultReport> resultReportQueue;
    private final BlockingQueue<TaskResultReport> deadLetterQueue;
    private Thread resultReporterThread;

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutionEngine.class);
    private static final int DEFAULT_MAX_FINISHED_SIZE = 1000;
    private static final int DEFAULT_REPORT_QUEUE_CAPACITY = 100;
    private static final int DEFAULT_DEAD_LETTER_CAPACITY = 1000;

    /**
     * Record representing a task waiting in the pending queue.
     */
    public record PendingTask(Long taskId, TaskExecution execution, TaskInput input) {}

    /**
     * Record representing a currently running task.
     */
    public record RunningTask(PendingTask pendingTask, Future<?> future) {}

    /**
     * Record representing a finished task.
     */
    public record FinishedTask(Long taskId, boolean completedNormally, boolean success, long durationMs) {}

    /**
     * Statistics snapshot for health reporting.
     */
    public record Statistics(
            int pendingSize,
            int runningSize,
            int finishedSize,
            int supportedExecutionKeysCount,
            List<String> supportedExecutionKeys
    ) {}

    public TaskExecutionEngine(AgentConfiguration config, AgentSchedulerClient agentSchedulerClient,
                               ExecutorService executorService, ObjectMapper objectMapper) {
        this.config = config;
        this.agentSchedulerClient = agentSchedulerClient;
        this.executorService = executorService;
        this.objectMapper = objectMapper;

        this.pendingQueue = new ArrayBlockingQueue<>(config.getMaxPendingTasks());
        this.runningTasks = new ConcurrentHashMap<>();
        this.finishedTasks = new ConcurrentLinkedQueue<>();
        this.maxFinishedSize = new AtomicInteger(DEFAULT_MAX_FINISHED_SIZE);
        this.resultReportQueue = new ArrayBlockingQueue<>(DEFAULT_REPORT_QUEUE_CAPACITY);
        this.deadLetterQueue = new ArrayBlockingQueue<>(DEFAULT_DEAD_LETTER_CAPACITY);
    }

    /**
     * Constructor with custom queue capacities.
     */
    public TaskExecutionEngine(AgentConfiguration config, AgentSchedulerClient agentSchedulerClient,
                               ExecutorService executorService, ObjectMapper objectMapper,
                               int maxFinishedSize, int reportQueueCapacity, int deadLetterCapacity) {
        this.config = config;
        this.agentSchedulerClient = agentSchedulerClient;
        this.executorService = executorService;
        this.objectMapper = objectMapper;

        this.pendingQueue = new ArrayBlockingQueue<>(config.getMaxPendingTasks());
        this.runningTasks = new ConcurrentHashMap<>();
        this.finishedTasks = new ConcurrentLinkedQueue<>();
        this.maxFinishedSize = new AtomicInteger(maxFinishedSize);
        this.resultReportQueue = new ArrayBlockingQueue<>(reportQueueCapacity);
        this.deadLetterQueue = new ArrayBlockingQueue<>(deadLetterCapacity);
    }

    /**
     * Starts the background queue processor that continuously dispatches tasks
     * from the pending queue to the executor service.
     */
    public void start() {
        queueProcessor = new Thread(() -> {
            LOGGER.info("Task execution manager queue processor started");
            while (running.get()) {
                try {
                    processBatched(pendingQueue, pendingTask -> {
                        pendingTaskIds.remove(pendingTask.taskId());
                        try {
                            Future<?> future = executorService.submit(() -> executeTask(pendingTask));
                            runningTasks.put(pendingTask.taskId(), new RunningTask(pendingTask, future));
                            LOGGER.debug("Moved task {} from pending to running", pendingTask.taskId());
                        } catch (RejectedExecutionException e) {
                            // Thread pool is full, all concurrent slots are busy
                            // pendingQueue.put() blocks until there is space - this naturally handles the race
                            LOGGER.warn("Task executor rejected task {}, all concurrent threads are busy, putting back to pending queue", pendingTask.taskId());
                            try {
                                // put() blocks until there is space - exactly what we need
                                pendingQueue.put(pendingTask);
                                // Add back to index
                                pendingTaskIds.add(pendingTask.taskId());
                                // Short sleep to avoid 100% CPU spin when pool is continuously full
                                Thread.sleep(100);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                // If interrupted, drop the task
                                LOGGER.error("Interrupted while waiting to reinsert task {}, dropping", pendingTask.taskId());
                                finishedTasks.add(new FinishedTask(pendingTask.taskId(), false, false, 0));
                                trimFinishedQueueIfNeeded();
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.error("Unexpected error in queue processor, continuing processing", e);
                }
            }
            LOGGER.info("Task execution manager queue processor stopped");
        });
        queueProcessor.setName("task-execution-queue-processor");
        queueProcessor.setDaemon(true);
        queueProcessor.start();

        // Start result reporter thread
        resultReporterThread = new Thread(() -> {
            LOGGER.info("Task result reporter thread started");
            while (reporterRunning.get()) {
                try {
                    processBatched(resultReportQueue, report -> {
                        try {
                            agentSchedulerClient.reportTaskResult(report);
                        } catch (Exception e) {
                            LOGGER.error("Failed to report task result {} to scheduler server, moving to dead letter queue", report.taskId(), e);
                            if (!deadLetterQueue.offer(report)) {
                                LOGGER.error("Dead letter queue full, dropping oldest result to make room");
                                deadLetterQueue.poll();
                                deadLetterQueue.offer(report);
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.error("Unexpected fatal error in result reporter, continuing", e);
                }
            }
            LOGGER.info("Task result reporter thread stopped");
        });
        resultReporterThread.setName("task-result-reporter");
        resultReporterThread.setDaemon(true);
        resultReporterThread.start();
    }

    /**
     * Stops the execution engine gracefully:
     * <ol>
     *   <li>Stops background threads (queue processor and result reporter)</li>
     *   <li>Reports all pending tasks as failed to the scheduler server</li>
     *   <li>Waits for all running tasks to complete execution</li>
     *   <li>Persists any remaining failed reports in dead letter queue to local file (if configured)</li>
     * </ol>
     */
    public void stop() {
        stop(0);
    }

    /**
     * Stops the execution engine gracefully with a timeout for waiting running tasks.
     *
     * @param timeoutMs maximum time to wait for running tasks to complete (in milliseconds).
     *                   If 0, waits indefinitely.
     */
    public void stop(long timeoutMs) {
        LOGGER.info("Initiating graceful shutdown of TaskExecutionEngine");

        // Step 1: Signal background threads to stop
        running.set(false);
        reporterRunning.set(false);

        // Interrupt the background threads if they are blocked on take
        if (queueProcessor != null) {
            queueProcessor.interrupt();
        }
        if (resultReporterThread != null) {
            resultReporterThread.interrupt();
        }

        // Step 2: Report all pending tasks as failed to scheduler
        int pendingCount = pendingQueue.size();
        if (pendingCount > 0) {
            LOGGER.info("Reporting {} pending tasks as failed since agent is shutting down", pendingCount);
            List<PendingTask> remainingPending = new ArrayList<>();
            pendingQueue.drainTo(remainingPending);
            for (PendingTask pending : remainingPending) {
                Long taskId = pending.taskId();
                // Report task as failed - agent shutdown
                var report = new TaskResultReport(
                        config.getAgentId(), taskId, false,
                        "Task was pending when agent shutdown, never executed", null
                );
                try {
                    agentSchedulerClient.reportTaskResult(report);
                } catch (Exception e) {
                    LOGGER.error("Failed to report pending task {} as failed during shutdown", taskId, e);
                    // Still add to dead letter, we'll persist it later
                    if (!deadLetterQueue.offer(report)) {
                        LOGGER.error("Dead letter queue full during shutdown, dropping report for task {}", taskId);
                    }
                }
                pendingTaskIds.remove(taskId);
                finishedTasks.add(new FinishedTask(taskId, false, false, 0));
            }
            trimFinishedQueueIfNeeded();
            LOGGER.info("Finished reporting {} pending tasks", pendingCount);
        }

        // Step 3: Wait for all running tasks to complete
        int runningCount = runningTasks.size();
        if (runningCount > 0) {
            LOGGER.info("Waiting for {} running tasks to complete", runningCount);
            long startTime = System.currentTimeMillis();
            for (RunningTask runningTask : runningTasks.values()) {
                try {
                    if (timeoutMs > 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        long remainingTimeout = timeoutMs - elapsed;
                        if (remainingTimeout > 0) {
                            runningTask.future().get();
                        }
                    } else {
                        runningTask.future().get();
                    }
                } catch (InterruptedException e) {
                    LOGGER.warn("Interrupted while waiting for running task {}", runningTask.pendingTask().taskId());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Execution failed, already handled, just log
                    LOGGER.debug("Task completed with exception during shutdown", e);
                }
            }
            LOGGER.info("All running tasks completed after {}ms", System.currentTimeMillis() - startTime);
        }

        // Step 4: Persist any remaining dead letter entries to local file if configured
        String persistencePath = config.getDeadLetterPersistenceFile();
        if (persistencePath != null && !persistencePath.isBlank() && !deadLetterQueue.isEmpty()) {
            LOGGER.info("Persisting {} failed result reports to dead letter file: {}",
                    deadLetterQueue.size(), persistencePath);
            try {
                persistDeadLetterQueue(persistencePath);
                LOGGER.info("Successfully persisted dead letter queue to file");
            } catch (Exception e) {
                LOGGER.error("Failed to persist dead letter queue to {}", persistencePath, e);
            }
        }

        LOGGER.info("TaskExecutionEngine shutdown complete");
    }

    /**
     * Persists all entries in the dead letter queue to a JSON file.
     * Each line contains one JSON object per failed report.
     */
    private void persistDeadLetterQueue(String filePath) throws Exception {
        List<TaskResultReport> remaining = new ArrayList<>();
        deadLetterQueue.drainTo(remaining);
        // Use ObjectMapper to write as JSON array
        objectMapper.writeValue(new java.io.File(filePath), remaining);
    }

    /**
     * Retries all entries from a persisted dead letter file after restart.
     * Loads reports from file and attempts to report them to the scheduler.
     * Any reports that still fail are returned back to the dead letter queue.
     * @param filePath path to the persisted dead letter file (can be null, uses configured path from config
     * @return number of reports successfully recovered and reported
     * @throws Exception if file reading fails
     */
    public int recoverPersistedDeadLetter(String filePath) throws Exception {
        String path = filePath != null ? filePath : config.getDeadLetterPersistenceFile();
        if (path == null || path.isBlank()) {
            LOGGER.debug("No dead letter persistence file configured, skipping recovery");
            return 0;
        }
        java.io.File file = new java.io.File(path);
        if (!file.exists()) {
            LOGGER.info("No persisted dead letter file found at {}, nothing to recover", path);
            return 0;
        }
        TaskResultReport[] reports = objectMapper.readValue(file, TaskResultReport[].class);
        int successCount = 0;
        for (TaskResultReport report : reports) {
            try {
                agentSchedulerClient.reportTaskResult(report);
                successCount++;
            } catch (Exception e) {
                LOGGER.error("Failed to recover persisted report for task {}, putting back to dead letter queue", report.taskId(), e);
                if (!deadLetterQueue.offer(report)) {
                    LOGGER.error("Dead letter queue full during recovery, dropping report for task {}", report.taskId());
                }
            }
        }
        // Delete the file after successful recovery attempt
        if (file.delete()) {
            LOGGER.debug("Deleted persisted dead letter file after recovery");
        }
        LOGGER.info("Recovered {} out of {} persisted dead letter reports", successCount, reports.length);
        return successCount;
    }

    /**
     * Submits a new task for execution.
     *
     * @param taskId         the task ID
     * @param executionClass the execution class name to instantiate
     * @param inputJson      the input JSON (can be null)
     * @return submission result indicating if accepted and why
     */
    public SubmissionResult submit(Long taskId, String executionClass, String inputJson) {
        // Check for duplicate task ID - O(1) check using indexes
        if (runningTasks.containsKey(taskId) || pendingTaskIds.contains(taskId)) {
            return SubmissionResult.duplicate(taskId);
        }

        // Create execution instance
        TaskExecution execution = TaskFactory.createTaskForExecution(executionClass);
        if (execution == null) {
            return SubmissionResult.executionCreationFailed(executionClass);
        }

        // Parse input
        TaskInput input;
        try {
            input = parseInput(inputJson);
        } catch (Exception e) {
            return SubmissionResult.inputParseFailed(e.getMessage());
        }

        // Try to add to pending queue
        PendingTask pendingTask = new PendingTask(taskId, execution, input);
        boolean added = pendingQueue.offer(pendingTask);
        if (!added) {
            return SubmissionResult.queueFull(config.getMaxPendingTasks(), pendingQueue.size());
        }

        pendingTaskIds.add(taskId);
        LOGGER.info("Task {} added to pending queue. Pending size: {}", taskId, pendingQueue.size());
        return SubmissionResult.accepted(taskId, pendingQueue.size());
    }

    /**
     * Kills a task (can be in pending or running).
     *
     * @param taskId the task ID to kill
     * @return kill result
     */
    public KillResult kill(Long taskId) {
        return cancelOrKill(taskId);
    }

    /**
     * Cancels a task (can be in pending or running).
     * Identical to kill operation provided for semantic consistency.
     *
     * @param taskId the task ID to cancel
     * @return cancel result
     */
    public KillResult cancel(Long taskId) {
        return cancelOrKill(taskId);
    }

    /**
     * Common implementation for both kill and cancel operations.
     */
    private KillResult cancelOrKill(Long taskId) {
        // Check pending queue first using our index for fast check then remove
        if (pendingTaskIds.contains(taskId)) {
            boolean removed = pendingQueue.removeIf(p -> p.taskId().equals(taskId));
            if (removed) {
                pendingTaskIds.remove(taskId);
                LOGGER.info("Task {} removed from pending queue before execution", taskId);
                finishInterrupted(taskId, false);
                return KillResult.successFromPending(taskId);
            }
            pendingTaskIds.remove(taskId); // clean up stale entry
        }

        // Check running
        RunningTask runningTask = runningTasks.remove(taskId);
        if (runningTask == null) {
            return KillResult.notFound(taskId);
        }

        boolean cancelled = runningTask.future().cancel(true);
        if (cancelled) {
            LOGGER.info("Task {} cancelled successfully from running", taskId);
            finishInterrupted(taskId, false);
            return KillResult.successFromRunning(taskId);
        } else {
            LOGGER.warn("Failed to cancel task {}", taskId);
            return KillResult.failedToCancel(taskId);
        }
    }

    /**
     * Forces a task to complete successfully (can be in pending or running).
     * Reports success to dag-server immediately.
     *
     * @param taskId the task ID
     * @return force-ok result
     */
    public ForceOkResult forceOk(Long taskId) {
        // Remove from pending
        if (pendingTaskIds.contains(taskId)) {
            boolean removed = pendingQueue.removeIf(p -> p.taskId().equals(taskId));
            if (removed) {
                pendingTaskIds.remove(taskId);
                LOGGER.info("Task {} force-ok from pending queue", taskId);
                finishInterrupted(taskId, true);
                var report = new TaskResultReport(
                        config.getAgentId(), taskId, true, "{\"forced\":true}", Instant.now()
                );
                agentSchedulerClient.reportTaskResult(report);
                return ForceOkResult.successFromPending(taskId);
            }
            pendingTaskIds.remove(taskId); // clean up stale entry
        }

        // Remove from running
        RunningTask runningTask = runningTasks.remove(taskId);
        if (runningTask != null) {
            runningTask.future().cancel(true);
            LOGGER.info("Task {} force-ok from running", taskId);
            finishInterrupted(taskId, true);
            var report = new TaskResultReport(
                    config.getAgentId(), taskId, true, "{\"forced\":true}", Instant.now()
            );
            agentSchedulerClient.reportTaskResult(report);
            return ForceOkResult.successFromRunning(taskId);
        }

        return ForceOkResult.notFound(taskId);
    }

    /**
     * Finishes a task that was interrupted (kill/cancel/force-ok).
     */
    private void finishInterrupted(Long taskId, boolean success) {
        finishedTasks.add(new FinishedTask(taskId, true, success, 0));
        trimFinishedQueueIfNeeded();
    }

    /**
     * Gets current statistics for health reporting.
     */
    public Statistics getStatistics() {
        return new Statistics(
                pendingQueue.size(),
                runningTasks.size(),
                finishedTasks.size(),
                config.getSupportedExecutionKeys().size(),
                new ArrayList<>(config.getSupportedExecutionKeys())
        );
    }

    /**
     * Executes a task after it has been dequeued.
     * Result is queued for asynchronous reporting to scheduler server.
     */
    private void executeTask(PendingTask pendingTask) {
        Long taskId = pendingTask.taskId();
        TaskExecution execution = pendingTask.execution();
        TaskInput input = pendingTask.input();
        long startTime = System.currentTimeMillis();

        try {
            LOGGER.info("Starting execution of task {}", taskId);
            TaskOutput output = execution.execute(input);
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("Completed execution of task {} in {}ms", taskId, duration);
            var report = new top.ilovemyhome.dagtask.si.agent.TaskResultReport(
                    config.getAgentId(), taskId, output.isSuccess(), serializeOutput(output), null
            );
            // Offer to report queue, if queue full drop it (we already logged completion)
            if (!resultReportQueue.offer(report)) {
                LOGGER.warn("Result report queue full, dropping report for task {}", taskId);
            }
            finishTask(taskId, true, output.isSuccess(), duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.error("Task {} execution failed after {}ms", taskId, duration, e);
            var report = new TaskResultReport(config.getAgentId(), taskId, false, e.getMessage());
            // Offer to report queue, if queue full drop it
            if (!resultReportQueue.offer(report)) {
                LOGGER.warn("Result report queue full, dropping report for task {}", taskId);
            }
            finishTask(taskId, false, false, duration);
        }
    }

    /**
     * Moves a task from running to finished.
     */
    private void finishTask(Long taskId, boolean completedNormally, boolean success, long durationMs) {
        runningTasks.remove(taskId);
        finishedTasks.add(new FinishedTask(taskId, completedNormally, success, durationMs));
        trimFinishedQueueIfNeeded();
        LOGGER.debug("Moved task {} from running to finished", taskId);
    }

    /**
     * Trims the finished queue if it exceeds the maximum size limit.
     * Removes the oldest entries when limit is reached.
     */
    private void trimFinishedQueueIfNeeded() {
        int maxSize = maxFinishedSize.get();
        if (finishedTasks.size() > maxSize) {
            // Remove oldest half to avoid trimming every time
            int toRemove = finishedTasks.size() - maxSize / 2;
            for (int i = 0; i < toRemove && !finishedTasks.isEmpty(); i++) {
                finishedTasks.poll();
            }
            LOGGER.debug("Trimmed finished queue to {} entries", finishedTasks.size());
        }
    }

    /**
     * Sets the maximum size for the finished tasks queue.
     * @param maxSize maximum number of finished tasks to keep
     */
    public void setMaxFinishedSize(int maxSize) {
        this.maxFinishedSize.set(maxSize);
    }

    /**
     * Clears all finished tasks from the finished queue.
     * Can be called periodically to free memory.
     */
    public void clearFinishedQueue() {
        finishedTasks.clear();
        LOGGER.info("Cleared finished tasks queue");
    }

    /**
     * Returns the number of failed reports in the dead letter queue.
     */
    public int getDeadLetterQueueSize() {
        return deadLetterQueue.size();
    }

    /**
     * Retries sending all failed reports from the dead letter queue.
     * Call this after scheduler server connection is restored.
     * @return number of reports successfully retried
     */
    public int retryDeadLetter() {
        int successCount = 0;
        TaskResultReport report;
        while ((report = deadLetterQueue.poll()) != null) {
            try {
                agentSchedulerClient.reportTaskResult(report);
                successCount++;
            } catch (Exception e) {
                LOGGER.error("Failed to retry report for task {}, putting back to dead letter queue", report.taskId(), e);
                // Put back if still failing
                if (!deadLetterQueue.offer(report)) {
                    LOGGER.error("Dead letter queue full during retry, dropping report");
                }
                break; // stop on first failure to avoid looping
            }
        }
        LOGGER.info("Retried dead letter queue, {} reports succeeded", successCount);
        return successCount;
    }

    private TaskInput parseInput(String inputJson) throws Exception {
        if (inputJson == null || inputJson.isBlank()) {
            return new TaskInput(null, null, Map.of());
        }
        Map<String, Object> map = objectMapper.readValue(inputJson, Map.class);
        return new TaskInput(null, map, Map.of());
    }

    private String serializeOutput(TaskOutput output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (Exception e) {
            LOGGER.warn("Failed to serialize output for task", e);
            return output.toString();
        }
    }

    /**
     * Generic batch processing helper for background threads.
     * Takes one blocked on first item, then drains all remaining available items from the queue,
     * processes each with the given consumer.
     * Reduces lock contention compared to taking one at a time.
     */
    private <T> void processBatched(BlockingQueue<T> queue, java.util.function.Consumer<T> processor) throws InterruptedException {
        // Block waiting for at least one item
        T first = queue.take();
        processor.accept(first);

        // Drain all already available items in one go
        List<T> batch = new ArrayList<>();
        int drained = queue.drainTo(batch);
        for (T item : batch) {
            processor.accept(item);
        }
        if (drained > 0) {
            LOGGER.debug("Batch drained {} additional items from queue", drained);
        }
    }
}
