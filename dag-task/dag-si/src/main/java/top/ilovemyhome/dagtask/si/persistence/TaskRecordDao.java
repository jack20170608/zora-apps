package top.ilovemyhome.dagtask.si.persistence;

import java.util.Optional;

import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRecordDao extends BaseDao<TaskRecord> {


    List<TaskRecord> search(TaskRecordSearchCriteria criteria);

    //Get the next task id
    Long getNextId();

    int deleteByOrderKey(String orderKey);

    int createTasksForOrder(String orderKey, List<TaskRecord> listOfTask);

    boolean isOrdered(String orderKey);

    boolean isSuccess(String orderKey);

    int start(Long id, TaskInput input, LocalDateTime startDt);

    int stop(Long id, TaskStatus newStatus, TaskOutput output, LocalDateTime stopDt);

    String getTaskOrderByTaskId(Long taskId);

    /**
     * Check if the task is ready to execute by querying the database.
     * A task is ready when all its predecessor tasks have completed successfully.
     * @param taskId the task id to check
     * @return true if all predecessors are SUCCESS, false otherwise
     */
    boolean isReady(Long taskId);

    /**
     * Load a single task by ID.
     * @param taskId the task id to load
     * @return the loaded task, or empty if not found
     */
    Optional<TaskRecord> loadTaskById(Long taskId);

    /**
     * Find all ready tasks for an order that are still in INIT status.
     * A task is ready when all its predecessor tasks have completed successfully.
     * @param orderKey the order key to find ready tasks for
     * @return list of ready tasks in INIT status ready to be executed
     */
    List<TaskRecord> findReadyTasksForOrder(String orderKey);

    /**
     * Find all ready successor tasks of a completed task.
     * After a task completes, find all its direct successors that are now ready
     * (all their predecessor tasks have completed successfully).
     * @param taskId the completed task ID whose successors we want to check
     * @return list of ready successor tasks ready to be executed
     */
    List<TaskRecord> findReadySuccessors(Long taskId);

    /**
     * Update just the status of a task without changing other fields.
     * @param taskId the task ID to update
     * @param newStatus the new status to set
     * @return number of rows updated (1 if success, 0 if task not found)
     */
    int updateStatus(Long taskId, TaskStatus newStatus);

}
