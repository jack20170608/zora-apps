package top.ilovemyhome.dagtask.si;

import org.slf4j.MDC;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
     * Global cancellation registry. Task IDs added here are considered cancelled
     * regardless of thread interrupt status. Implementations should call
     * {@link #checkInterruption(Long)} periodically to respect cancellation.
     */
    Set<Long> CANCELLED = ConcurrentHashMap.newKeySet();

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
     * Marks a task as cancelled in the global registry.
     * Called by the execution engine when kill / force-ok / force-nok / hold
     * is requested. The cancellation flag is cleared when the task finishes.
     *
     * @param taskId the task ID to cancel
     */
    static void cancel(Long taskId) {
        if (taskId != null) {
            CANCELLED.add(taskId);
        }
    }

    /**
     * Clears a task from the global cancellation registry.
     * Called by the execution engine after a task completes.
     *
     * @param taskId the task ID to clear
     */
    static void clearCancellation(Long taskId) {
        if (taskId != null) {
            CANCELLED.remove(taskId);
        }
    }

    /**
     * Checks whether the current thread has been interrupted or the given task
     * has been marked for cancellation. Long-running or CPU-intensive
     * implementations should call this method periodically inside tight loops
     * so that the engine can stop them promptly.
     *
     * @param taskId the task ID to check against the cancellation registry
     * @throws IllegalStateException if interrupted or cancelled
     */
    default void checkInterruption(Long taskId) {
        if (Thread.currentThread().isInterrupted() || CANCELLED.contains(taskId)) {
            throw new IllegalStateException("Task was interrupted");
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
