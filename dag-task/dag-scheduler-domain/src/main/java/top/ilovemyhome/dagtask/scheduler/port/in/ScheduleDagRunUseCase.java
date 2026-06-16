package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.List;

/**
 * Drive the scheduler's runtime loop: start a DAG, discover ready tasks, dispatch them,
 * and reconcile completion callbacks.
 * <p>
 * Replaces {@code DagScheduleService.start/findReadyTasks/triggerReadyTasks/onTaskCompleted}.
 * Manual operator interventions live in {@link ManualTaskOperationUseCase}.
 * </p>
 */
public interface ScheduleDagRunUseCase {

    /**
     * Start execution of a DAG. Finds all initially ready tasks (no predecessors or
     * all predecessors completed) and triggers them for execution.
     *
     * @param orderKey the order key of the DAG to start
     */
    void start(String orderKey);

    /**
     * Find all ready tasks for a given order. A task is ready when all its predecessor
     * tasks have completed successfully.
     *
     * @param orderKey the order key to search
     * @return list of tasks ready for execution
     */
    List<TaskRecord> findReadyTasks(String orderKey);

    /**
     * Trigger all ready tasks to the dispatcher for execution.
     *
     * @param orderKey the order key
     * @return number of tasks triggered
     */
    int triggerReadyTasks(String orderKey);

    /**
     * Callback when a task completes. Updates task status and triggers all ready
     * successor tasks.
     *
     * @param taskId the completed task ID
     * @param newStatus the final status of the task
     * @param output the task output
     */
    void onTaskCompleted(Long taskId, TaskStatus newStatus, TaskOutput output);
}
