package top.ilovemyhome.dagtask.server.interfaces.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.agent.AgentRegistryItem;
import top.ilovemyhome.dagtask.si.dto.AgentRegistrySearchCriteria;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;
import top.ilovemyhome.dagtask.si.persistence.AgentRegistryDao;

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

    private final AgentRegistryDao agentRegistryDao;

    @Inject
    public AgentAdminApi(AgentRegistryDao agentRegistryDao) {
        this.agentRegistryDao = agentRegistryDao;
    }

    @GET
    public Response listAll() {
        AgentRegistrySearchCriteria criteria = AgentRegistrySearchCriteria.builder().build();
        List<AgentRegistryItem> agents = agentRegistryDao.search(criteria);
        List<Map<String, Object>> result = agents.stream()
            .map(this::toAgentMap)
            .collect(Collectors.toList());
        LOGGER.info("Listed agents: count={}", result.size());
        return Response.ok().entity(ResEntityHelper.ok("Agents retrieved successfully", result)).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        AgentRegistrySearchCriteria criteria = AgentRegistrySearchCriteria.builder()
            .withAgentId(id)
            .build();
        List<AgentRegistryItem> agents = agentRegistryDao.search(criteria);
        if (agents.isEmpty()) {
            LOGGER.warn("Agent not found: id=[{}]", id);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Agent not found: " + id))
                .build();
        }
        Map<String, Object> agent = toAgentMap(agents.get(0));
        LOGGER.info("Retrieved agent: id=[{}]", id);
        return Response.ok().entity(ResEntityHelper.ok("Agent retrieved successfully", agent)).build();
    }

    @GET
    @Path("/{id}/metrics")
    public Response getMetrics(@PathParam("id") String id) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("cpuUsage", 25.5);
        metrics.put("memoryUsage", 60.0);
        metrics.put("diskUsage", 45.0);
        metrics.put("timestamp", Instant.now().toString());
        LOGGER.info("Retrieved metrics for agent: id=[{}]", id);
        return Response.ok().entity(ResEntityHelper.ok("Agent metrics retrieved successfully", metrics)).build();
    }

    private Map<String, Object> toAgentMap(AgentRegistryItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getAgentId());
        map.put("name", item.getAgentId());
        map.put("host", item.getAgentUrl());
        map.put("status", item.isRunning() ? "online" : "offline");
        map.put("version", "1.0.0");
        map.put("capabilities", item.getSupportedExecutionKeys());
        map.put("currentTasks", item.getRunningTasks());
        map.put("maxTasks", item.getMaxConcurrentTasks());
        map.put("cpuUsage", 0.0);
        map.put("memoryUsage", 0.0);
        map.put("diskUsage", 0.0);
        map.put("lastHeartbeat", item.getLastHeartbeatAt());
        map.put("tags", List.of());
        return map;
    }
}
