package top.ilovemyhome.dagtask.agent.log;

import top.ilovemyhome.dagtask.si.TaskLogWriter;

/**
 * Thread-local holder for the active {@link TaskLogWriter} during task execution.
 * Used by {@link TaskLogAppender} to route SLF4J logs into the per-task log file.
 */
public final class TaskLogContext {

    private static final ThreadLocal<TaskLogWriter> HOLDER = new ThreadLocal<>();

    private TaskLogContext() {
    }

    /**
     * Binds the given writer to the current thread.
     */
    public static void set(TaskLogWriter writer) {
        HOLDER.set(writer);
    }

    /**
     * Returns the writer bound to the current thread, or {@code null} if none.
     */
    public static TaskLogWriter get() {
        return HOLDER.get();
    }

    /**
     * Removes the writer binding for the current thread.
     * Must be called after task execution to prevent memory leaks.
     */
    public static void clear() {
        HOLDER.remove();
    }
}
