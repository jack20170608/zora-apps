package top.ilovemyhome.dagtask.agent.dto;

/**
 * Result of hold operation on a task.
 */
public record HoldResult(
        boolean success,
        String message,
        boolean found,
        boolean fromPending
) {
    public static HoldResult successFromPending(Long taskId) {
        return new HoldResult(true,
                String.format("Task %d removed from pending queue and held", taskId),
                true, true);
    }

    public static HoldResult successFromRunning(Long taskId) {
        return new HoldResult(true,
                String.format("Task %d cancelled and held (removed from running)", taskId),
                true, false);
    }

    public static HoldResult notFound(Long taskId) {
        return new HoldResult(false,
                String.format("Task %d not found in pending or running", taskId),
                false, false);
    }
}
