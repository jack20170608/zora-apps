package top.ilovemyhome.dagtask.agent.core;

/**
 * Result of force-ok operation on a task.
 */
public record ForceOkResult(
        boolean success,
        String message,
        boolean found,
        boolean fromPending
) {
    public static ForceOkResult successFromPending(Long taskId) {
        return new ForceOkResult(true,
                String.format("Task %d marked as successful (removed from pending)", taskId),
                true, true);
    }

    public static ForceOkResult successFromRunning(Long taskId) {
        return new ForceOkResult(true,
                String.format("Task %d marked as successful (removed from running)", taskId),
                true, false);
    }

    public static ForceOkResult notFound(Long taskId) {
        return new ForceOkResult(false,
                String.format("Task %d not found in pending or running", taskId),
                false, false);
    }
}
