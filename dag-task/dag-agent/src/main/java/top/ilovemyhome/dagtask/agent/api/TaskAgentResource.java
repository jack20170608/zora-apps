package top.ilovemyhome.dagtask.agent.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgent;
import top.ilovemyhome.dagtask.agent.dto.ForceNokResult;
import top.ilovemyhome.dagtask.agent.dto.ForceOkResult;
import top.ilovemyhome.dagtask.agent.dto.FreeResult;
import top.ilovemyhome.dagtask.agent.dto.HoldResult;
import top.ilovemyhome.dagtask.agent.dto.KillResult;
import top.ilovemyhome.dagtask.agent.dto.SubmissionResult;
import top.ilovemyhome.dagtask.agent.core.TaskExecutionEngine;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.Constants;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.dto.OperationRequest;
import top.ilovemyhome.dagtask.si.dto.SubmitRequest;
import top.ilovemyhome.dagtask.si.enums.OpsType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Combined REST API resource for DAG task agent.
 * Delegates all task lifecycle management to {@link TaskExecutionEngine}.
 * Provides all API endpoints:
 * - POST /api/submit - Submit task for execution
 * - POST /api/kill/{taskId} - Kill a running or pending task
 * - POST /api/force-ok/{taskId} - Force task to complete successfully
 * - GET /api/health - Get agent health status with queue statistics
 * - GET /api/ping - Heartbeat check
 */
@Path(Constants.API_VERSION)
@OpenAPIDefinition(
        info = @Info(
                title = "DAG Task Agent API",
                version = "1.0.0",
                description = "REST API for the DAG task execution agent that executes tasks assigned by the DAG scheduling server",
                contact = @Contact(name = "DAG Task Project")
        )
)
public class TaskAgentResource {

