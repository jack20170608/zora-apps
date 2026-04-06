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

/**
 * {@link TaskDagService} implementation using <b>named semaphore</b> approach for dependency tracking.
 * <p>
 * This implementation uses a <b>named semaphore</b> pattern where each task maintains a set of
 * <b>still-incomplete predecessor dependency task IDs</b>. When a predecessor completes successfully,
 * it's removed from the set. The task becomes ready when the set becomes empty.
 * </p>
 * <p>
 * <b>Compared to counting semaphore:</b>
 * <ul>
 *     <li><b>Advantage:</b> Naturally supports task retry. If a predecessor fails and gets retried,
 *         completing it again won't cause incorrect decrementing - it's already removed from the set.</li>
 *     <li><b>Advantage:</b> More resilient against duplicate completion events. Multiple success events
 *         for the same predecessor are safely ignored.</li>
 *     <li><b>Disadvantage:</b> Slightly more memory usage (stores the set vs just an int), but this
 *         is negligible for typical workloads.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Hybrid approach:</b> Same as counting semaphore - in-memory tracking for speed, all state persisted
 * to database for recovery. In-memory state can be reconstructed on startup.
 * </p>
 */
public class NamedSemaphoreTaskDagServiceImpl implements TaskDagService {

    private final DagServerConfig config;
    private final Jdbi jdbi;
    private final TaskRecordDao taskRecordDao;
    private final TaskOrderDao taskOrderDao;
    private final AgentRegistryDao agentRegistryDao;
    private final TaskTemplateDao taskTemplateDao;

    /**
     * In-memory named semaphore: for each pending task, stores the set of
     * predecessor task IDs that have not yet completed successfully.
     * <p>
     * Key: waiting task ID
     * Value: set of incomplete predecessor IDs
     * <p>
     * When the set becomes empty, the task is ready to execute.
     */
    private final Map<Long, Set<Long>> incompleteDependencies = new ConcurrentHashMap<>();

    /**
     * Ready task set that accumulates tasks that have become ready
     * and are waiting to be scheduled.
     */
    private final Set<Long> readyTasks = ConcurrentHashMap.newKeySet();

    private static final Logger logger = LoggerFactory.getLogger(NamedSemaphoreTaskDagServiceImpl.class);

