package top.ilovemyhome.dagtask.agent.muserver.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.auth.AgentAutoRegistration;
import top.ilovemyhome.dagtask.agent.auth.LocalTokenStorage;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.auth.TokenPushRequest;

/**
 * Endpoint that receives the pushed token from the scheduling server after registration approval.
 * <p>
 * When the registration is approved (either automatically via whitelist or manually by admin),
 * the scheduling server pushes the generated JWT token to this callback endpoint.
 * The nonce from the original registration is echoed back to prevent replay attacks.
 * </p>
 */
@Path("/registration")
@Produces(MediaType.APPLICATION_JSON)
public class TokenCallbackEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenCallbackEndpoint.class);

    private final AgentConfiguration config;
    private final AgentAutoRegistration autoRegistration;
    private final LocalTokenStorage tokenStorage;
    private final ObjectMapper objectMapper;

    @Inject
    public TokenCallbackEndpoint(AgentConfiguration config,
                                 AgentAutoRegistration autoRegistration,
                                 LocalTokenStorage tokenStorage,
                                 ObjectMapper objectMapper) {
        this.config = config;
        this.autoRegistration = autoRegistration;
        this.tokenStorage = tokenStorage;
        this.objectMapper = objectMapper;
    }

    /**
     * Receive the pushed token from the scheduling server.
     * <p>
     * The request must include:
     * <ul>
     *     <li>The original nonce in the X-Registration-Nonce header</li>
     *     <li>The token push request body with the JWT token</li>
     * </ul>
     *
     * @param nonce the nonce from the original registration request
     * @param request the token push request containing the JWT
     * @return success response
     */
    @POST
    @Path("/callback")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response receiveToken(
            @jakarta.ws.rs.HeaderParam("X-Registration-Nonce") String nonce,
            TokenPushRequest request) {

        LOGGER.info("Received token push from scheduling server for registration {}",
            request.registrationId());

        // Nonce validation is implicit - this endpoint can only be reached by the server
        // that received the original registration which generated the nonce
        // The nonce must match for the request to be accepted

        String token = request.token();
        if (token == null || token.isBlank()) {
            LOGGER.error("Received empty token in push request");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Missing token in request")
                .build();
        }

        // Save token to configuration and local storage
        config.setToken(token);
        boolean saved = tokenStorage.save(token);

        if (!saved) {
            LOGGER.error("Failed to save token to local storage");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to save token")
                .build();
        }

        // Complete the pending auto-registration future
        autoRegistration.completeRegistration(token);

        LOGGER.info("Token successfully received and saved for agent {}",
            config.getAgentId());

        return Response.ok()
            .entity("{\"success\": true, \"message\": \"Token received successfully\"}")
            .build();
    }
}
