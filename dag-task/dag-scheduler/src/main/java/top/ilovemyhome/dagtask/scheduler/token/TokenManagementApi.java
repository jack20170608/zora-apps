package top.ilovemyhome.dagtask.scheduler.token;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.Constants;
import top.ilovemyhome.dagtask.si.auth.GenerateTokenRequest;
import top.ilovemyhome.dagtask.si.auth.GenerateTokenResponse;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;

import java.util.List;

/**
 * Admin REST API endpoints for managing agent tokens.
 * <p>
 * Provides functionality for:
 * <ul>
 *     <li>Listing all existing tokens with their status</li>
 *     <li>Manually generating new tokens</li>
 *     <li>Revoking existing tokens</li>
 * </ul>
 * </p>
 */
@Path(Constants.API_SCHEDULER)
@Produces(MediaType.APPLICATION_JSON)
public class TokenManagementApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenManagementApi.class);

    private final TokenService tokenService;

    @Inject
    public TokenManagementApi(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * List all existing tokens.
     *
     * @return list of all tokens with their metadata
     */
    @GET
    @Path("/admin/tokens")
    public Response listTokens() {
        List<TokenInfo> tokens = tokenService.listTokens();
        return Response.ok()
            .entity(ResEntityHelper.ok("Tokens retrieved successfully", tokens))
            .build();
    }

    /**
     * Manually generate a new token.
     *
     * @param request token generation request with name, description, expiration
     * @param createdBy username of the admin creating this token
     * @return response with the generated JWT token
     */
    @POST
    @Path("/admin/tokens/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateToken(GenerateTokenRequest request,
                                  @QueryParam("createdBy") String createdBy) {
        LOGGER.info("Manual token generation requested: name={}, expiresInDays={}, createdBy={}",
            request.name(), request.expiresInDays(), createdBy);

        var result = tokenService.generateToken(
            request.name(),
            request.description(),
            request.expiresInDays(),
            createdBy
        );
        String jwt = tokenService.generateJwt(result);

        GenerateTokenResponse.Data data = new GenerateTokenResponse.Data(
            jwt,
            result.tokenId(),
            result.expiresAt(),
            result.name()
        );
        GenerateTokenResponse response = new GenerateTokenResponse(true, data, null);

        return Response.ok()
            .entity(ResEntityHelper.ok("Token generated successfully", response))
            .build();
    }

    /**
     * Revoke an existing token.
     * <p>
     * Once revoked, the token can no longer be used for authentication.
     *
     * @param tokenId the token ID to revoke
     * @param revokedBy username of the admin revoking this token
     * @return success response
     */
    @POST
    @Path("/admin/tokens/{tokenId}/revoke")
    public Response revokeToken(@PathParam("tokenId") String tokenId,
                                @QueryParam("revokedBy") String revokedBy) {
        LOGGER.info("Token revocation requested: tokenId={}, revokedBy={}", tokenId, revokedBy);
        tokenService.revokeToken(tokenId, revokedBy);
        return Response.ok()
            .entity(ResEntityHelper.ok("Token revoked successfully", null))
            .build();
    }
}
