package top.ilovemyhome.dagtask.scheduler.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;

import java.io.IOException;

/**
 * JAX-RS request filter that authenticates agent requests using JWT tokens.
 * <p>
 * This filter checks the Authorization header for a Bearer token, validates it,
 * and rejects requests that don't have a valid non-revoked token.
 * Only endpoints that require authentication are protected by this filter.
 * </p>
 */
@Provider
public class AgentTokenAuthFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentTokenAuthFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    @Inject
    public AgentTokenAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            LOGGER.debug("No valid Authorization header found");
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("Missing or invalid Authorization header")
                .build());
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();

        if (!tokenService.validateToken(token)) {
            LOGGER.info("Invalid or revoked token provided");
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("Invalid or revoked token")
                .build());
        }
    }
}