    private final DagTaskAgent agent;
    private final TaskExecutionEngine executionEngine;

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskAgentResource.class);

    public TaskAgentResource(AgentConfiguration config, AgentSchedulerClient agentSchedulerClient,
                             ExecutorService taskExecutor, DagTaskAgent agent) {
        this.agent = agent;
        ObjectMapper objectMapper = new ObjectMapper();
        this.executionEngine = new TaskExecutionEngine(config, agentSchedulerClient, taskExecutor, objectMapper);
    }

    public TaskAgentResource(DagTaskAgent agent, TaskExecutionEngine executionEngine) {
        this.agent = agent;
        this.executionEngine = executionEngine;
    }

    /**
     * Starts the underlying task execution manager.
     * Called after server startup by DagTaskAgent.
     */
    public void startQueueProcessor() {
        executionEngine.start();
    }

    // ========== Submit endpoint ==========

    @POST
    @Path(Constants.API_SUBMIT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Submit a new task for execution",
               description = "Receives a task execution request from the DAG scheduling server and queues it for execution. " +
                             "Rejects the request with HTTP 429 if the pending queue is full (backpressure).")
    @RequestBody(description = "Task submission request containing task ID, execution class name, and input JSON",
                  required = true,
                  content = @Content(schema = @Schema(implementation = SubmitRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Task accepted for execution",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "400", description = "Invalid request or failed to create execution",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "429", description = "Pending queue is full, too many pending tasks",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response submit(SubmitRequest request) {
        Long taskId = request.taskId();
        String executionClass = request.executionClass();
        String inputJson = request.input();
        LOGGER.info("Received task submission: taskId={}, executionClass={}, input={}",
            taskId, executionClass, inputJson);
        SubmissionResult result = executionEngine.submit(taskId, executionClass, inputJson);
        if (!result.accepted()) {
            if (result.queueFull()) {
                return Response.status(Response.Status.TOO_MANY_REQUESTS)
                        .entity(Map.of(
                                "error", result.message(),
                                "capacity", result.capacity(),
                                "currentSize", result.currentSize()
                        ))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", result.message()))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .build();
        }

        return Response.accepted()
                .entity(Map.of(
                        "success", true,
                        "message", result.message(),
                        "taskId", result.taskId(),
                        "pendingPosition", result.pendingPosition()
                ))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
    }



    // ========== Kill endpoint ==========

    @POST
    @Path(Constants.API_KILL)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Kill a running or pending task",
               description = "Attempts to interrupt and kill a task that is either waiting in the pending queue or currently running. " +
                             "Once killed, the task is marked as failed and moved to the finished queue.")
    @RequestBody(description = "Kill operation request containing task ID",
                  required = true,
                  content = @Content(schema = @Schema(implementation = OperationRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kill operation completed successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "400", description = "Invalid operation type",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "404", description = "Task not found in pending or running queues",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "500", description = "Failed to kill the running task",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response kill(OperationRequest request) {
        if (!Objects.equals(request.opsType() , OpsType.KILL)){
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Operation type " + request.opsType() + " is not kill."))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
        }
        KillResult result = executionEngine.kill(request.taskId());

        if (!result.success()) {
            if (!result.found()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", result.message()))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", result.message()))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .build();
        }

        return Response.ok(Map.of(
                "success", true,
                "message", result.message()
        ))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
    }

    // ========== Force Ok endpoint ==========

    @POST
    @Path(Constants.API_FORCE_OK)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Force a task to complete successfully",
               description = "Forces a task that is either waiting in the pending queue or currently running " +
                             "to complete immediately with a successful result. " +
                             "Reports the successful result back to the DAG server immediately.")
    @Parameter(name = "taskId", description = "ID of the task to force as successful", required = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Force-ok operation completed successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "404", description = "Task not found in pending or running queues",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response forceOk(OperationRequest request) {
        ForceOkResult result = executionEngine.forceOk(request.taskId());
        if (!Objects.equals(request.opsType() , OpsType.FORCE_OK)){
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Operation type " + request.opsType() + " is not forceOk."))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
        }

        if (!result.success()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", result.message()))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .build();
        }

        // Report the result to the scheduler server
        var report = new top.ilovemyhome.dagtask.si.agent.TaskExecuteResult(
            agent.getConfig().getAgentId(), request.taskId(), true, "{\"forced\":true}", java.time.Instant.now()
        );
        executionEngine.reportResult(report);

        return Response.ok(Map.of(
                "success", true,
                "message", result.message()
        ))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
    }

    // ========== Force Nok endpoint ==========

    @POST
    @Path(Constants.API_FORCE_NOK)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Force a task to complete as failed",
               description = "Forces a task that is either waiting in the pending queue or currently running " +
                             "to complete immediately with a failed result. " +
                             "Reports the failed result back to the DAG server immediately.")
    @RequestBody(description = "Force-nok operation request containing task ID",
                  required = true,
                  content = @Content(schema = @Schema(implementation = OperationRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Force-nok operation completed successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "404", description = "Task not found in pending or running queues",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response forceNok(OperationRequest request) {
        ForceNokResult result = executionEngine.forceNok(request.taskId());
        if (!Objects.equals(request.opsType() , OpsType.FORCE_NOK)){
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Operation type " + request.opsType() + " is not forceNok."))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
        }

        if (!result.success()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", result.message()))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .build();
        }

        // Report the result to the scheduler server
        var report = new top.ilovemyhome.dagtask.si.agent.TaskExecuteResult(
            agent.getConfig().getAgentId(), request.taskId(), false, "{\"forced\":false}", java.time.Instant.now()
        );
        executionEngine.reportResult(report);

        return Response.ok(Map.of(
                "success", true,
                "message", result.message()
        ))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
    }

    // ========== Hold endpoint ==========

    @POST
    @Path(Constants.API_HOLD)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Hold a pending or running task",
               description = "Holds a task so it won't be executed. If pending, removes from pending queue. " +
                             "If running, cancels the running task. After holding, the task is not tracked on the agent " +
                             "and will need to be re-dispatched when freed.")
    @RequestBody(description = "Hold operation request containing task ID",
                  required = true,
                  content = @Content(schema = @Schema(implementation = OperationRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hold operation completed successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "400", description = "Invalid operation type",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "404", description = "Task not found in pending or running queues",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response hold(OperationRequest request) {
        HoldResult result = executionEngine.hold(request.taskId());
        if (!Objects.equals(request.opsType() , OpsType.HOLD)){
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Operation type " + request.opsType() + " is not hold."))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
        }

        if (!result.success()) {
            if (!result.found()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", result.message()))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", result.message()))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .build();
        }

        return Response.ok(Map.of(
                "success", true,
                "message", result.message()
        ))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
    }

    // ========== Free endpoint ==========

    @POST
    @Path(Constants.API_FREE)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Release a previously held task",
               description = "Releases a held task so it can be re-dispatched by the scheduler. " +
                             "Since the agent doesn't track held tasks, this mainly confirms the task " +
                             "is not currently executing on this agent.")
    @RequestBody(description = "Free operation request containing task ID",
                  required = true,
                  content = @Content(schema = @Schema(implementation = OperationRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Free operation completed successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "400", description = "Invalid operation type",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "404", description = "Task not found held on this agent",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response free(OperationRequest request) {
        FreeResult result = executionEngine.free(request.taskId());
        if (!Objects.equals(request.opsType() , OpsType.FREE)){
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Operation type " + request.opsType() + " is not free."))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
        }

        if (!result.success()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", result.message()))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .build();
        }

        return Response.ok(Map.of(
                "success", true,
                "message", result.message()
        ))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
    }

    // ========== Health endpoint ==========

    @GET
    @Path(Constants.API_HEALTH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get agent health status",
               description = "Returns detailed health statistics about the agent including " +
                             "running status, configuration, queue sizes, and supported execution keys. " +
                             "Used for monitoring and health checks.")
    @ApiResponse(responseCode = "200", description = "Health status retrieved successfully",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response health() {
        AgentConfiguration config = agent.getConfig();
        TaskExecutionEngine.Statistics stats = executionEngine.getStatistics();

        Map<String, Object> health = new HashMap<>();
        health.put("running", agent.isRunning());
        health.put("agentId", config.getAgentId());
        health.put("agentUrl", config.getAgentUrl());
        health.put("dagServerUrl", config.getDagServerUrl());
        health.put("maxConcurrentTasks", config.getMaxConcurrentTasks());
        health.put("maxPendingTasks", config.getMaxPendingTasks());
        health.put("supportedExecutionKeysCount", stats.supportedExecutionKeysCount());
        health.put("supportedExecutionKeys", stats.supportedExecutionKeys());
        health.put("pendingSize", stats.pendingSize());
        health.put("runningSize", stats.runningSize());

        return Response.ok(health)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
    }

    // ========== Ping endpoint ==========

    @GET
    @Path(Constants.API_PING)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Ping heartbeat check",
               description = "Simple heartbeat check to verify the agent is running and responding. " +
                             "Always returns the string 'pong' with 200 OK status.")
    @ApiResponse(responseCode = "200", description = "Agent is alive",
                 content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(example = "pong")))
    public Response ping() {
        return Response.ok("pong")
                .header("Content-Type", MediaType.TEXT_PLAIN)
                .build();
    }
}
