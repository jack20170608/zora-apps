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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final ExecutorService taskExecutor;
    private final ObjectMapper objectMapper;
    private final TaskFactory taskFactory;

    // Three state queues
    private final ArrayBlockingQueue<PendingTask> pendingQueue;
    private final ConcurrentHashMap<Long, RunningTask> runningTasks;
    private final ConcurrentLinkedQueue<FinishedTask> finishedTasks;

    // Background queue processor
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread queueProcessor;

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutionEngine.class);

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
                               ExecutorService taskExecutor, ObjectMapper objectMapper, TaskFactory taskFactory) {
        this.config = config;
        this.agentSchedulerClient = agentSchedulerClient;
        this.taskExecutor = taskExecutor;
        this.objectMapper = objectMapper;
        this.taskFactory = taskFactory;

        this.pendingQueue = new ArrayBlockingQueue<>(config.getMaxPendingTasks());
        this.runningTasks = new ConcurrentHashMap<>();
        this.finishedTasks = new ConcurrentLinkedQueue<>();
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
                    PendingTask pendingTask = pendingQueue.take();
                    Future<?> future = taskExecutor.submit(() -> executeTask(pendingTask));
                    RunningTask runningTask = new RunningTask(pendingTask, future);
                    runningTasks.put(pendingTask.taskId(), runningTask);
                    LOGGER.debug("Moved task {} from pending to running", pendingTask.taskId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            LOGGER.info("Task execution manager queue processor stopped");
        });
        queueProcessor.setDaemon(true);
        queueProcessor.start();
    }

    /**
     * Stops the queue processor.
     */
    public void stop() {
        running.set(false);
        if (queueProcessor != null) {
            queueProcessor.interrupt();
        }
    }

    /**
     * Submits a new task for execution.
     *
     * @param taskId         the task ID
     * @param inputJson      the input JSON (can be null)
     * @return submission result indicating if accepted and why
     */
    public SubmissionResult submit(Long taskId, String inputJson) {
        // Check for duplicate task ID
        if (runningTasks.containsKey(taskId)) {
            return SubmissionResult.duplicate(taskId);
        }
        boolean alreadyInPending = pendingQueue.stream()
                .anyMatch(p -> p.taskId().equals(taskId));
        if (alreadyInPending) {
            return SubmissionResult.duplicate(taskId);
        }

        // Create execution instance
//        TaskExecution execution = taskFactory.createTaskForExecution(executionClass);
//        if (execution == null) {
//            return SubmissionResult.executionCreationFailed(executionClass);
//        }

        // Parse input
        TaskInput input;
        try {
            input = parseInput(inputJson);
        } catch (Exception e) {
            return SubmissionResult.inputParseFailed(e.getMessage());
        }

        // Try to add to pending queue
        PendingTask pendingTask = new PendingTask(taskId, null, input);
        boolean added = pendingQueue.offer(pendingTask);
        if (!added) {
            return SubmissionResult.queueFull(config.getMaxPendingTasks(), pendingQueue.size());
        }

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
        // Check pending queue first
        boolean removedPending = pendingQueue.removeIf(p -> p.taskId().equals(taskId));
        if (removedPending) {
            LOGGER.info("Task {} removed from pending queue before execution", taskId);
            finishedTasks.add(new FinishedTask(taskId, true, false, 0));
            return KillResult.successFromPending(taskId);
        }

        // Check running
        RunningTask runningTask = runningTasks.remove(taskId);
        if (runningTask == null) {
            return KillResult.notFound(taskId);
        }

        boolean cancelled = runningTask.future().cancel(true);
        if (cancelled) {
            LOGGER.info("Task {} killed successfully from running", taskId);
            finishedTasks.add(new FinishedTask(taskId, true, false, 0));
            return KillResult.successFromRunning(taskId);
        } else {
            LOGGER.warn("Failed to kill task {}", taskId);
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
        boolean removedPending = pendingQueue.removeIf(p -> p.taskId().equals(taskId));
        if (removedPending) {
            LOGGER.info("Task {} force-ok from pending queue", taskId);
            finishedTasks.add(new FinishedTask(taskId, true, true, 0));
            var report = new TaskResultReport(
                    config.getAgentId(), taskId, true, "{\"forced\":true}", Instant.now()
            );
            agentSchedulerClient.reportTaskResult(report);
            return ForceOkResult.successFromPending(taskId);
        }

        // Remove from running
        RunningTask runningTask = runningTasks.remove(taskId);
        if (runningTask != null) {
            runningTask.future().cancel(true);
            LOGGER.info("Task {} force-ok from running", taskId);
            finishedTasks.add(new FinishedTask(taskId, true, true, 0));
            var report = new TaskResultReport(
                    config.getAgentId(), taskId, true, "{\"forced\":true}", Instant.now()
            );
            agentSchedulerClient.reportTaskResult(report);
            return ForceOkResult.successFromRunning(taskId);
        }

        return ForceOkResult.notFound(taskId);
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
            agentSchedulerClient.reportTaskResult(report);
            finishTask(taskId, true, output.isSuccess(), duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.error("Task {} execution failed after {}ms", taskId, duration, e);
            var report = new top.ilovemyhome.dagtask.si.agent.TaskResultReport(
                    config.getAgentId(), taskId, false, e.getMessage(), null
            );
            agentSchedulerClient.reportTaskResult(report);
            finishTask(taskId, false, false, duration);
        }
    }

    /**
     * Moves a task from running to finished.
     */
    private void finishTask(Long taskId, boolean completedNormally, boolean success, long durationMs) {
        runningTasks.remove(taskId);
        finishedTasks.add(new FinishedTask(taskId, completedNormally, success, durationMs));
        LOGGER.debug("Moved task {} from running to finished", taskId);
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
     * Result of submitting a task.
     */
    public record SubmissionResult(
            boolean accepted,
            String message,
            Long taskId,
            Integer pendingPosition,
            Integer capacity,
            Integer currentSize,
            boolean duplicate,
            boolean executionCreationFailed,
            boolean inputParseFailed,
            boolean queueFull
    ) {
        public static SubmissionResult accepted(Long taskId, int pendingPosition) {
            return new SubmissionResult(true,
                    String.format("Task %d accepted for execution", taskId),
                    taskId, pendingPosition, null, null, false, false, false, false);
        }

        public static SubmissionResult duplicate(Long taskId) {
            return new SubmissionResult(false,
                    String.format("Task %d already exists in pending or running", taskId),
                    taskId, null, null, null, true, false, false, false);
        }

        public static SubmissionResult executionCreationFailed(String executionClass) {
            return new SubmissionResult(false,
                    "Failed to create execution for class: " + executionClass,
                    null, null, null, null, false, true, false, false);
        }

        public static SubmissionResult inputParseFailed(String error) {
            return new SubmissionResult(false,
                    "Failed to parse input: " + error,
                    null, null, null, null, false, false, true, false);
        }

        public static SubmissionResult queueFull(int capacity, int currentSize) {
            return new SubmissionResult(false,
                    "Pending queue is full",
                    null, null, capacity, currentSize, false, false, false, true);
        }
    }

    /**
     * Result of killing a task.
     */
    public record KillResult(
            boolean success,
            String message,
            boolean found,
            boolean fromPending
    ) {
        public static KillResult successFromPending(Long taskId) {
            return new KillResult(true,
                    String.format("Task %d removed from pending queue", taskId),
                    true, true);
        }

        public static KillResult successFromRunning(Long taskId) {
            return new KillResult(true,
                    String.format("Task %d killed successfully", taskId),
                    true, false);
        }

        public static KillResult notFound(Long taskId) {
            return new KillResult(false,
                    String.format("Task %d not found in pending or running", taskId),
                    false, false);
        }

        public static KillResult failedToCancel(Long taskId) {
            return new KillResult(false,
                    String.format("Failed to kill task %d", taskId),
                    true, false);
        }
    }

    /**
     * Result of force-ok operation.
     */
    public record ForceOkResult(
            boolean success,
            String message,
            boolean found,
            boolean fromPending
    ) {
        public static ForceOkResult successFromPending(Long taskId) {
            return new ForceOkResult(true,
                    String.format("Task %d marked as successful (removed from pending)", taskId),
                    true, true);
        }

        public static ForceOkResult successFromRunning(Long taskId) {
            return new ForceOkResult(true,
                    String.format("Task %d marked as successful (removed from running)", taskId),
                    true, false);
        }

        public static ForceOkResult notFound(Long taskId) {
            return new ForceOkResult(false,
                    String.format("Task %d not found in pending or running", taskId),
                    false, false);
        }
    }
}
