package top.ilovemyhome.dagtask.si;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Functional interface for task execution.
 * Each implementation receives a {@link TaskInput} containing the task parameters
 * and a {@link TaskLogWriter} for per-task logging.
 */
@FunctionalInterface
public interface TaskExecution {

    Logger logger = LoggerFactory.getLogger(TaskExecution.class);

    /**
     * Execute a task with the given input and log writer.
     *
     * @param input     the task input containing parameters
     * @param logWriter the per-task log writer; may be null if per-task logging is disabled
     * @return the task execution output
     */
    TaskOutput execute(TaskInput input, TaskLogWriter logWriter);
}
