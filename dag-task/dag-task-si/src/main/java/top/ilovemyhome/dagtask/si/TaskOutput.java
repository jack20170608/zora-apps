package top.ilovemyhome.dagtask.si;

import java.util.Objects;

public record TaskOutput<O>(
    Long taskId,
    boolean isSuccess,
    String message,
    O output) {

    public static <O> TaskOutput<O> success(Long taskId, O output) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        return new TaskOutput<>(taskId, true, null, output);
    }

    public static <O> TaskOutput<O> fail(Long taskId, O output, String message) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        return new TaskOutput<>(taskId, false, message, output);
    }

    public static <O> TaskOutput<O> createErrorOutput(Long taskId, Throwable t) {
        return new TaskOutput<>(taskId, false, t.getMessage(), null);
    }

}
