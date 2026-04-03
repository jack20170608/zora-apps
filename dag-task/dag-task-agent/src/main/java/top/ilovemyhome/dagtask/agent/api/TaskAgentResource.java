package top.ilovemyhome.dagtask.agent.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.muserver.rest.Description;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
import top.ilovemyhome.dagtask.agent.core.TaskExecutionManager;
import top.ilovemyhome.dagtask.agent.client.DagServerClient;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.TaskFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Combined REST API resource for DAG task agent.
 * Delegates all task lifecycle management to {@link TaskExecutionManager}.
 * Provides all API endpoints:
 * - POST /api/submit - Submit task for execution
 * - POST /api/kill/{taskId} - Kill a running or pending task
 * - POST /api/force-ok/{taskId} - Force task to complete successfully
 * - GET /api/health - Get agent health status with queue statistics
 * - GET /api/ping - Heartbeat check
 */
@Path("/api")
@Description(value = "DAG Task Agent API", details = "All API endpoints for the DAG task execution agent")
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
    private final TaskExecutionManager executionManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskAgentResource.class);

    public TaskAgentResource(AgentConfiguration config, DagServerClient dagServerClient,
                             ExecutorService taskExecutor, DagTaskAgent agent) {
        this.agent = agent;
        ObjectMapper objectMapper = new ObjectMapper();
        TaskFactory taskFactory = new DefaultTaskFactory();
        this.executionManager = new TaskExecutionManager(config, dagServerClient, taskExecutor, objectMapper, taskFactory);
    }

    public TaskAgentResource(DagTaskAgent agent, TaskExecutionManager executionManager) {
        this.agent = agent;
        this.executionManager = executionManager;
    }

    /**
     * Starts the underlying task execution manager.
     * Called after server startup by DagTaskAgent.
     */
    public void startQueueProcessor() {
        executionManager.start();
    }

    // ========== Submit endpoint ==========

    @POST
    @Path("/submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Submit task for execution", details = "Receives a task execution request from the server and queues it for execution. Rejects if pending queue is full.")
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

        LOGGER.info("Received task submission: taskId={}, executionClass={}", taskId, executionClass);

        TaskExecutionManager.SubmissionResult result =
                executionManager.submit(taskId, executionClass, inputJson);

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

    @Schema(description = "Request to submit a new task for execution")
    public record SubmitRequest(
            @Schema(description = "Unique ID of the task", required = true)
            Long taskId,
            @Schema(description = "Fully qualified class name of the task execution implementation", required = true)
            String executionClass,
            @Schema(description = "Optional JSON input data for the task")
            String input
    ) {}

    // ========== Kill endpoint ==========

    @POST
    @Path("/kill/{taskId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Kill running task", details = "Attempts to interrupt and kill a running or pending task")
    @Operation(summary = "Kill a running or pending task",
               description = "Attempts to interrupt and kill a task that is either waiting in the pending queue or currently running. " +
                             "Once killed, the task is marked as failed and moved to the finished queue.")
    @Parameter(name = "taskId", description = "ID of the task to kill", required = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kill operation completed successfully",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "404", description = "Task not found in pending or running queues",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @ApiResponse(responseCode = "500", description = "Failed to cancel the running task",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    public Response kill(@PathParam("taskId") Long taskId) {
        TaskExecutionManager.KillResult result = executionManager.kill(taskId);

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
    @Path("/force-ok/{taskId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Force task success", details = "Marks a running or pending task as completed successfully")
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
    public Response forceOk(@PathParam("taskId") Long taskId) {
        TaskExecutionManager.ForceOkResult result = executionManager.forceOk(taskId);

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
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Get agent health", details = "Returns the current health status including queue statistics")
    @Operation(summary = "Get agent health status",
               description = "Returns detailed health statistics about the agent including " +
                             "running status, configuration, queue sizes, and supported execution keys. " +
                             "Used for monitoring and health checks.")
    @ApiResponse(responseCode = "200", description = "Health status retrieved successfully",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response health() {
        AgentConfiguration config = agent.getConfig();
        TaskExecutionManager.Statistics stats = executionManager.getStatistics();

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
        health.put("finishedSize", stats.finishedSize());

        return Response.ok(health)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
    }

    // ========== Ping endpoint ==========

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    @Description(value = "Ping the agent", details = "Returns pong if agent is alive")
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

    private static class DefaultTaskFactory implements TaskFactory {
        // Use default implementation
    }
}
