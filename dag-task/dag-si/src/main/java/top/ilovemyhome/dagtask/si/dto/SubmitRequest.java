package top.ilovemyhome.dagtask.si.dto;

import top.ilovemyhome.dagtask.si.enums.OpsType;
import top.ilovemyhome.dagtask.si.enums.PriorityType;
import top.ilovemyhome.dagtask.si.enums.TaskType;

import java.time.Instant;
import java.util.Objects;

public record SubmitRequest(
    Long taskId
    , TaskType taskType
    , OpsType opsType
    , PriorityType priorityType
    , String executionClass
    , String input
    , String dealer
    , Instant requestDt) {

    public SubmitRequest{
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(taskType);

        opsType = OpsType.SUBMIT;
        requestDt = requestDt == null ? Instant.now()  : requestDt;
    }

    public SubmitRequest(Long taskId, TaskType taskType, String executionClass, String input){
        this(taskId, taskType, null, PriorityType.NORMAL, executionClass, input, "SYSTEM", Instant.now());
    }
}
