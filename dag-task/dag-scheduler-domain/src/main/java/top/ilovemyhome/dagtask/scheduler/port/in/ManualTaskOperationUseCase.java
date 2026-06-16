package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.si.DispatchResult;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

/**
 * Operator-initiated interventions on individual tasks.
 * <p>
 * Replaces {@code DagScheduleService.runNow/forceOk/kill/hold/resume}.
 * </p>
 */
public interface ManualTaskOperationUseCase {

    /**
     * Execute a task synchronously right now. Useful for testing or manual triggering.
     *
     * @param taskId the task ID to execute
     * @param input the input parameters
     * @return the dispatch result after execution
     */
    DispatchResult runNow(Long taskId, TaskInput input);

    /**
     * Force mark a task as completed successfully. After marking, triggers all ready
     * successor tasks.
     *
     * @param taskId the task ID
     * @param output the output to set
     */
    void forceOk(Long taskId, TaskOutput output);

    /**
     * Force kill (fail) a task. After marking, no successor tasks will be triggered.
     *
     * @param taskId the task ID
     */
    void kill(Long taskId);

    /**
     * Put a task on hold. The task will not be triggered even if it is ready.
     *
     * @param taskId the task ID
     */
    void hold(Long taskId);

    /**
     * Resume a held task. If the task is ready after resuming, it will be triggered.
     *
     * @param taskId the task ID
     */
    void resume(Long taskId);
}
