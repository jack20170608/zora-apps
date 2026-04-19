package top.ilovemyhome.dagtask.scheduler.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.Constants;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationRequest;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationResponse;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;

/**
 * Public REST API endpoint for agent self-registration.
 * <p>
 * This endpoint is publicly accessible (no authentication required) because
 * agents need to initiate registration before they have a token.
 * Agents call this endpoint on first startup when no token exists locally.
 * </p>
 */
@Path(Constants.API_SCHEDULER)
@Produces(MediaType.APPLICATION_JSON)
public class PublicRegistrationApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicRegistrationApi.class);

    private final RegistrationService registrationService;

    @Inject
    public PublicRegistrationApi(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Start the agent registration process.
     * <p>
     * Agents call this endpoint when they start up without a valid token.
     * The registration will be:
     * <ul>
     *     <li>Automatically approved if the agent name matches the whitelist</li>
     *     <li>Set to pending for manual admin approval if no match</li>
     * </ul>
     * When auto-approved, the token will be pushed to the agent callback URL immediately.
     *
     * @param request the registration information including agent name, description,
     *                labels, and callback URL
     * @return response with registration status and ID
     */
    @POST
    @Path("/registration/start")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startRegistration(AgentRegistrationRequest request) {
        LOGGER.info("Received agent registration request: name={}, callback={}",
            request.name(), request.callbackUrl());

        String clientAddress = getClientAddress();
        AgentRegistrationResponse result = registrationService.createRegistration(request, clientAddress);

        if (result.success()) {
            return Response.ok()
                .entity(ResEntityHelper.ok("Registration created successfully", result))
                .build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest(result.message()))
                .build();
        }
    }

    private String getClientAddress() {
        // In a real container environment, this would be extracted from the request
        // For now, return null - container-specific extraction can be added if needed
        return null;
    }
}
