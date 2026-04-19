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
import top.ilovemyhome.dagtask.si.auth.TokenPushRequest;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;
import top.ilovemyhome.dagtask.scheduler.config.AutoApproveConfig;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Public REST API endpoint for agent self-registration.
 * <p>
 * This endpoint is publicly accessible (no authentication required) because
 * agents need to initiate registration before they have a token.
 * Agents call this endpoint on first startup when no token exists locally.
 * </p>
 * <p>
 * Simplified flow: check whitelist → if match generate token and push → done.
 * No approval workflow is stored. If not match, reject immediately.
 * </p>
 */
@Path(Constants.API_SCHEDULER)
@Produces(MediaType.APPLICATION_JSON)
public class PublicRegistrationApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicRegistrationApi.class);

    private final TokenService tokenService;
    private final TokenPusher tokenPusher;
    private final AutoApproveConfig autoApproveConfig;
    private final SecureRandom random = new SecureRandom();

    @Inject
    public PublicRegistrationApi(TokenService tokenService,
                                 TokenPusher tokenPusher,
                                 AutoApproveConfig autoApproveConfig) {
        this.tokenService = tokenService;
        this.tokenPusher = tokenPusher;
        this.autoApproveConfig = autoApproveConfig;
    }

    /**
     * Start the agent registration process.
     * <p>
     * Agents call this endpoint when they start up without a valid token.
     * The registration will be:
     * <ul>
     *     <li>Automatically approved if the agent name matches the whitelist</li>
     *     <li>Rejected immediately if no match</li>
     * </ul>
     * When approved, the token will be pushed to the agent callback URL immediately.
     * </p>
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

        // Check whitelist match
        boolean approved = autoApproveConfig.isMatch(request.name());
        if (!approved) {
            LOGGER.warn("Agent registration rejected - name '{}' does not match whitelist", request.name());
            return Response.status(Response.Status.FORBIDDEN)
                .entity(ResEntityHelper.badRequest("Agent name not in whitelist, registration rejected"))
                .build();
        }

        String nonce = generateId();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(365, ChronoUnit.DAYS);

        // Generate token
        var tokenResult = tokenService.generateToken(
            request.name(), request.description(), 365, "system");
        String jwt = tokenService.generateJwt(tokenResult);

        // Push token to callback
        TokenPushRequest pushRequest = new TokenPushRequest(
            null,
            jwt,
            tokenResult.tokenId(),
            tokenResult.expiresAt(),
            tokenResult.name()
        );

        boolean pushed = tokenPusher.pushToken(request.callbackUrl(), nonce, pushRequest);

        if (pushed) {
            LOGGER.info("Agent registration auto-approved and token pushed: name={}", request.name());
            AgentRegistrationResponse.Data data = new AgentRegistrationResponse.Data(
                tokenResult.tokenId(),
                "APPROVED",
                "Registration approved, token pushed successfully"
            );
            return Response.ok()
                .entity(ResEntityHelper.ok("Registration approved", new AgentRegistrationResponse(true, data, null)))
                .build();
        } else {
            LOGGER.error("Failed to push token to callback URL: {}", request.callbackUrl());
            return Response.status(Response.Status.BAD_GATEWAY)
                .entity(ResEntityHelper.badRequest("Failed to push token to callback URL"))
                .build();
        }
    }

    private String generateId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String getClientAddress() {
        // In a real container environment, this would be extracted from the request
        // For now, return null - container-specific extraction can be added if needed
        return null;
    }
}
