package top.ilovemyhome.dagtask.si.enums;

/**
 * Execution status enum for a DAG task.
 * <p>
 * This enum defines all possible states that a task can go through during its lifecycle.
 * All terminal states are final and cannot be transitioned out of.
 * </p>
 */
public enum TaskStatus {

    /**
     * Initial state - task has been created but dependencies are not yet satisfied.
     * Task stays in this state until all predecessor tasks complete successfully.
     */
    INIT,

    /**
     * Ready state - all dependencies have been satisfied, task is waiting to be scheduled for execution.
     */
    READY,

    /**
     * Held state - task has been manually paused by a user.
     * Can be transitioned back to READY to resume execution.
     */
    HOLD,

    /**
     * Running state - task is currently executing on an agent.
     */
    RUNNING,

    /**
     * Unknown state - the agent that was executing this task stopped sending heartbeats
     * and is considered dead. This is a terminal state.
     */
    UNKNOWN,

    /**
     * Success state - task completed execution successfully. This is a terminal state.
     */
    SUCCESS,

    /**
     * Error state - task completed execution with an error. This is a terminal state.
     */
    ERROR,

    /**
     * Timeout state - task exceeded its execution timeout and was terminated. This is a terminal state.
     */
    TIMEOUT,

    /**
     * Skipped state - task was skipped because one or more predecessor tasks failed.
     * This is a terminal state.
     */
    SKIPPED;

    /**
     * Checks if this is a terminal state (no further transitions possible).
     * Terminal states are: UNKNOWN, SUCCESS, ERROR, TIMEOUT, SKIPPED.
     *
     * @return true if this is a terminal state
     */
    public boolean isTerminal() {
        return this == UNKNOWN || this == SUCCESS || this == ERROR
            || this == TIMEOUT || this == SKIPPED;
    }

    /**
     * Checks if the task is currently running.
     *
     * @return true if status is RUNNING
     */
    public boolean isRunning() {
        return this == RUNNING;
    }

    /**
     * Checks if the task completed successfully.
     * Only valid for terminal states.
     *
     * @return true if status is SUCCESS
     */
    public boolean isSuccessful() {
        return this == SUCCESS;
    }

    /**
     * Checks if the task is in a active state that can transition to something else.
     *
     * @return true if not a terminal state
     */
    public boolean isActive() {
        return !isTerminal();
    }
}
