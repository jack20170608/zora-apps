package top.ilovemyhome.dagtask.agent.dto;

/**
 * Result of free operation on a task.
 */
public record FreeResult(
        boolean success,
        String message,
        boolean found
) {
    public static FreeResult successResult() {
        return new FreeResult(true, "Task released successfully", true);
    }

    public static FreeResult notFound(Long taskId) {
        return new FreeResult(false,
                String.format("Task %d not found held on this agent", taskId),
                false);
    }
}
