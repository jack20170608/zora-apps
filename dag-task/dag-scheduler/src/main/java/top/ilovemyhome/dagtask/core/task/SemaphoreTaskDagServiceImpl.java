package top.ilovemyhome.dagtask.core.task;

import com.google.common.collect.ImmutableSet;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.dao.TaskRecordDaoJdbiImpl;
import top.ilovemyhome.dagtask.core.server.DagServerConfig;
import top.ilovemyhome.dagtask.si.TaskDagService;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.dagtask.si.persistence.AgentRegistryDao;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.persistence.TaskTemplateDao;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * {@link TaskDagService} implementation using a semaphore-based approach for dependency tracking.
 * <p>
 * This implementation uses a <b>hybrid approach</b>:
 * <ul>
 *     <li><b>In-memory semaphore counters</b>: Track how many predecessor tasks are still incomplete
 *         for each waiting task. When the counter reaches zero, the task becomes ready.</li>
 *     <li><b>Database persistence</b>: All task state is always persisted to the database.
 *         In-memory state can be reconstructed from database on startup.</li>
 * </ul>
 * </p>
 * <p>
 * <b>How it works:</b>
 * <ol>
 *     <li>When tasks are created, each task gets an initial counter = number of incomplete predecessor tasks</li>
 *     <li>When a task completes successfully, we decrement the counter for each of its direct successors</li>
 *     <li>When any task's counter reaches zero, it's marked as READY and ready to be scheduled</li>
 *     <li>When a task completes with failure, all reachable downstream tasks are marked as SKIPPED</li>
 *     <li>On startup, in-memory counters are reconstructed by scanning all incomplete tasks from database</li>
 * </ol>
 * </p>
 * <p>
 * Advantages over pure database approach:
 * <ul>
 *     <li>Better performance: No expensive SQL queries to check readiness after each completion</li>
 *     <li>Immediate triggering: Ready tasks are known immediately, no need to wait for scanning</li>
 *     <li>Lower database load: Reduces complex JSONB queries on every task completion</li>
 * </ul>
 * </p>
 * <p>
 * Advantages over pure in-memory approach:
 * <ul>
 *     <li>Persistence: All state is persisted to database, survives restarts</li>
 *     <li>Consistency: Database guarantees ACID, no lost updates</li>
 *     <li>No distributed coordination needed for single scheduler center deployment</li>
 * </ul>
 * </p>
 */
public class SemaphoreTaskDagServiceImpl implements TaskDagService {

    private final DagServerConfig config;
    private final Jdbi jdbi;
    private final TaskRecordDao taskRecordDao;
    private final TaskOrderDao taskOrderDao;
    private final AgentRegistryDao agentRegistryDao;
    private final TaskTemplateDao taskTemplateDao;

    /**
     * In-memory semaphore counter for each pending task.
     * Key: taskId, Value: atomic counter of remaining incomplete predecessors.
     * When counter reaches 0, task is ready to execute.
     */
    private final Map<Long, AtomicInteger> dependencyCounter = new ConcurrentHashMap<>();

    /**
     * Ready task queue that accumulates tasks that have become ready
     * and are waiting to be scheduled.
     */
    private final Set<Long> readyTasks = ConcurrentHashMap.newKeySet();

    private static final Logger logger = LoggerFactory.getLogger(SemaphoreTaskDagServiceImpl.class);

