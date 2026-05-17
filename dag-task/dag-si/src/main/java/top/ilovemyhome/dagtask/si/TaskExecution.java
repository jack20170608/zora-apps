package top.ilovemyhome.dagtask.si;

import org.slf4j.MDC;

/**
 * Functional interface for task execution.
 * Each implementation receives a {@link TaskInput} containing the task parameters.
 *
 * <p>Implementations should override {@link #doExecute(TaskInput)} rather than
 * {@link #execute(TaskInput)} so that task MDC context is set up automatically.</p>
 */
@FunctionalInterface
public interface TaskExecution {

    String MDC_TASK_ID = "taskId";
    String MDC_TASK_NAME = "taskName";

    /**
     * Execute a task with the given input.
     * This method sets up MDC context (taskId/taskName) before delegation
     * and cleans it up afterwards. Implementations should not call this directly.
     *
     * @param input the task input containing parameters
     * @return the task execution output
     */
    default TaskOutput execute(TaskInput input) {
        Long taskId = input.taskId();
        String taskName = input.name();
        try {
            if (taskId != null) {
                MDC.put(MDC_TASK_ID, taskId.toString());
            }
            if (taskName != null) {
                MDC.put(MDC_TASK_NAME, taskName);
            }
            return doExecute(input);
        } finally {
            MDC.remove(MDC_TASK_ID);
            MDC.remove(MDC_TASK_NAME);
        }
    }

    /**
     * Core task execution logic. Implementations override this method.
     *
     * @param input the task input containing parameters
     * @return the task execution output
     */
    TaskOutput doExecute(TaskInput input);
}
