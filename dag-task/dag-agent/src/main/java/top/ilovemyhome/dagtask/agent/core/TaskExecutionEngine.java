package top.ilovemyhome.dagtask.agent.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.TaskResultReportFailException;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.agent.TaskFactory;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Manages the entire task execution lifecycle across three queues:
 * <ol>
 *   <li>pending - Tasks waiting for an available worker thread (bounded)</li>
 *   <li>running - Tasks currently executing</li>
 *   <li>finished - Completed tasks (success/failure/cancelled/forced-ok)</li>
 * </ol>
 * <p>
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

    // Background queue processor
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread queueProcessor;

    // Background result reporter thread
    private final AtomicBoolean reporterRunning = new AtomicBoolean(true);
    private final BlockingQueue<TaskExecuteResult> resultReportQueue;
    private Thread resultReporterThread;

    // Background dead letter retry thread - automatically retries persisted failed reports
    private final AtomicBoolean deadLetterRetryRunning = new AtomicBoolean(true);
    private Thread deadLetterRetryThread;
    // Dead letter persistence - directory mode, each failed batch is a separate file
    private File deadLetterPersistenceDir;

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionEngine.class);
    private static final int DEFAULT_REPORT_QUEUE_CAPACITY = 100;
    // Retry interval for dead letter - 30 seconds
    private static final long DEFAULT_DEAD_LETTER_RETRY_INTERVAL_MS = 30000L;

    /**
     * Record representing a task waiting in the pending queue.
     */
    public record PendingTask(Long taskId, TaskExecution execution, TaskInput input) {
    }

    /**
     * Record representing a currently running task.
     */
    public record RunningTask(PendingTask pendingTask, Future<?> future) {
    }

    /**
     * Statistics snapshot for health reporting.
     */
    public record Statistics(
        int pendingSize,
        int runningSize,
        int supportedExecutionKeysCount,
        List<String> supportedExecutionKeys
    ) {
    }

    public TaskExecutionEngine(AgentConfiguration config, AgentSchedulerClient agentSchedulerClient,
                               ExecutorService executorService, ObjectMapper objectMapper) {
        this.config = config;
        this.agentSchedulerClient = agentSchedulerClient;
        this.executorService = executorService;
        this.objectMapper = objectMapper;

        this.pendingQueue = new ArrayBlockingQueue<>(config.getMaxPendingTasks());
        this.runningTasks = new ConcurrentHashMap<>();
        this.resultReportQueue = new ArrayBlockingQueue<>(DEFAULT_REPORT_QUEUE_CAPACITY);
        initDeadLetterPersistence();
    }

    /**
     * Constructor with custom queue capacities.
     */
    public TaskExecutionEngine(AgentConfiguration config
        , AgentSchedulerClient agentSchedulerClient
        , ExecutorService executorService
        , ObjectMapper objectMapper
        , int reportQueueCapacity) {
        this.config = config;
        this.agentSchedulerClient = agentSchedulerClient;
        this.executorService = executorService;
        this.objectMapper = objectMapper;

        this.pendingQueue = new ArrayBlockingQueue<>(config.getMaxPendingTasks());
        this.runningTasks = new ConcurrentHashMap<>();
        this.resultReportQueue = new ArrayBlockingQueue<>(reportQueueCapacity);
        initDeadLetterPersistence();
    }

    /**
     * Initializes dead letter persistence directory.
     */
    private void initDeadLetterPersistence() {
        String pathConfig = config.getDeadLetterPersistencePath();

        if (pathConfig != null && !pathConfig.isBlank()) {
            this.deadLetterPersistenceDir = new java.io.File(pathConfig);
            // Ensure directory exists
            if (!deadLetterPersistenceDir.exists()) {
                deadLetterPersistenceDir.mkdirs();
            }
            logger.info("Dead letter persistence configured in directory: {}", deadLetterPersistenceDir.getAbsolutePath());
            return;
        }

        // No persistence configured
        this.deadLetterPersistenceDir = null;
        logger.info("No dead letter persistence configured, failed reports will be dropped");
    }

    /**
     * Starts the background queue processor that continuously dispatches tasks
     * from the pending queue to the executor service.
     */
    public void start() {
        queueProcessor = new Thread(() -> {
            logger.info("Task execution manager queue processor started");
            while (running.get()) {
                try {
                    singleProcess(pendingQueue, pendingTask -> {
                        pendingTaskIds.remove(pendingTask.taskId());
                        try {
                            Future<?> future = executorService.submit(() -> executeTask(pendingTask));
                            runningTasks.put(pendingTask.taskId(), new RunningTask(pendingTask, future));
                            logger.debug("Moved task {} from pending to running", pendingTask.taskId());
                        } catch (RejectedExecutionException e) {
                            // Thread pool is full, all concurrent slots are busy
                            // pendingQueue.put() blocks until there is space - this naturally handles the race
                            logger.warn("Task executor rejected task {}, all concurrent threads are busy, putting back to pending queue", pendingTask.taskId());
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
                                logger.error("Interrupted while waiting to reinsert task {}, dropping", pendingTask.taskId());
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Unexpected error in queue processor, continuing processing", e);
                }
            }
            logger.info("Task execution manager queue processor stopped");
        });
        queueProcessor.setName("task-execution-queue-processor");
        queueProcessor.setDaemon(true);
        queueProcessor.start();

        // Start result reporter thread
        resultReporterThread = new Thread(() -> {
            logger.info("Task result reporter thread started");
            while (reporterRunning.get()) {
                try {
                    batchProcess(resultReportQueue, this::reportResults);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Unexpected fatal error in result reporter, continuing", e);
                }
            }
            logger.info("Task result reporter thread stopped");
        });
        resultReporterThread.setName("task-result-reporter");
        resultReporterThread.setDaemon(true);
        resultReporterThread.start();

        // Start dead letter retry thread if persistence is configured
        if (deadLetterPersistenceDir != null) {
            deadLetterRetryThread = new Thread(() -> {
                logger.info("Dead letter retry thread started");
                while (deadLetterRetryRunning.get()) {
                    try {
                        // Load and retry all persisted failed reports
                        int retried = retryPersistedDeadLetterOnce();
                        if (retried > 0) {
                            logger.info("Retried {} persisted dead letter reports", retried);
                        }
                        // Sleep before next retry cycle
                        Thread.sleep(DEFAULT_DEAD_LETTER_RETRY_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        logger.error("Unexpected error in dead letter retry thread, continuing", e);
                        try {
                            Thread.sleep(DEFAULT_DEAD_LETTER_RETRY_INTERVAL_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                logger.info("Dead letter retry thread stopped");
            });
            deadLetterRetryThread.setName("dead-letter-retry");
            deadLetterRetryThread.setDaemon(true);
            deadLetterRetryThread.start();
        }
    }

    private int reportResults(List<TaskExecuteResult> results) {
        try {
            var httpResponse = agentSchedulerClient.reportTaskResult(results);
            if (httpResponse.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new TaskResultReportFailException("Report task result failed with status code " + httpResponse.getStatus());
            }
            return results.size();
        } catch (Exception e) {
            List<Long> taskIds = results.stream().map(TaskExecuteResult::taskId).toList();
            logger.error("Failed to report task result {} to scheduler server, persisting to dead letter file", taskIds, e);
            persistToDeadLetterFile(results);
        }
        return 0;
    }

    /**
     * Stops the execution engine gracefully:
     * <ol>
     *   <li>Stops background threads (queue processor, result reporter, dead letter retry)</li>
     *   <li>Reports all pending tasks as failed to the scheduler server</li>
     *   <li>Waits for all running tasks to complete execution</li>
     * </ol>
     */
    public void stop() {
        stop(0);
    }

    /**
     * Stops the execution engine gracefully with a timeout for waiting running tasks.
     *
     * @param timeoutMs maximum time to wait for running tasks to complete (in milliseconds).
     *                  If 0, waits indefinitely.
     */
    public void stop(long timeoutMs) {
        logger.info("Initiating graceful shutdown of TaskExecutionEngine");

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
            logger.info("Reporting {} pending tasks as failed since agent is shutting down", pendingCount);
            List<PendingTask> remainingPending = new ArrayList<>();
            pendingQueue.drainTo(remainingPending);

            List<TaskExecuteResult> results = null;
            try {
                results = remainingPending.stream().map(p -> TaskExecuteResult.of(config.getAgentId(), p.taskId, false, "Task was pending when agent shutdown, never executed"))
                    .toList();
                agentSchedulerClient.reportTaskResult(results);
                pendingTaskIds.clear();
            } catch (Exception e) {
                logger.error("Failed to report pending task {} as failed during shutdown", results, e);
                // Persist directly to dead letter file
                persistToDeadLetterFile(results);
            }
            logger.info("Finished reporting {} pending tasks", pendingCount);
        }

        // Step 3: Wait for all running tasks to complete
        int runningCount = runningTasks.size();
        if (runningCount > 0) {
            logger.info("Waiting for {} running tasks to complete", runningCount);
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
                    logger.warn("Interrupted while waiting for running task {}", runningTask.pendingTask().taskId());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Execution failed, already handled, just log
                    logger.debug("Task completed with exception during shutdown", e);
                }
            }
            logger.info("All running tasks completed after {}ms", System.currentTimeMillis() - startTime);
        }

        // Step 4: Stop the dead letter retry thread
        deadLetterRetryRunning.set(false);
        if (deadLetterRetryThread != null) {
            deadLetterRetryThread.interrupt();
        }

        logger.info("TaskExecutionEngine shutdown complete");
    }

    /**
     * Persists failed results to dead letter directory.
     * Each failed batch creates a new unique file.
     */
    private void persistToDeadLetterFile(List<TaskExecuteResult> results) {
        List<Long> taskIds = results.stream().map(TaskExecuteResult::taskId).toList();

        // Directory mode - new file per batch
        if (deadLetterPersistenceDir != null) {
            try {
                // Generate unique filename: YYYYMMDD-HHMMss-<UUID>.json
                String timestamp = java.time.format.DateTimeFormatter
                    .ofPattern("yyyyMMdd-HHmmss")
                    .format(java.time.LocalDateTime.now());
                String filename = timestamp + "-" + java.util.UUID.randomUUID() + ".json";
                File newFile = new java.io.File(deadLetterPersistenceDir, filename);

                // Write the entire batch as JSON
                FileWriter writer = new FileWriter(newFile);
                String json = objectMapper.writeValueAsString(results);
                writer.write(json);
                writer.close();
                logger.debug("Persisted failed results for task {} to dead letter file {}", taskIds, filename);
            } catch (Exception e) {
                logger.error("Failed to persist failed results for task {} to dead letter directory", taskIds, e);
            }
            return;
        }

        // No persistence configured
        logger.debug("No dead letter persistence configured, dropping failed results for tasks {}", taskIds);
    }

    /**
     * Retries all persisted failed reports once from the dead letter directory.
     * Processes each file individually, deletes after processing regardless of outcome.
     *
     * @return number of reports successfully reported
     */
    private int retryPersistedDeadLetterOnce() {
        // Directory mode
        if (deadLetterPersistenceDir != null) {
            return retryPersistedInDirectory();
        }

        return 0;
    }

    /**
     * Retry processing in directory mode: list all files, process each one, delete after processing.
     */
    private int retryPersistedInDirectory() {
        File[] files = deadLetterPersistenceDir.listFiles();
        if (Objects.isNull(files)) {
            return 0;
        }
        int successTotal = 0;
        for (File file : files) {
            if (!file.isFile() || file.getName().startsWith(".")) {
                continue; // skip directories and hidden files
            }

            try {
                // Read the entire file content
                String json = java.nio.file.Files.readString(file.toPath());
                if (json.isBlank()) {
                    Files.deleteIfExists(file.toPath());
                    continue;
                }
                // Parse as list of TaskExecuteResult
                List<TaskExecuteResult> reports = objectMapper.readValue(json, new TypeReference<>() {});
                successTotal += reportResults(reports);
                // Always delete the file after processing - failures get re-persisted as new files
                Files.deleteIfExists(file.toPath());
            } catch (Exception e) {
                logger.error("Error processing dead letter file {}, will skip for now", file.getName(), e);
            }
        }
        return successTotal;
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
        logger.info("Task {} added to pending queue. Pending size: {}", taskId, pendingQueue.size());
        return SubmissionResult.accepted(taskId, pendingQueue.size());
    }

    /**
     * Kills a task (can be in pending or running).
     *
     * @param taskId the task ID to kill
     * @return kill result
     */
    public KillResult kill(Long taskId) {
        // Check pending queue first using our index for fast check then remove
        if (pendingTaskIds.contains(taskId)) {
            boolean removed = pendingQueue.removeIf(p -> p.taskId().equals(taskId));
            if (removed) {
                pendingTaskIds.remove(taskId);
                logger.info("Task {} removed from pending queue before execution", taskId);
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
            logger.info("Task {} killed successfully from running", taskId);
            finishInterrupted(taskId, false);
            return KillResult.successFromRunning(taskId);
        } else {
            logger.warn("Failed to kill task {}", taskId);
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
                logger.info("Task {} force-ok from pending queue", taskId);
                finishInterrupted(taskId, true);
                var report = new TaskExecuteResult(
                    config.getAgentId(), taskId, true, "{\"forced\":true}", Instant.now()
                );
                agentSchedulerClient.reportTaskResult(List.of(report));
                return ForceOkResult.successFromPending(taskId);
            }
            pendingTaskIds.remove(taskId); // clean up stale entry
        }

        // Remove from running
        RunningTask runningTask = runningTasks.remove(taskId);
        if (runningTask != null) {
            runningTask.future().cancel(true);
            logger.info("Task {} force-ok from running", taskId);
            finishInterrupted(taskId, true);
            var report = new TaskExecuteResult(
                config.getAgentId(), taskId, true, "{\"forced\":true}", Instant.now()
            );
            agentSchedulerClient.reportTaskResult(List.of(report));
            return ForceOkResult.successFromRunning(taskId);
        }

        return ForceOkResult.notFound(taskId);
    }

    /**
     * Forces a task to complete as failed (can be in pending or running).
     * Reports failure to dag-server immediately.
     *
     * @param taskId the task ID
     * @return force-nok result
     */
    public ForceNokResult forceNok(Long taskId) {
        // Remove from pending
        if (pendingTaskIds.contains(taskId)) {
            boolean removed = pendingQueue.removeIf(p -> p.taskId().equals(taskId));
            if (removed) {
                pendingTaskIds.remove(taskId);
                logger.info("Task {} force-nok from pending queue", taskId);
                finishInterrupted(taskId, false);
                var report = new TaskExecuteResult(
                    config.getAgentId(), taskId, false, "{\"forced\":false}", Instant.now()
                );
                agentSchedulerClient.reportTaskResult(List.of(report));
                return ForceNokResult.successFromPending(taskId);
            }
            pendingTaskIds.remove(taskId); // clean up stale entry
        }

        // Remove from running
        RunningTask runningTask = runningTasks.remove(taskId);
        if (runningTask != null) {
            runningTask.future().cancel(true);
            logger.info("Task {} force-nok from running", taskId);
            finishInterrupted(taskId, false);
            var report = new TaskExecuteResult(
                config.getAgentId(), taskId, false, "{\"forced\":false}", Instant.now()
            );
            agentSchedulerClient.reportTaskResult(List.of(report));
            return ForceNokResult.successFromRunning(taskId);
        }

        return ForceNokResult.notFound(taskId);
    }

    /**
     * Finishes a task that was interrupted (kill/force-ok/force-nok).
     */
    private void finishInterrupted(Long taskId, boolean success) {
        // Task already removed from running or pending, nothing more to do
    }

    /**
     * Gets current statistics for health reporting.
     */
    public Statistics getStatistics() {
        return new Statistics(
            pendingQueue.size(),
            runningTasks.size(),
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
            logger.info("Starting execution of task {}", taskId);
            TaskOutput output = execution.execute(input);
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Completed execution of task {} in {}ms", taskId, duration);
            var report = new TaskExecuteResult(
                config.getAgentId(), taskId, output.isSuccess(), serializeOutput(output), null
            );
            // Offer to report queue, if queue full persist directly to dead letter (don't drop)
            if (!resultReportQueue.offer(report)) {
                logger.warn("Result report queue full, persisting report for task {} directly to dead letter", taskId);
                persistToDeadLetterFile(List.of(report));
            }
            finishTask(taskId, true, output.isSuccess(), duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Task {} execution failed after {}ms", taskId, duration, e);
            var report = new TaskExecuteResult(config.getAgentId(), taskId, false, e.getMessage());
            // Offer to report queue, if queue full persist directly to dead letter (don't drop)
            if (!resultReportQueue.offer(report)) {
                logger.warn("Result report queue full, persisting report for task {} directly to dead letter", taskId);
                persistToDeadLetterFile(List.of(report));
            }
            finishTask(taskId, false, false, duration);
        }
    }

    /**
     * Moves a task from running to finished.
     */
    private void finishTask(Long taskId, boolean completedNormally, boolean success, long durationMs) {
        runningTasks.remove(taskId);
        logger.debug("Moved task {} from running to finished", taskId);
    }

    /**
     * Retries sending all failed reports from the dead letter.
     * Call this after scheduler server connection is restored.
     *
     * @return number of reports successfully retried
     */
    public int retryDeadLetter() {
        return retryPersistedDeadLetterOnce();
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
            logger.warn("Failed to serialize output for task", e);
            return output.toString();
        }
    }

    /**
     * Generic batch processing helper for background threads.
     * Takes one blocked on first item, then drains all remaining available items from the queue,
     * processes each with the given consumer.
     * Reduces lock contention compared to taking one at a time.
     */
    private <T> void singleProcess(BlockingQueue<T> queue, Consumer<T> processor) throws InterruptedException {
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
            logger.debug("Batch drained {} additional items from queue", drained);
        }
    }


    private <T> void batchProcess(BlockingQueue<T> queue, Consumer<List<T>> processor) throws InterruptedException {
        // Block waiting for at least one item
        T first = queue.take();
        List<T> batch = new ArrayList<>();
        batch.add(first);
        int drained = queue.drainTo(batch);
        logger.debug("Batch drained {} additional items from queue", drained);
        if (!batch.isEmpty()) {
            processor.accept(batch);
        }
    }
}
