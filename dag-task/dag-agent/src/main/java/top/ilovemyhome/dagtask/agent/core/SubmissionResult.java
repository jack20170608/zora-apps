package top.ilovemyhome.dagtask.agent.core;

/**
 * Result of submitting a new task to the execution engine.
 */
public record SubmissionResult(
        boolean accepted,
        String message,
        Long taskId,
        Integer pendingPosition,
        Integer capacity,
        Integer currentSize,
        boolean duplicate,
        boolean executionCreationFailed,
        boolean inputParseFailed,
        boolean queueFull
) {
    public static SubmissionResult accepted(Long taskId, int pendingPosition) {
        return new SubmissionResult(true,
                String.format("Task %d accepted for execution", taskId),
                taskId, pendingPosition, null, null, false, false, false, false);
    }

    public static SubmissionResult duplicate(Long taskId) {
        return new SubmissionResult(false,
                String.format("Task %d already exists in pending or running", taskId),
                taskId, null, null, null, true, false, false, false);
    }

    public static SubmissionResult executionCreationFailed(String executionClass) {
        return new SubmissionResult(false,
                "Failed to create execution for class: " + executionClass,
                null, null, null, null, false, true, false, false);
    }

    public static SubmissionResult inputParseFailed(String error) {
        return new SubmissionResult(false,
                "Failed to parse input: " + error,
                null, null, null, null, false, false, true, false);
    }

    public static SubmissionResult queueFull(int capacity, int currentSize) {
        return new SubmissionResult(false,
                "Pending queue is full",
                null, null, capacity, currentSize, false, false, false, true);
    }
}
