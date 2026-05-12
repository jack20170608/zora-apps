package top.ilovemyhome.dagtask.si;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FunctionalInterface
public interface TaskExecution {

    Logger logger = LoggerFactory.getLogger(TaskExecution.class);

    TaskOutput execute(TaskInput input);

    /**
     * Execute a task with a {@link TaskLogWriter} for per-task logging.
     * Implementations that support per-task logging should override this method.
     * Default implementation delegates to {@link #execute(TaskInput)} for backward compatibility.
     */
    default TaskOutput execute(TaskInput input, TaskLogWriter logWriter) {
        return execute(input);
    }
}
