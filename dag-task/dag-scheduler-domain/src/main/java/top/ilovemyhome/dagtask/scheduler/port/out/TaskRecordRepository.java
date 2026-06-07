package top.ilovemyhome.dagtask.scheduler.port.out;

import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for {@link TaskRecord} persistence. Implementations live in
 * dag-scheduler-adapter-persistence-jdbc (or any other persistence adapter).
 */
public interface TaskRecordRepository {

    /** Search task records using dynamic criteria. */
    // TODO(step-3): replace with domain-owned query record; SearchCriteria is a temporary leak from dag-si.
    List<TaskRecord> search(TaskRecordSearchCriteria criteria);

    /**
     * @deprecated Use {@link IdGenerator#nextTaskId()} instead. This method exists
     *             temporarily to keep the legacy TaskRecordDaoJdbiImpl signature stable;
     *             remove once the application service migration (Task 6) is complete.
     */
    @Deprecated(forRemoval = true, since = "step 2")
    Long getNextId();

    /** Delete all task records belonging to the given order. */
    int deleteByOrderKey(String orderKey);

    /** Create all task records for an order in a single batch. */
    int createTasksForOrder(String orderKey, List<TaskRecord> listOfTask);

    /** Returns true if the order has been laid out (records exist). */
    boolean isOrdered(String orderKey);

    /** Returns true if all task records for the order completed successfully. */
    boolean isSuccess(String orderKey);

    /** Mark the task as started, recording its input and start timestamp. */
    int start(Long id, TaskInput input, LocalDateTime startDt);

    /** Mark the task as stopped, recording its output, final status, and stop timestamp. */
    int stop(Long id, TaskStatus newStatus, TaskOutput output, LocalDateTime stopDt);

    /** Lookup the order key for a given task id. */
    String getTaskOrderByTaskId(Long taskId);

    /**
     * Check if the task is ready to execute by querying the database.
     * A task is ready when all its predecessor tasks have completed successfully.
     *
     * @param taskId the task id to check
     * @return true if all predecessors are SUCCESS, false otherwise
     */
    boolean isReady(Long taskId);

    /**
     * Load a single task by ID.
     *
     * @param taskId the task id to load
     * @return the loaded task, or empty if not found
     */
    Optional<TaskRecord> loadTaskById(Long taskId);

    /**
     * Find all ready tasks for an order that are still in INIT status.
     * A task is ready when all its predecessor tasks have completed successfully.
     *
     * @param orderKey the order key to find ready tasks for
     * @return list of ready tasks in INIT status ready to be executed
     */
    List<TaskRecord> findReadyTasksForOrder(String orderKey);

    /**
     * Find all ready successor tasks of a completed task.
     * After a task completes, find all its direct successors that are now ready
     * (all their predecessor tasks have completed successfully).
     *
     * @param taskId the completed task ID whose successors we want to check
     * @return list of ready successor tasks ready to be executed
     */
    List<TaskRecord> findReadySuccessors(Long taskId);

    /**
     * Update just the status of a task without changing other fields.
     *
     * @param taskId    the task ID to update
     * @param newStatus the new status to set
     * @return number of rows updated (1 if success, 0 if task not found)
     */
    int updateStatus(Long taskId, TaskStatus newStatus);
}
