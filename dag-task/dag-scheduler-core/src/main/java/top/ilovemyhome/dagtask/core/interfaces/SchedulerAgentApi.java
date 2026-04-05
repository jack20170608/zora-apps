package top.ilovemyhome.dagtask.core.interfaces;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.agent.AgentRegistryService;
import top.ilovemyhome.dagtask.si.agent.AgentRegistration;
import top.ilovemyhome.dagtask.si.agent.AgentStatusReport;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.agent.TaskResultReport;

/**
 * REST API endpoints for agent communication with the scheduling center.
 * <p>
 * This is the server-side API that receives requests from agent clients.
 * Agents make HTTP calls to these endpoints to:
 * <ul>
 *     <li>Register themselves with the scheduling center when starting up</li>
 *     <li>Unregister themselves when shutting down gracefully</li>
 *     <li>Report the result of a completed task execution</li>
 *     <li>Periodically report their current status and queue statistics (heartbeat)</li>
 * </ul>
 * <p>
 * All endpoints accept JSON requests and return JSON responses.
 * Successful requests return HTTP 200 OK with a success indicator.
 * Failed requests return appropriate HTTP status codes (400 Bad Request, 500 Internal Server Error).
 *
 * @see top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient AgentSchedulerClient interface (client side)
 * @see AgentRegistryService AgentRegistryService (business logic layer)
 */
@Path("/api/v1/agent")
@Produces(MediaType.APPLICATION_JSON)
public class SchedulerAgentApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerAgentApi.class);

    private final AgentRegistryService agentRegistryService;

    /**
     * Creates a new SchedulerAgentApi with injected dependency.
     *
     * @param agentRegistryService the service that handles agent registry operations
     */
    @Inject
    public SchedulerAgentApi(AgentRegistryService agentRegistryService) {
        this.agentRegistryService = agentRegistryService;
    }

    /**
     * Endpoint for agent registration.
     * <p>
     * Agents call this endpoint when they start up to register themselves
     * with the scheduling center. The registration contains agent capabilities
     * and endpoint information that the scheduler uses to dispatch tasks.
     *
     * @param registration the agent registration information containing agent ID, URL,
     *                     maximum concurrent tasks, and supported execution keys
     * @return HTTP 200 OK if registration succeeded, HTTP 400 Bad Request if failed
     */
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(AgentRegistration registration) {
        LOGGER.debug("Received registration request from agent: {}", registration.agentId());
        boolean success = agentRegistryService.registerAgent(registration);
        if (success) {
            return Response.ok().entity(new SuccessResponse("Agent registered successfully")).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Registration failed"))
                .build();
        }
    }

    /**
     * Endpoint for agent unregistration.
     * <p>
     * Agents call this endpoint when they are shutting down gracefully
     * to unregister themselves from the scheduling center. This prevents
     * the scheduler from trying to dispatch tasks to a stopped agent.
     *
     * @param unregistration the agent unregistration information containing the agent ID
     * @return HTTP 200 OK if unregistration succeeded, HTTP 400 Bad Request if failed
     */
    @POST
    @Path("/unregister")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response unregister(AgentUnregistration unregistration) {
        LOGGER.debug("Received unregistration request from agent: {}", unregistration.agentId());
        boolean success = agentRegistryService.unregisterAgent(unregistration);
        if (success) {
            return Response.ok().entity(new SuccessResponse("Agent unregistered successfully")).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Unregistration failed, agent not found"))
                .build();
        }
    }

    /**
     * Endpoint for reporting task execution result after an agent completes a task.
     * <p>
     * After an agent finishes executing a task (whether success or failure), it calls
     * this endpoint to report the result back to the scheduling center. The scheduler
     * then updates the task status and proceeds to schedule any successor tasks.
     *
     * @param taskResultReport the task result report containing task ID, success flag,
     *                         and output JSON
     * @return HTTP 200 OK if result processed successfully, HTTP 400 Bad Request if failed
     */
    @POST
    @Path("/task/result")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reportTaskResult(TaskResultReport taskResultReport) {
        LOGGER.debug("Received task result from agent [{}] for task [{}]",
            taskResultReport.agentId(), taskResultReport.taskId());
        boolean success = agentRegistryService.reportTaskResult(taskResultReport);
        if (success) {
            return Response.ok().entity(new SuccessResponse("Task result processed successfully")).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Failed to process task result"))
                .build();
        }
    }

    /**
     * Endpoint for periodic agent status reporting (heartbeat).
     * <p>
     * Agents call this endpoint periodically (e.g., every 30 seconds) to report
     * their current status including queue statistics. This helps the scheduler
     * monitor agent health and make informed load balancing decisions.
     *
     * @param statusReport the agent status report containing current queue counts
     *                     and running state
     * @return HTTP 200 OK if status updated successfully, HTTP 400 Bad Request if failed
     */
    @POST
    @Path("/status")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reportStatus(AgentStatusReport statusReport) {
        LOGGER.trace("Received status report from agent: {}", statusReport.agentId());
        boolean success = agentRegistryService.reportAgentStatus(statusReport);
        if (success) {
            return Response.ok().entity(new SuccessResponse("Status updated successfully")).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Failed to update agent status"))
                .build();
        }
    }

    /**
     * Standard response record for successful API calls.
     * @param message descriptive success message
     */
    public record SuccessResponse(String message) {}

    /**
     * Standard response record for failed API calls.
     * @param error description of the error that occurred
     */
    public record ErrorResponse(String error) {}
}
