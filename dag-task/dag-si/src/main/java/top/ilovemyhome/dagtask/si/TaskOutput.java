package top.ilovemyhome.dagtask.si;

import java.util.Objects;

public record TaskOutput(
    Long taskId,
    boolean isSuccess,
    String message,
    Object output) {

    public static TaskOutput success(Long taskId, Object output) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        return new TaskOutput(taskId, true, null, output);
    }

    public static TaskOutput fail(Long taskId, Object output, String message) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        return new TaskOutput(taskId, false, message, output);
    }

    public static TaskOutput createErrorOutput(Long taskId, Throwable t) {
        return new TaskOutput(taskId, false, t.getMessage(), null);
    }

}
