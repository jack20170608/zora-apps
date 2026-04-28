package top.ilovemyhome.dagtask.server.interfaces.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.agent.AgentWhitelist;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;
import top.ilovemyhome.dagtask.si.persistence.AgentWhitelistDao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * REST API endpoints for managing agent whitelist entries.
 */
@Path("/api/v1/agents/whitelist")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentWhitelistAdminApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentWhitelistAdminApi.class);

    private final AgentWhitelistDao agentWhitelistDao;

    @Inject
    public AgentWhitelistAdminApi(AgentWhitelistDao agentWhitelistDao) {
        this.agentWhitelistDao = agentWhitelistDao;
    }

    @GET
    public Response listAll(@QueryParam("enabled") Boolean enabled) {
        List<AgentWhitelist> list;
        if (enabled != null) {
            list = enabled ? agentWhitelistDao.findAllEnabled() : agentWhitelistDao.findAll();
            list = list.stream().filter(e -> e.isEnabled() == enabled).toList();
        } else {
            list = agentWhitelistDao.findAll();
        }
        LOGGER.info("Listed whitelist entries: count={}, enabledFilter={}", list.size(), enabled);
        return Response.ok().entity(ResEntityHelper.ok("Whitelist entries retrieved successfully", list)).build();
    }

    @POST
    public Response create(AgentWhitelist entry) {
        if (entry == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Whitelist entry must not be null"))
                .build();
        }
        if (isBlank(entry.getIpSegment()) && isBlank(entry.getAgentId())) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Either ipSegment or agentId must be provided"))
                .build();
        }

        entry.setEnabled(true);
        entry.setCreatedAt(Instant.now());
        entry.setUpdatedAt(Instant.now());
        agentWhitelistDao.create(entry);
        LOGGER.info("Created whitelist entry: id={}, ipSegment={}, agentId={}",
            entry.getId(), entry.getIpSegment(), entry.getAgentId());
        return Response.ok().entity(ResEntityHelper.ok("Whitelist entry created successfully", entry)).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        Optional<AgentWhitelist> opt = agentWhitelistDao.findOne(id);
        if (opt.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Whitelist entry not found: " + id))
                .build();
        }
        return Response.ok().entity(ResEntityHelper.ok("Whitelist entry retrieved successfully", opt.get())).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, AgentWhitelist entry) {
        if (entry == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Whitelist entry must not be null"))
                .build();
        }
        Optional<AgentWhitelist> existingOpt = agentWhitelistDao.findOne(id);
        if (existingOpt.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Whitelist entry not found: " + id))
                .build();
        }

        AgentWhitelist existing = existingOpt.get();
        if (entry.getIpSegment() != null) {
            existing.setIpSegment(entry.getIpSegment());
        }
        if (entry.getAgentId() != null) {
            existing.setAgentId(entry.getAgentId());
        }
        if (entry.getDescription() != null) {
            existing.setDescription(entry.getDescription());
        }
        existing.setEnabled(entry.isEnabled());
        existing.setUpdatedAt(Instant.now());

        agentWhitelistDao.update(id, existing);
        LOGGER.info("Updated whitelist entry: id={}", id);
        return Response.ok().entity(ResEntityHelper.ok("Whitelist entry updated successfully", existing)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        Optional<AgentWhitelist> existingOpt = agentWhitelistDao.findOne(id);
        if (existingOpt.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Whitelist entry not found: " + id))
                .build();
        }
        agentWhitelistDao.delete(id);
        LOGGER.info("Deleted whitelist entry: id={}", id);
        return Response.ok().entity(ResEntityHelper.ok("Whitelist entry deleted successfully", null)).build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
