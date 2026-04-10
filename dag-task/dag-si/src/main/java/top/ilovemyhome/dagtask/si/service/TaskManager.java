package top.ilovemyhome.dagtask.si.service;

import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;

import java.util.Optional;

/**
 * Task Manager - Handles manual operations on individual tasks.
 * Provides human intervention capabilities for workflow control:
 * <ul>
 *     <li>Manual task execution</li>
 *     <li>Force completion (success/failure)</li>
 *     <li>Pause/resume operations</li>
 * </ul>
 */
public interface TaskManager {

    /**
     * Get a task by ID.
     *
     * @param taskId the task ID
     * @return Optional containing the task if found
     */
    Optional<TaskRecord> getTask(Long taskId);

    /**
     * Execute a task synchronously right now.
     * Useful for testing or manual triggering.
     *
     * @param taskId the task ID to execute
     * @param input  the input parameters
     * @return the task output after execution
     */
    TaskOutput runNow(Long taskId, TaskInput input);

    /**
     * Force mark a task as completed successfully.
     * After marking, triggers all ready successor tasks.
     *
     * @param taskId the task ID
     * @param output the output to set
     */
    void forceOk(Long taskId, TaskOutput output);

    /**
     * Force kill (fail) a task.
     * After marking, no successor tasks will be triggered.
     *
     * @param taskId the task ID
     */
    void kill(Long taskId);

    /**
     * Put a task on hold.
     * The task will not be triggered even if it is ready.
     *
     * @param taskId the task ID
     */
    void hold(Long taskId);

    /**
     * Resume a held task.
     * If the task is ready after resuming, it will be triggered.
     *
     * @param taskId the task ID
     */
    void resume(Long taskId);
}
