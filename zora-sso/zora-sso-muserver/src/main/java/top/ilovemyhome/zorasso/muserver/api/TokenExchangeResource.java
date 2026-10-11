package top.ilovemyhome.zorasso.muserver.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import top.ilovemyhome.zorasso.muserver.application.AppContext;
import top.ilovemyhome.zorasso.si.AuthenticatedIdentity;
import top.ilovemyhome.zorasso.si.SsoException;
import top.ilovemyhome.zorasso.si.SsoService;

import java.net.URI;
import java.util.Optional;

@Path("/token/exchange")
public final class TokenExchangeResource {

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(
        @FormParam("code") String code,
        @FormParam("client_id") String clientId,
        @FormParam("client_secret") String clientSecret,
        @FormParam("redirect_uri") String redirectUri
    ) {
        return exchange(code, clientId, clientSecret, redirectUri);
    }


    private Response exchange(String code, String clientId, String clientSecret, String redirectUri) {
        try {
            AuthenticatedIdentity identity = ssoService.exchange(
                code,
                clientId,
                Optional.ofNullable(clientSecret).orElse("").toCharArray(),
                URI.create(Optional.ofNullable(redirectUri).orElse(""))
            );
            String roles = identity.roles().stream()
                .sorted()
                .map(role -> "\"" + jsonEscape(role) + "\"")
                .collect(java.util.stream.Collectors.joining(","));
            return restResponse(200, "{\"subject\":\"" + jsonEscape(identity.subject())
                + "\",\"username\":\"" + jsonEscape(identity.username())
                + "\",\"displayName\":\"" + jsonEscape(identity.displayName())
                + "\",\"roles\":[" + roles + "],\"authenticatedAt\":\""
                + identity.authenticatedAt() + "\"}");
        } catch (IllegalArgumentException exception) {
            return restError(new SsoException(SsoException.Code.INVALID_REQUEST, "Invalid exchange request"));
        } catch (SsoException exception) {
            return restError(exception);
        }
    }

    public TokenExchangeResource(AppContext appContext) {
        this.ssoService = appContext.ssoService();
    }

    private final SsoService ssoService;
}
