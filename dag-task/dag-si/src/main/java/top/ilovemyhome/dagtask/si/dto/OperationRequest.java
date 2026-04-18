package top.ilovemyhome.dagtask.si.dto;


import top.ilovemyhome.dagtask.si.enums.OpsType;

import java.time.Instant;
import java.util.Objects;

public record OperationRequest(
    Long taskId,
    OpsType opsType,
    Boolean force,
    String reason,
    String dealer,
    Instant requestDt) {

    public OperationRequest {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(opsType);
        if (Objects.equals(OpsType.SUBMIT,opsType )){
            throw new IllegalArgumentException("Please the submitRequest to submit request.");
        }
        force = force != null && force;
        requestDt = Objects.isNull(requestDt) ? Instant.now(): requestDt;
    }
}
