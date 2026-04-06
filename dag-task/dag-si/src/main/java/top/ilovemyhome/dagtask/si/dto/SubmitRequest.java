package top.ilovemyhome.dagtask.si.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to submit a new task for execution")
    public record SubmitRequest(
            @Schema(description = "Unique ID of the task", required = true)
            Long taskId,
            @Schema(description = "Fully qualified class name of the task execution implementation", required = true)
            String executionClass,
            @Schema(description = "Optional JSON input data for the task")
            String input
    ) {}
