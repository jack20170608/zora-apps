package top.ilovemyhome.dagtask.si.agent;

import java.time.Instant;
import java.util.Objects;

public record TaskExecuteResult(
    String agentId,
    Long taskId,
    boolean success,
    String output,
    Instant endTime
) {
    public TaskExecuteResult(String agentId,
                             Long taskId,
                             boolean success,
                             String output) {
        this(agentId, taskId, success, output, Instant.now());
    }

    public TaskExecuteResult {
        Objects.requireNonNull(agentId);
        Objects.requireNonNull(taskId);
        endTime = Objects.isNull(endTime) ? Instant.now() : endTime;
    }

    public static TaskExecuteResult of(String agentId, Long taskId, boolean success, String output) {
        return new TaskExecuteResult(agentId, taskId, success, output);
    }
}
