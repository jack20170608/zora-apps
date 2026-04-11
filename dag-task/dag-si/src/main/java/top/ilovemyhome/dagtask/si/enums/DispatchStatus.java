package top.ilovemyhome.dagtask.si.enums;

/**
 * Dispatch status enum.
 */
public enum DispatchStatus {
    /**
     * Task has been dispatched to the agent but not yet accepted.
     */
    DISPATCHED,

    /**
     * Agent has accepted the task and it's queued/running.
     */
    ACCEPTED,

    /**
     * Agent rejected the task (e.g., queue full).
     */
    REJECTED,

    /**
     * Dispatch failed due to connection/network error.
     */
    FAILED,

    /**
     * Task has completed execution (success or failure).
     */
    COMPLETED
}
