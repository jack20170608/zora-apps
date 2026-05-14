package top.ilovemyhome.dagtask.si;

/**
 * Functional interface for task execution.
 * Each implementation receives a {@link TaskInput} containing the task parameters.
 */
@FunctionalInterface
public interface TaskExecution {

    /**
     * Execute a task with the given input.
     *
     * @param input the task input containing parameters
     * @return the task execution output
     */
    TaskOutput execute(TaskInput input);
}
