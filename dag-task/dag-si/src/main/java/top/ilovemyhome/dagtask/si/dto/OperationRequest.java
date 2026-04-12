package top.ilovemyhome.dagtask.si.dto;


import top.ilovemyhome.dagtask.si.enums.OpsType;
import top.ilovemyhome.dagtask.si.enums.PriorityType;

import java.time.Instant;
import java.util.Objects;

public record OperationRequest(
    Long taskId,
    OpsType opsType,
    String executionClass,
    String input,
    Boolean force,
    PriorityType priorityType,
    String reason,
    String dealer,
    Instant requestDt
    ) {

    public OperationRequest {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(opsType);

        force = force != null && force;
        priorityType = Objects.isNull(priorityType) ? PriorityType.NORMAL : priorityType;
        requestDt = Objects.isNull(requestDt) ? Instant.now(): requestDt;
    }
}