    /**
     * Creates a new NamedSemaphoreTaskDagServiceImpl.
     * After construction, call {@link #initialize()} to rebuild in-memory state from database.
     *
     * @param config server configuration
     * @param jdbi Jdbi instance
     * @param taskOrderDao task order DAO
     * @param taskRecordDao task record DAO
     * @param agentRegistryDao agent registry DAO
     * @param taskTemplateDao task template DAO
     */
    public NamedSemaphoreTaskDagServiceImpl(DagServerConfig config
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
     * Initialize the in-memory named semaphore state from database.
     * Must be called once after construction before using the service.
     * <p>
     * This reconstructs the incomplete dependency sets for all non-terminal tasks
     * by collecting all predecessors that are still incomplete for each task.
     * </p>
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing named-semaphore-based DAG service from database...");
        incompleteDependencies.clear();
        readyTasks.clear();

        jdbi.useHandle(handle -> {
            // Process all non-terminal tasks
            for (TaskStatus status : TaskStatus.values()) {
                if (status.isTerminal()) continue;

                List<TaskRecord> tasks = taskRecordDao.findByStatus(status);
                for (TaskRecord task : tasks) {
                    if (status == TaskStatus.READY) {
                        readyTasks.add(task.getId());
                    } else if (status == TaskStatus.INIT) {
                        Set<Long> incompletePredecessors = findIncompletePredecessors(task);
                        if (incompletePredecessors.isEmpty()) {
                            // No incomplete predecessors, task is ready
                            readyTasks.add(task.getId());
                            if (task.getStatus() == TaskStatus.INIT) {
                                taskRecordDao.updateStatus(task.getId(), TaskStatus.READY);
                            }
                        } else {
                            incompleteDependencies.put(task.getId(), incompletePredecessors);
                        }
                    }
                    // RUNNING and HOLD don't need dependency tracking - they're not waiting
                }
            }
        });

        logger.info("Named semaphore initialized: {} pending tasks, {} ready tasks",
            incompleteDependencies.size(), readyTasks.size());
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

                // Build the set of incomplete predecessors
                Set<Long> incompletePredecessors = findIncompletePredecessors(record);
                if (incompletePredecessors.isEmpty()) {
                    // No incomplete predecessors, ready immediately
                    readyTasks.add(id);
                    if (record.getStatus() == TaskStatus.INIT) {
                        taskRecordDao.updateStatus(id, TaskStatus.READY);
                    }
                } else {
                    incompleteDependencies.put(id, incompletePredecessors);
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
        TaskStatus status = task.getStatus();

        // Check if task can be started
        if (status.isTerminal()) {
            throw new IllegalStateException("Task " + taskId + " is already in terminal status: " + status);
        }
        if (status != TaskStatus.READY) {
            throw new IllegalStateException("Task " + taskId + " is not ready (current status: " + status + ")");
        }

        // Remove from ready tasks and mark as running
        readyTasks.remove(taskId);
        LocalDateTime now = LocalDateTime.now();
        taskRecordDao.start(taskId, input, now);

        // Execute synchronously
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

            // Clean up in-memory state
            readyTasks.remove(taskId);
            incompleteDependencies.remove(taskId);
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

            // Clean up in-memory state
            readyTasks.remove(taskId);
            incompleteDependencies.remove(taskId);
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
            incompleteDependencies.remove(taskId);

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
     * Gets the current set of ready tasks waiting to be scheduled.
     *
     * @return immutable set of ready task IDs
     */
    public Set<Long> getReadyTasks() {
        return ImmutableSet.copyOf(readyTasks);
    }

    /**
     * Gets the set of incomplete dependencies for a pending task.
     *
     * @param taskId the task ID
     * @return immutable set of incomplete predecessor IDs, or null if task not pending
     */
    public Set<Long> getIncompleteDependencies(Long taskId) {
        Set<Long> deps = incompleteDependencies.get(taskId);
        return deps != null ? ImmutableSet.copyOf(deps) : null;
    }

    /**
     * Gets the number of pending tasks waiting for dependencies to complete.
     *
     * @return count of pending tasks
     */
    public int getPendingTaskCount() {
        return incompleteDependencies.size();
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
     * Removes this task from all successor dependency sets, and marks successors
     * as ready when their dependency sets become empty.
     *
     * @param completedTaskId the completed task ID
     * @param directSuccessors direct successor task IDs
     */
    private void processSuccess(Long completedTaskId, Set<Long> directSuccessors) {
        if (directSuccessors == null || directSuccessors.isEmpty()) {
            return;
        }

        List<Long> newlyReady = new ArrayList<>();

        for (Long successorId : directSuccessors) {
            Set<Long> successorDeps = incompleteDependencies.get(successorId);
            if (successorDeps == null) {
                // Successor already ready, already completed, or already skipped
                continue;
            }

            // Remove the completed predecessor from the dependency set
            boolean removed = successorDeps.remove(completedTaskId);

            if (removed && successorDeps.isEmpty()) {
                // All dependencies complete, successor is now ready
                incompleteDependencies.remove(successorId);
                readyTasks.add(successorId);
                taskRecordDao.updateStatus(successorId, TaskStatus.READY);
                newlyReady.add(successorId);
                logger.debug("Task {} is now ready (all dependencies completed)", successorId);
            }
        }

        if (!newlyReady.isEmpty()) {
            logger.info("Task {} completed successfully, {} successor(s) are now ready: {}",
                completedTaskId, newlyReady.size(), newlyReady);
        }
    }

    /**
     * Process a failed task. All reachable downstream tasks are marked as SKIPPED
     * since they depend on the failed task.
     *
     * @param failedTaskId the failed task ID
     * @param directSuccessors direct successors of the failed task
     */
    private void processFailure(Long failedTaskId, Set<Long> directSuccessors) {
        if (directSuccessors == null || directSuccessors.isEmpty()) {
            return;
        }

        Set<Long> toSkip = collectAllDownstream(directSuccessors);
        int skippedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Long taskId : toSkip) {
            incompleteDependencies.remove(taskId);
            readyTasks.remove(taskId);

            TaskOutput output = TaskOutput.fail(taskId, null,
                "Skipped because one or more predecessor tasks failed");
            taskRecordDao.stop(taskId, TaskStatus.SKIPPED, output, now);
            skippedCount++;
        }

        logger.info("Task {} failed, skipped {} downstream tasks", failedTaskId, skippedCount);
    }

    /**
     * Collect all reachable downstream tasks starting from a set of direct successors.
     *
     * @param startIds starting point
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

                // Add its successors to next level
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
     * Find all predecessor tasks that are not yet successful for a given task.
     * A predecessor is any task that has this task in its successor list.
     *
     * @param task the task to find incomplete predecessors for
     * @return set of incomplete predecessor task IDs
     */
    private Set<Long> findIncompletePredecessors(TaskRecord task) {
        String sql = String.format(
            "select id from %s " +
                "where (successor_ids::jsonb) @> jsonb_build_array(:taskId) " +
                "and status != 'SUCCESS'",
            TaskRecordDaoJdbiImpl.TABLE_NAME
        );

        return jdbi.withHandle(h ->
            h.createQuery(sql)
                .bind("taskId", task.getId())
                .mapTo(Long.class)
                .collectInto(HashSet::new)
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
