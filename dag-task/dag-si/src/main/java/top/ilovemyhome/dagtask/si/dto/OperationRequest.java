package top.ilovemyhome.dagtask.si.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to kill a task in this agent.")
public record OperationRequest(
    @Schema(description = "Unique ID of the task", required = true)
    Long taskId,
    @Schema(description = "The flag to identify it's a force kill or not", required = false)
    boolean force,
    @Schema(description = "The reason of kill the task", required = false)
    String reason) {
}
