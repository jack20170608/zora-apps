package top.ilovemyhome.dagtask.agent.dto;

/**
 * Result of force-nok operation on a task.
 */
public record ForceNokResult(
        boolean success,
        String message,
        boolean found,
        boolean fromPending
) {
    public static ForceNokResult successFromPending(Long taskId) {
        return new ForceNokResult(true,
                String.format("Task %d marked as failed (removed from pending)", taskId),
                true, true);
    }

    public static ForceNokResult successFromRunning(Long taskId) {
        return new ForceNokResult(true,
                String.format("Task %d marked as failed (removed from running)", taskId),
                true, false);
    }

    public static ForceNokResult notFound(Long taskId) {
        return new ForceNokResult(false,
                String.format("Task %d not found in pending or running", taskId),
                false, false);
    }
}