    /**
     * Creates a new SemaphoreTaskDagServiceImpl.
     * After construction, call {@link #initialize()} to rebuild in-memory state from database.
     *
     * @param config server configuration
     * @param jdbi Jdbi instance
     * @param taskOrderDao task order DAO
     * @param taskRecordDao task record DAO
     * @param agentRegistryDao agent registry DAO
     * @param taskTemplateDao task template DAO
     */
    public SemaphoreTaskDagServiceImpl(DagServerConfig config
        , Jdbi jdbi
        , TaskOrderDao taskOrderDao
        , TaskRecordDao taskRecordDao
        , AgentRegistryDao agentRegistryDao
        , TaskTemplateDao taskTemplateDao
    ) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.jdbi = Objects.requireNonNull(jdbi, "jdbi must not be null");
        this.taskOrderDao = Objects.requireNonNull(taskOrderDao, "taskOrderDao must not be null");
        this.taskRecordDao = Objects.requireNonNull(taskRecordDao, "taskRecordDao must not be null");
        this.agentRegistryDao = agentRegistryDao;
        this.taskTemplateDao = taskTemplateDao;
    }

    /**
     * Initialize the in-memory semaphore state from database.
     * Must be called once after construction before using the service.
     * <p>
     * This reconstructs the dependency counters for all non-terminal tasks
     * by counting how many predecessors are still incomplete for each task.
     * </p>
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing semaphore-based DAG service from database...");
        dependencyCounter.clear();
        readyTasks.clear();

        jdbi.useHandle(handle -> {
            // Get all non-terminal tasks from all orders
            for (TaskStatus status : TaskStatus.values()) {
                if (status.isTerminal()) continue;

                List<TaskRecord> tasks = taskRecordDao.findByStatus(status);
                for (TaskRecord task : tasks) {
                    if (status == TaskStatus.READY) {
                        readyTasks.add(task.getId());
                    } else if (status == TaskStatus.INIT) {
                        // Count how many predecessors are not yet successful
                        int incompletePredecessors = countIncompletePredecessors(task);
                        if (incompletePredecessors == 0) {
                            // No predecessors or all complete, this task is ready
                            readyTasks.add(task.getId());
                        } else {
                            dependencyCounter.put(task.getId(), new AtomicInteger(incompletePredecessors));
                        }
                    }
                    // For RUNNING and HOLD, we don't need a counter - it hasn't completed yet
                }
            }
        });

        logger.info("Semaphore initialized: {} pending tasks, {} ready tasks",
            dependencyCounter.size(), readyTasks.size());
    }

    @Override
    public List<Long> getNextTaskIds(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("The count must be greater than 0");
        }
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(taskRecordDao.getNextId());
        }
        return Collections.unmodifiableList(ids);
    }

    @Override
    public List<Long> createTasks(List<TaskRecord> records) {
        Objects.requireNonNull(records, "records must not be null");
        if (records.isEmpty()) {
            throw new IllegalArgumentException("The records list must not be empty");
        }

        // Validate all records belong to the same order
        String orderKey = records.getFirst().getOrderKey();
        Objects.requireNonNull(orderKey, "orderKey must not be null");

        return jdbi.inTransaction(h -> {
            List<Long> generatedIds = new ArrayList<>();

            for (TaskRecord record : records) {
                if (record.getStatus() == null) {
                    record = taskRecordBuilder(record).withStatus(TaskStatus.INIT).build();
                }
                long id = taskRecordDao.create(record);
                generatedIds.add(id);

                // Initialize semaphore counter for this new task
                int incompletePredecessors = countIncompletePredecessors(record);
                if (incompletePredecessors == 0) {
                    // No incomplete predecessors, task is ready immediately
                    readyTasks.add(id);
                    if (record.getStatus() == TaskStatus.INIT) {
                        taskRecordDao.updateStatus(id, TaskStatus.READY);
                    }
                } else {
                    dependencyCounter.put(id, new AtomicInteger(incompletePredecessors));
                }
            }

            logger.info("Created {} new tasks, {} are ready immediately",
                generatedIds.size(), readyTasks.size());
            return generatedIds;
        });
    }

    @Override
    public TaskOutput runNow(Long taskId, TaskInput input) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(input, "input must not be null");

        Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found with id: " + taskId);
        }

        TaskRecord task = taskOpt.get();

        // Check if task can be started
        TaskStatus status = task.getStatus();
        if (status.isTerminal()) {
            throw new IllegalStateException("Task " + taskId + " is already in terminal status: " + status);
        }
        if (status != TaskStatus.READY) {
            throw new IllegalStateException("Task " + taskId + " is not ready (current status: " + status + ")");
        }

        // Remove from ready tasks set and mark as running
        readyTasks.remove(taskId);
        LocalDateTime now = LocalDateTime.now();
        taskRecordDao.start(taskId, input, now);

        // TODO: Actual task execution will be handled by the dispatcher
        // For now, we just complete synchronously
        try {
            TaskOutput output = TaskOutput.success(taskId, null);
            receiveTaskEvent(taskId, TaskStatus.SUCCESS, output);
            return output;
        } catch (Exception e) {
            TaskOutput output = TaskOutput.fail(taskId, null, e.getMessage());
            receiveTaskEvent(taskId, TaskStatus.ERROR, output);
            return output;
        }
    }

    @Override
    public void forceOk(Long taskId, TaskOutput output) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        logger.info("Forcing task {} to complete successfully", taskId);

        jdbi.inTransaction(h -> {
            Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
            if (taskOpt.isEmpty()) {
                throw new IllegalArgumentException("Task not found with id: " + taskId);
            }

            TaskRecord task = taskOpt.get();
            if (task.getStatus().isTerminal()) {
                logger.warn("Task {} is already in terminal status {}, ignoring forceOk",
                    taskId, task.getStatus());
                return null;
            }

            readyTasks.remove(taskId);
            dependencyCounter.remove(taskId);
            taskRecordDao.stop(taskId, TaskStatus.SUCCESS, output, LocalDateTime.now());
            processSuccess(taskId, task.getSuccessorIds());
            return null;
        });
    }

    @Override
    public void kill(Long taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        logger.info("Killing task {} (marking as ERROR)", taskId);

        jdbi.inTransaction(h -> {
            Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
            if (taskOpt.isEmpty()) {
                throw new IllegalArgumentException("Task not found with id: " + taskId);
            }

            TaskRecord task = taskOpt.get();
            if (task.getStatus().isTerminal()) {
                logger.warn("Task {} is already in terminal status {}, ignoring kill",
                    taskId, task.getStatus());
                return null;
            }

            readyTasks.remove(taskId);
            dependencyCounter.remove(taskId);
            TaskOutput output = TaskOutput.fail(taskId, null, "Task killed by operator");
            taskRecordDao.stop(taskId, TaskStatus.ERROR, output, LocalDateTime.now());
            processFailure(taskId, task.getSuccessorIds());
            return null;
        });
    }

    @Override
    public void hold(Long taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        logger.info("Holding task {}", taskId);

        jdbi.inTransaction(h -> {
            Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
            if (taskOpt.isEmpty()) {
                throw new IllegalArgumentException("Task not found with id: " + taskId);
            }

            TaskRecord task = taskOpt.get();
            TaskStatus currentStatus = task.getStatus();

            if (currentStatus.isTerminal()) {
                throw new IllegalStateException("Cannot hold task in terminal status: " + currentStatus);
            }

            if (currentStatus == TaskStatus.HOLD) {
                logger.warn("Task {} is already HOLD, ignoring", taskId);
                return null;
            }

            readyTasks.remove(taskId);
            taskRecordDao.updateStatus(taskId, TaskStatus.HOLD);
            logger.info("Task {} held successfully", taskId);
            return null;
        });
    }

    @Override
    public boolean isSuccess(String orderKey) {
        Objects.requireNonNull(orderKey, "orderKey must not be null");
        return taskRecordDao.isSuccess(orderKey);
    }

    @Override
    public void start(String orderKey) {
        Objects.requireNonNull(orderKey, "orderKey must not be null");
        logger.info("Starting DAG execution for order: {}", orderKey);

        List<TaskRecord> tasks = findTaskByOrderKey(orderKey);
        int started = 0;
        LocalDateTime now = LocalDateTime.now();

        for (TaskRecord task : tasks) {
            if (task.getStatus() == TaskStatus.READY) {
                readyTasks.remove(task.getId());
                taskRecordDao.start(task.getId(), null, now);
                started++;
                logger.debug("Marked task {} as RUNNING", task.getId());
            }
        }

        logger.info("Started {} ready tasks for order: {}", started, orderKey);
    }

    @Override
    public void receiveTaskEvent(Long taskId, TaskStatus newStatus, TaskOutput output) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        Objects.requireNonNull(output, "output must not be null");

        logger.debug("Received task event: taskId={}, newStatus={}", taskId, newStatus);

        jdbi.inTransaction(h -> {
            Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
            if (taskOpt.isEmpty()) {
                throw new IllegalArgumentException("Task not found with id: " + taskId);
            }

            TaskRecord task = taskOpt.get();
            TaskStatus oldStatus = task.getStatus();

            if (oldStatus.isTerminal()) {
                logger.warn("Task {} is already in terminal status {}, ignoring event with {}",
                    taskId, oldStatus, newStatus);
                return null;
            }

            // Clean up in-memory state
            readyTasks.remove(taskId);
            dependencyCounter.remove(taskId);

            // Update database
            taskRecordDao.stop(taskId, newStatus, output, LocalDateTime.now());

            // Process based on completion status
            Set<Long> successors = task.getSuccessorIds();
            if (newStatus == TaskStatus.SUCCESS) {
                processSuccess(taskId, successors);
            } else if (newStatus.isTerminal()) {
                // FAILURE: ERROR, TIMEOUT, UNKNOWN - skip all downstream
                processFailure(taskId, successors);
            }

            logger.info("Task event processed: taskId={}, oldStatus={}, newStatus={}",
                taskId, oldStatus, newStatus);

            return null;
        });
    }

    @Override
    public List<TaskRecord> findTaskByOrderKey(String orderKey) {
        Objects.requireNonNull(orderKey, "orderKey must not be null");
        return taskRecordDao.findByOrderKey(orderKey);
    }

    /**
     * Gets the current set of ready tasks that are waiting to be scheduled.
     *
     * @return immutable set of ready task IDs
     */
    public Set<Long> getReadyTasks() {
        return ImmutableSet.copyOf(readyTasks);
    }

    /**
     * Gets the current dependency counter for a task.
     *
     * @param taskId the task ID
     * @return the current counter value, or null if task not tracked
     */
    public Integer getDependencyCount(Long taskId) {
        AtomicInteger counter = dependencyCounter.get(taskId);
        return counter != null ? counter.get() : null;
    }

    /**
     * Gets the number of pending tasks (tasks still waiting for dependencies).
     *
     * @return count of pending tasks
     */
    public int getPendingTaskCount() {
        return dependencyCounter.size();
    }

    /**
     * Gets the number of ready tasks waiting to be scheduled.
     *
     * @return count of ready tasks
     */
    public int getReadyTaskCount() {
        return readyTasks.size();
    }

    /**
     * Process a successfully completed task.
     * Decrements the dependency counter for all direct successors,
     * and marks successors as ready when counter reaches zero.
     *
     * @param completedTaskId the completed task ID
     * @param successors the set of direct successor task IDs
     */
    private void processSuccess(Long completedTaskId, Set<Long> successors) {
        if (successors == null || successors.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Long> newlyReady = new ArrayList<>();

        for (Long successorId : successors) {
            AtomicInteger counter = dependencyCounter.get(successorId);
            if (counter == null) {
                // Counter is null means either:
                // 1. This successor was already completed/skipped
                // 2. This successor is already ready
                continue;
            }

            int remaining = counter.decrementAndGet();
            if (remaining == 0) {
                // All predecessors complete, this successor is now ready
                dependencyCounter.remove(successorId);
                readyTasks.add(successorId);
                taskRecordDao.updateStatus(successorId, TaskStatus.READY);
                newlyReady.add(successorId);
                logger.debug("Task {} is now ready (all predecessors completed)", successorId);
            }
        }

        if (!newlyReady.isEmpty()) {
            logger.info("Task {} completed successfully, {} successor(s) are now ready: {}",
                completedTaskId, newlyReady.size(), newlyReady);
        }
    }

    /**
     * Process a failed task (ERROR, TIMEOUT, UNKNOWN).
     * Skips all reachable downstream tasks since they depend on the failed task.
     *
     * @param failedTaskId the failed task ID
     * @param directSuccessors the direct successors of the failed task
     */
    private void processFailure(Long failedTaskId, Set<Long> directSuccessors) {
        if (directSuccessors == null || directSuccessors.isEmpty()) {
            return;
        }

        Set<Long> toSkip = collectAllDownstream(directSuccessors);
        int skippedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Long taskId : toSkip) {
            dependencyCounter.remove(taskId);
            readyTasks.remove(taskId);

            TaskOutput output = TaskOutput.fail(taskId, null,
                "Skipped because one or more predecessor tasks failed");
            taskRecordDao.stop(taskId, TaskStatus.SKIPPED, output, now);
            skippedCount++;
        }

        logger.info("Task {} failed, skipped {} downstream tasks", failedTaskId, skippedCount);
    }

    /**
     * Collects all reachable downstream tasks starting from a set of direct successors.
     * This traverses the DAG to find all tasks that depend (directly or indirectly) on the failed task.
     *
     * @param startIds starting point (direct successors of failed task)
     * @return set of all downstream task IDs that need to be skipped
     */
    private Set<Long> collectAllDownstream(Set<Long> startIds) {
        Set<Long> result = new HashSet<>();
        Set<Long> currentLevel = new HashSet<>(startIds);

        while (!currentLevel.isEmpty()) {
            Set<Long> nextLevel = new HashSet<>();

            for (Long taskId : currentLevel) {
                if (result.contains(taskId)) {
                    continue;
                }

                Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
                if (taskOpt.isEmpty()) {
                    continue;
                }

                TaskRecord task = taskOpt.get();
                if (task.getStatus().isTerminal()) {
                    // Already terminal, still need to process its successors
                    Set<Long> successors = task.getSuccessorIds();
                    if (successors != null) {
                        nextLevel.addAll(successors);
                    }
                    continue;
                }

                // This task needs to be skipped
                result.add(taskId);

                // Add its successors to next level for processing
                Set<Long> successors = task.getSuccessorIds();
                if (successors != null) {
                    nextLevel.addAll(successors);
                }
            }

            currentLevel = nextLevel;
        }

        return result;
    }

    /**
     * Counts how many predecessor tasks are not yet successful for a given task.
     * A predecessor is any task that has this task in its successor list.
     *
     * @param task the task to count predecessors for
     * @return number of incomplete predecessors
     */
    private int countIncompletePredecessors(TaskRecord task) {
        // Find all tasks that have this task in their successor list
        // Those are our predecessors
        String sql = String.format(
            "select count(*) from %s " +
                "where (successor_ids::jsonb) @> jsonb_build_array(:taskId) " +
                "and status != 'SUCCESS'",
            TaskRecordDaoJdbiImpl.TABLE_NAME
        );

        return jdbi.withHandle(h ->
            h.createQuery(sql)
                .bind("taskId", task.getId())
                .mapTo(Integer.class)
                .one()
        );
    }

    /**
     * Creates a builder initialized from an existing TaskRecord.
     *
     * @param record the existing task record
     * @return a builder with all fields copied
     */
    private TaskRecord.Builder taskRecordBuilder(TaskRecord record) {
        return TaskRecord.builder().from(record);
    }
}
