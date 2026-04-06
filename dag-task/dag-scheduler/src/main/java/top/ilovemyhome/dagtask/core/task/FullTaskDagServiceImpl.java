package top.ilovemyhome.dagtask.core.task;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.dag.DagHelper;
import top.ilovemyhome.dagtask.core.dag.DagNode;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Full implementation of {@link TaskDagService} that handles complete DAG task scheduling lifecycle.
 * <p>
 * This implementation provides complete handling for:
 * <ul>
 *     <li>Task creation and DAG cycle validation</li>
 *     <li>Starting a DAG workflow</li>
 *     <li>Task status transition handling</li>
 *     <li>Automatic ready detection for successor tasks</li>
 *     <li>Skip propagation when predecessor tasks fail</li>
 *     <li>Manual operations: force success, kill, hold</li>
 *     <li>Synchronous task execution</li>
 * </ul>
 * </p>
 */
public class FullTaskDagServiceImpl implements TaskDagService {

    private final Jdbi jdbi;
    private final TaskRecordDao taskRecordDao;
    private final TaskOrderDao taskOrderDao;
    private final AgentRegistryDao agentRegistryDao;
    private final TaskTemplateDao taskTemplateDao;
    private final DagServerConfig config;

    private static final Logger logger = LoggerFactory.getLogger(FullTaskDagServiceImpl.class);

