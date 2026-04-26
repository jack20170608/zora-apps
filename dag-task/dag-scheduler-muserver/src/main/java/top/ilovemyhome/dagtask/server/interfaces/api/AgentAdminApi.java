package top.ilovemyhome.dagtask.server.interfaces.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.agent.Agent;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;
import top.ilovemyhome.dagtask.si.persistence.AgentDao;
import top.ilovemyhome.dagtask.si.persistence.AgentStatusDao;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API endpoints for managing agents.
 */
@Path("/api/v1/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentAdminApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentAdminApi.class);

    private final AgentDao agentDao;
    private final AgentStatusDao agentStatusDao;

    @Inject
    public AgentAdminApi(AgentDao agentDao, AgentStatusDao agentStatusDao) {
        this.agentDao = agentDao;
        this.agentStatusDao = agentStatusDao;
    }

    @GET
    public Response listAll() {
        List<Agent> agents = agentDao.findAll();
        List<Map<String, Object>> result = agents.stream()
            .map(this::toAgentMap)
            .collect(Collectors.toList());
        LOGGER.info("Listed agents: count={}", result.size());
        return Response.ok().entity(ResEntityHelper.ok("Agents retrieved successfully", result)).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        var agentOpt = agentDao.findByAgentId(id);
        if (agentOpt.isEmpty()) {
            LOGGER.warn("Agent not found: id=[{}]", id);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Agent not found: " + id))
                .build();
        }
        Map<String, Object> agent = toAgentMap(agentOpt.get());
        LOGGER.info("Retrieved agent: id=[{}]", id);
        return Response.ok().entity(ResEntityHelper.ok("Agent retrieved successfully", agent)).build();
    }

    @GET
    @Path("/{id}/metrics")
    public Response getMetrics(@PathParam("id") String id) {
        var statusOpt = agentStatusDao.findByAgentId(id);
        Map<String, Object> metrics = new HashMap<>();
        if (statusOpt.isPresent()) {
            AgentStatus status = statusOpt.get();
            metrics.put("running", status.isRunning());
            metrics.put("pendingTasks", status.getPendingTasks());
            metrics.put("runningTasks", status.getRunningTasks());
            metrics.put("finishedTasks", status.getFinishedTasks());
            metrics.put("maxConcurrentTasks", status.getMaxConcurrentTasks());
        } else {
            metrics.put("running", false);
            metrics.put("pendingTasks", 0);
            metrics.put("runningTasks", 0);
            metrics.put("finishedTasks", 0);
        }
        metrics.put("timestamp", Instant.now().toString());
        LOGGER.info("Retrieved metrics for agent: id=[{}]", id);
        return Response.ok().entity(ResEntityHelper.ok("Agent metrics retrieved successfully", metrics)).build();
    }

    private Map<String, Object> toAgentMap(Agent agent) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", agent.getAgentId());
        map.put("name", agent.getName());
        map.put("status", agent.getStatus().name().toLowerCase());
        map.put("version", "1.0.0");
        map.put("lastHeartbeat", agent.getLastHeartbeatAt());

        var statusOpt = agentStatusDao.findByAgentId(agent.getAgentId());
        if (statusOpt.isPresent()) {
            AgentStatus status = statusOpt.get();
            map.put("host", status.getAgentUrl());
            map.put("capabilities", List.of(status.getSupportedExecutionKeys().split(",")));
            map.put("currentTasks", status.getRunningTasks());
            map.put("maxTasks", status.getMaxConcurrentTasks());
            map.put("cpuUsage", 0.0);
            map.put("memoryUsage", 0.0);
            map.put("diskUsage", 0.0);
            map.put("tags", List.of());
        }
        return map;
    }
}
