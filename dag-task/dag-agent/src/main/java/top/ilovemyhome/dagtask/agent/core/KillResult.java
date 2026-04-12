package top.ilovemyhome.dagtask.agent.core;

/**
 * Result of killing/canceling a task.
 */
public record KillResult(
        boolean success,
        String message,
        boolean found,
        boolean fromPending
) {
    public static KillResult successFromPending(Long taskId) {
        return new KillResult(true,
                String.format("Task %d removed from pending queue", taskId),
                true, true);
    }

    public static KillResult successFromRunning(Long taskId) {
        return new KillResult(true,
                String.format("Task %d killed successfully", taskId),
                true, false);
    }

    public static KillResult notFound(Long taskId) {
        return new KillResult(false,
                String.format("Task %d not found in pending or running", taskId),
                false, false);
    }

    public static KillResult failedToCancel(Long taskId) {
        return new KillResult(false,
                String.format("Failed to kill task %d", taskId),
                true, false);
    }
}