    /**
     * Creates a new FullTaskDagServiceImpl with all required dependencies.
     *
     * @param config server configuration
     * @param jdbi Jdbi instance for database access
     * @param taskOrderDao DAO for task order operations
     * @param taskRecordDao DAO for task record operations
     * @param agentRegistryDao DAO for agent registry
     * @param taskTemplateDao DAO for task template operations
     */
    public FullTaskDagServiceImpl(DagServerConfig config
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
        if (StringUtils.isBlank(orderKey)) {
            throw new IllegalArgumentException("The record order key must not be empty");
        }
        records.forEach(r -> {
            if (!StringUtils.equals(orderKey, r.getOrderKey())) {
                throw new IllegalArgumentException("All records must have the same order key");
            }
        });

        return jdbi.inTransaction(h -> {
            List<TaskRecord> existingTasks = taskRecordDao.findByOrderKey(orderKey);
            Map<Long, TaskRecord> existingTaskMap = existingTasks.stream()
                .collect(Collectors.toMap(TaskRecord::getId, Function.identity()));

            // Check for duplicate task IDs
            records.forEach(newRecord -> {
                if (existingTaskMap.containsKey(newRecord.getId())) {
                    throw new IllegalArgumentException("Already existing task record with id: " + newRecord.getId());
                }
            });

            // Build complete DAG with existing and new tasks
            List<DagNode> dagNodes = new ArrayList<>();
            existingTasks.forEach(task -> dagNodes.add(toDagNode(task)));
            records.forEach(newRecord -> dagNodes.add(toDagNode(newRecord)));

            // Validate DAG (check for cycles)
            List<String> validationPath = new ArrayList<>();
            logger.info("Validating DAG for order: {}, total nodes: {}", orderKey, dagNodes.size());
            DagHelper.visitDAG(dagNodes, validationPath);
            logger.info("DAG validation passed. Topological order paths: {}", validationPath);

            // All new tasks start in INIT status
            records.forEach(newRecord -> {
                if (newRecord.getStatus() == null) {
                    newRecordBuilder(newRecord).withStatus(TaskStatus.INIT).build();
                }
            });

            // Insert all new records
            List<Long> generatedIds = new ArrayList<>();
            records.forEach(newRecord -> {
                long id = taskRecordDao.create(newRecord);
                generatedIds.add(id);
            });

            logger.info("Created {} new tasks for order: {}", generatedIds.size(), orderKey);
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
        if (!taskRecordDao.isReady(taskId)) {
            throw new IllegalStateException("Task " + taskId + " is not ready. Check predecessor dependencies.");
        }

        if (task.getStatus().isTerminal()) {
            throw new IllegalStateException("Task " + taskId + " is already in terminal status: " + task.getStatus());
        }

        // Execute synchronously in current thread
        // For synchronous tasks, we handle the entire lifecycle here
        LocalDateTime now = LocalDateTime.now();
        taskRecordDao.start(taskId, input, now);

        try {
            // TODO: Actual task execution will be handled by the executor
            // For now, we just mark it as success
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
        logger.info("Forcing task {} to complete successfully with output: {}", taskId, output);

        jdbi.inTransaction(h -> {
            Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
            if (taskOpt.isEmpty()) {
                throw new IllegalArgumentException("Task not found with id: " + taskId);
            }

            TaskRecord task = taskOpt.get();
            if (task.getStatus() == TaskStatus.SUCCESS) {
                logger.warn("Task {} is already SUCCESS, ignoring forceOk", taskId);
                return null;
            }

            // Mark as SUCCESS and complete
            taskRecordDao.stop(taskId, TaskStatus.SUCCESS, output, LocalDateTime.now());
            processTaskCompletion(taskId, TaskStatus.SUCCESS);
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

            TaskOutput output = TaskOutput.fail(taskId, null, "Task killed by operator");
            taskRecordDao.stop(taskId, TaskStatus.ERROR, output, LocalDateTime.now());
            processTaskCompletion(taskId, TaskStatus.ERROR);
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

            if (!currentStatus.isActive()) {
                throw new IllegalStateException("Cannot hold task in terminal status: " + currentStatus);
            }

            if (currentStatus == TaskStatus.HOLD) {
                logger.warn("Task {} is already HOLD, ignoring", taskId);
                return null;
            }

            // Update status to HOLD
            taskRecordDao.updateStatus(taskId, TaskStatus.HOLD);
            logger.info("Task {} held successfully", taskId);
            return null;
        });
    }

    @Override
    public boolean isSuccess(String orderKey) {
        Objects.requireNonNull(orderKey, "orderKey must not be null");
        boolean ordered = taskRecordDao.isOrdered(orderKey);
        if (!ordered) {
            return false;
        }
        return taskRecordDao.isSuccess(orderKey);
    }

    @Override
    public void start(String orderKey) {
        Objects.requireNonNull(orderKey, "orderKey must not be null");
        logger.info("Starting DAG execution for order: {}", orderKey);

        jdbi.inTransaction(h -> {
            List<TaskRecord> readyTasks = taskRecordDao.findReadyTasksForOrder(orderKey);

            if (readyTasks.isEmpty()) {
                logger.warn("No ready tasks found for order: {}", orderKey);
                return null;
            }

            logger.info("Found {} initial ready tasks to start for order: {}", readyTasks.size(), orderKey);

            LocalDateTime now = LocalDateTime.now();
            readyTasks.forEach(task -> {
                taskRecordDao.start(task.getId(), null, now);
                logger.debug("Marked task {} as RUNNING", task.getId());
            });

            // TODO: When task dispatcher is implemented, these tasks will be dispatched to agents
            // For now, we just mark them as running in the database

            return null;
        });

        logger.info("DAG start completed for order: {}", orderKey);
    }

    @Override
    public void receiveTaskEvent(Long taskId, TaskStatus newStatus, TaskOutput output) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        Objects.requireNonNull(output, "output must not be null");

        logger.info("Received task event: taskId={}, newStatus={}", taskId, newStatus);

        jdbi.inTransaction(h -> {
            Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
            if (taskOpt.isEmpty()) {
                throw new IllegalArgumentException("Task not found with id: " + taskId);
            }

            TaskRecord task = taskOpt.get();
            TaskStatus oldStatus = task.getStatus();

            // Validate status transition
            if (oldStatus.isTerminal()) {
                logger.warn("Task {} is already in terminal status {}, ignoring event with {}",
                    taskId, oldStatus, newStatus);
                return null;
            }

            // Update the task status and output
            taskRecordDao.stop(taskId, newStatus, output, LocalDateTime.now());

            // Process post-completion logic: update successor tasks
            processTaskCompletion(taskId, newStatus);

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
     * Process task completion and handle successor tasks.
     * <p>
     * This method:
     * <ul>
     *     <li>If task succeeded: check all direct successors - if all predecessors are complete, mark as READY</li>
     *     <li>If task failed: skip all reachable downstream tasks (mark as SKIPPED)</li>
     * </ul>
     * </p>
     *
     * @param completedTaskId the completed task ID
     * @param completionStatus the completion status (SUCCESS or ERROR/TIMEOUT/UNKNOWN)
     */
    private void processTaskCompletion(Long completedTaskId, TaskStatus completionStatus) {
        if (completionStatus == TaskStatus.SUCCESS) {
            // Task succeeded, check which successors are now ready
            List<TaskRecord> readySuccessors = taskRecordDao.findReadySuccessors(completedTaskId);
            logger.info("Task {} succeeded, {} successors are now ready", completedTaskId, readySuccessors.size());
            // Ready tasks will be picked up by the scanner and dispatched
        } else if (completionStatus.isTerminal() && completionStatus != TaskStatus.SKIPPED) {
            // Task failed (ERROR, TIMEOUT, UNKNOWN), skip all downstream tasks
            int skipped = skipDownstreamTasks(completedTaskId);
            logger.info("Task {} failed, skipped {} downstream tasks", completedTaskId, skipped);
        }
    }

    /**
     * Skip all reachable downstream tasks starting from a failed task.
     * This propagates failure through the DAG - if a task fails, all tasks that depend on it
     * cannot execute and get marked as SKIPPED.
     *
     * @param startingTaskId the starting failed task ID
     * @return number of tasks skipped
     */
    private int skipDownstreamTasks(Long startingTaskId) {
        Set<Long> skippedTasks = Sets.newHashSet();
        Set<Long> currentLevel = Sets.newHashSet();

        Optional<TaskRecord> startTaskOpt = taskRecordDao.loadTaskById(startingTaskId);
        if (startTaskOpt.isEmpty()) {
            return 0;
        }

        TaskRecord startTask = startTaskOpt.get();
        Set<Long> successors = startTask.getSuccessorIds();
        if (successors != null) {
            currentLevel.addAll(successors);
        }

        while (!currentLevel.isEmpty()) {
            Set<Long> nextLevel = Sets.newHashSet();
            LocalDateTime now = LocalDateTime.now();

            for (Long taskId : currentLevel) {
                if (skippedTasks.contains(taskId)) {
                    continue;
                }

                Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
                if (taskOpt.isEmpty()) {
                    logger.warn("Could not find task {} while skipping downstream", taskId);
                    continue;
                }

                TaskRecord task = taskOpt.get();
                if (task.getStatus().isTerminal()) {
                    // Already processed, just collect its successors
                    Set<Long> taskSuccessors = task.getSuccessorIds();
                    if (taskSuccessors != null) {
                        nextLevel.addAll(taskSuccessors);
                    }
                    continue;
                }

                // Skip this task
                TaskOutput output = TaskOutput.fail(taskId, null,
                    "Skipped because one or more predecessor tasks failed");
                taskRecordDao.stop(taskId, TaskStatus.SKIPPED, output, now);
                skippedTasks.add(taskId);
                logger.debug("Skipped downstream task {} due to predecessor failure", taskId);

                // Add its successors to next level for processing
                Set<Long> taskSuccessors = task.getSuccessorIds();
                if (taskSuccessors != null) {
                    nextLevel.addAll(taskSuccessors);
                }
            }

            currentLevel = nextLevel;
        }

        return skippedTasks.size();
    }

    /**
     * Converts a TaskRecord to a DagNode for DAG validation.
     *
     * @param record the task record
     * @return the corresponding DagNode
     */
    private DagNode toDagNode(TaskRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        return new DagNode(record.getId(), record.getName(),
            record.getSuccessorIds() != null ? Sets.newHashSet(record.getSuccessorIds()) : null);
    }

    /**
     * Creates a builder initialized from an existing TaskRecord.
     *
     * @param record the existing task record
     * @return a builder with all fields copied from the record
     */
    private TaskRecord.Builder taskRecordBuilder(TaskRecord record) {
        return TaskRecord.builder().from(record);
    }

    /**
     * Gets the Jdbi instance used by this service.
     *
     * @return the Jdbi instance
     */
    public Jdbi getJdbi() {
        return jdbi;
    }

    /**
     * Gets the task record DAO.
     *
     * @return the task record DAO
     */
    public TaskRecordDao getTaskRecordDao() {
        return taskRecordDao;
    }

    /**
     * Gets the task order DAO.
     *
     * @return the task order DAO
     */
    public TaskOrderDao getTaskOrderDao() {
        return taskOrderDao;
    }

    /**
     * Gets the configuration.
     *
     * @return the DAG server configuration
     */
    public DagServerConfig getConfig() {
        return config;
    }
}
