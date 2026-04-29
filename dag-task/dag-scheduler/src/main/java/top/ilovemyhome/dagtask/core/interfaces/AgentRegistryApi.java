package top.ilovemyhome.dagtask.core.interfaces;

import io.muserver.MuRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.agent.AgentRegistryService;
import top.ilovemyhome.dagtask.si.Constants;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterResponse;
import top.ilovemyhome.dagtask.si.agent.AgentStatusReport;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;

import java.util.List;

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
@Path(Constants.API_SCHEDULER)
@Produces(MediaType.APPLICATION_JSON)
public class AgentRegistryApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRegistryApi.class);

    private final AgentRegistryService agentRegistryService;

    /**
     * Creates a new SchedulerAgentApi with injected dependencies.
     *
     * @param agentRegistryService the service that handles agent registry operations
     */
    @Inject
    public AgentRegistryApi(AgentRegistryService agentRegistryService) {
        this.agentRegistryService = agentRegistryService;
    }

    /**
     * Endpoint for agent registration.
     * <p>
     * Agents call this endpoint when they start up to register themselves
     * with the scheduling center. The registration contains agent capabilities
     * and endpoint information that the scheduler uses to dispatch tasks.
     * If the request includes {@code generateToken=true} and registration succeeds,
     * a new JWT token will be generated and returned in the response.
     *
     * @param registration the agent registration information containing agent ID, URL,
     *                     maximum concurrent tasks, and supported execution keys
     * @return HTTP 200 OK if registration succeeded with {@link AgentRegisterResponse} data,
     *         HTTP 400 Bad Request if failed
     */
    @POST
    @Path(Constants.API_REGISTER)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(AgentRegisterRequest registration, @Context MuRequest muRequest) {
        String clientIp = muRequest != null ? muRequest.clientIP() : null;
        LOGGER.debug("Received registration request from agent: {}, clientIp: {}", registration.agentId(), clientIp);
        AgentRegisterResponse response = agentRegistryService.registerAgent(registration, clientIp);

        if (response.success()) {
            return Response.ok().entity(ResEntityHelper.ok(response.message(), response)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest(response.message(), response))
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
    @Path(Constants.API_UNREGISTER)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response unregister(AgentUnregistration unregistration) {
        LOGGER.debug("Received unregistration request from agent: {}", unregistration.agentId());
        boolean success = agentRegistryService.unregisterAgent(unregistration);
        if (success) {
            return Response.ok().entity(ResEntityHelper.ok("Agent unregistered successfully", null)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Unregistration failed, agent not found"))
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
     * @param taskExecuteResult the task result report containing task ID, success flag,
     *                         and output JSON
     * @return HTTP 200 OK if result processed successfully, HTTP 400 Bad Request if failed
     */
    @POST
    @Path(Constants.API_REPORT_RESULT)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reportResult(List<TaskExecuteResult> taskExecuteResult) {
        LOGGER.debug("Received task result for tasks [{}]", taskExecuteResult);
        boolean success = agentRegistryService.reportTaskResult(taskExecuteResult);
        if (success) {
            return Response.ok().entity(ResEntityHelper.ok("Task result processed successfully", null)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Failed to process task result"))
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
    @Path(Constants.API_REPORT_STATUS)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reportStatus(AgentStatusReport statusReport) {
        LOGGER.trace("Received status report from agent: {}", statusReport.agentId());
        boolean success = agentRegistryService.reportAgentStatus(statusReport);
        if (success) {
            return Response.ok().entity(ResEntityHelper.ok("Status updated successfully", null)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Failed to update agent status"))
                .build();
        }
    }

}
