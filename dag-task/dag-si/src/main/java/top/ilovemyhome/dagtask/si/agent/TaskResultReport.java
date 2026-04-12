package top.ilovemyhome.dagtask.si.agent;

import java.time.Instant;

public record TaskResultReport(
    String agentId,
    long taskId,
    boolean success,
    String output,
    Instant endTime
) {}
