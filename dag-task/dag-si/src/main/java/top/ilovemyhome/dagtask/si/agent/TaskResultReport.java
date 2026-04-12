package top.ilovemyhome.dagtask.si.agent;

import java.time.Instant;
import java.util.Objects;

public record TaskResultReport(
    String agentId,
    Long taskId,
    boolean success,
    String output,
    Instant endTime
) {
    public TaskResultReport(String agentId,
                            Long taskId,
                            boolean success,
                            String output) {
        this(agentId, taskId, success, output, Instant.now());
    }

    public TaskResultReport {
        Objects.requireNonNull(agentId);
        Objects.requireNonNull(taskId);
        endTime = Objects.isNull(endTime) ? Instant.now() : endTime;
    }
}
