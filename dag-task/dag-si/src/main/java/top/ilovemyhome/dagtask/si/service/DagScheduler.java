package top.ilovemyhome.dagtask.si.service;

import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.List;

/**
 * DAG Scheduler - Responsible for DAG runtime scheduling and execution triggering.
 * Handles:
 * <ul>
 *     <li>Starting a DAG execution</li>
 *     <li>Finding ready tasks (all predecessors completed)</li>
 *     <li>Triggering ready tasks to TaskDispatcher for distribution</li>
 *     <li>Callback after task completion to trigger successors</li>
 * </ul>
 */
public interface DagScheduler {

    /**
     * Start execution of a DAG.
     * Finds all initially ready tasks (no predecessors or all predecessors completed)
     * and triggers them for execution.
     *
     * @param orderKey the order key of the DAG to start
     */
    void start(String orderKey);

    /**
     * Find all ready tasks for a given order.
     * A task is ready when all its predecessor tasks have completed successfully.
     *
     * @param orderKey the order key to search
     * @return list of ready tasks ready for execution
     */
    List<TaskRecord> findReadyTasks(String orderKey);

    /**
     * Trigger all ready tasks to TaskDispatcher for execution.
     *
     * @param orderKey the order key
     * @return number of tasks triggered
     */
    int triggerReadyTasks(String orderKey);

    /**
     * Callback when a task completes.
     * Updates task status and triggers all ready successor tasks.
     *
     * @param taskId    the completed task ID
     * @param newStatus the final status of the task
     * @param output    the task output
     */
    void onTaskCompleted(Long taskId, TaskStatus newStatus, TaskOutput output);
}
