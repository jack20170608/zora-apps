package top.ilovemyhome.dagtask.scheduler.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.Constants;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationInfo;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin REST API endpoints for managing agent registration requests.
 * <p>
 * These endpoints require authentication and are only accessible by
 * administrators for reviewing and approving/rejecting pending registrations.
 * </p>
 */
@Path(Constants.API_SCHEDULER)
@Produces(MediaType.APPLICATION_JSON)
public class RegistrationAdminApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationAdminApi.class);

    private final RegistrationService registrationService;

    @Inject
    public RegistrationAdminApi(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * List all pending registration requests waiting for approval.
     *
     * @param limit maximum number of results to return (default: 50)
     * @return list of pending registrations
     */
    @GET
    @Path("/admin/registrations/pending")
    public Response listPending(@QueryParam("limit") Integer limit) {
        int max = limit != null ? limit : 50;
        List<AgentRegistration> registrations = registrationService.listByStatus(
            AgentRegistration.Status.PENDING, max);

        List<AgentRegistrationInfo> info = registrations.stream()
            .map(r -> new AgentRegistrationInfo(
                r.registrationId(),
                r.agentName(),
                r.description(),
                r.status().name(),
                r.clientAddress(),
                r.createdAt(),
                r.expiresAt(),
                r.processedBy(),
                r.processedAt(),
                r.notes()
            ))
            .collect(Collectors.toList());

        return Response.ok()
            .entity(ResEntityHelper.ok("Pending registrations retrieved", info))
            .build();
    }

    /**
     * Approve a pending registration request.
     * <p>
     * This will generate a token and push it to the agent's callback URL.
     *
     * @param registrationId the registration ID to approve
     * @param notes optional admin notes about this approval
     * @param processedBy the username of the admin approving this request
     * @return success response
     */
    @POST
    @Path("/admin/registrations/{registrationId}/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response approve(@jakarta.ws.rs.PathParam("registrationId") String registrationId,
                            @QueryParam("processedBy") String processedBy,
                            String notes) {
        LOGGER.info("Approving registration: registrationId={}, processedBy={}",
            registrationId, processedBy);
        registrationService.approve(registrationId, processedBy, notes);
        return Response.ok()
            .entity(ResEntityHelper.ok("Registration approved successfully", null))
            .build();
    }

    /**
     * Reject a pending registration request.
     *
     * @param registrationId the registration ID to reject
     * @param notes reason for rejection
     * @param processedBy the username of the admin rejecting this request
     * @return success response
     */
    @POST
    @Path("/admin/registrations/{registrationId}/reject")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reject(@jakarta.ws.rs.PathParam("registrationId") String registrationId,
                           @QueryParam("processedBy") String processedBy,
                           String notes) {
        LOGGER.info("Rejecting registration: registrationId={}, processedBy={}, notes={}",
            registrationId, processedBy, notes);
        registrationService.reject(registrationId, processedBy, notes);
        return Response.ok()
            .entity(ResEntityHelper.ok("Registration rejected", null))
            .build();
    }
}
