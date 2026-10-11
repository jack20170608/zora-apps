package top.ilovemyhome.zorasso.muserver.api;


import io.muserver.MuRequest;
import io.muserver.MuResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zorasso.si.SsoException;
import top.ilovemyhome.zorasso.si.SsoService;

import java.net.URI;
import java.util.Map;

@Path("/sso")
public class SsoHandler {

    @Path("/authorize")
    @POST
    private void authorize(@Context MuRequest request, MuResponse response) {
        secureHeaders(response);
        try {
            SsoService.AuthorizationStart result = ssoService.beginAuthorization(
                new SsoService.AuthorizationRequest(
                    requiredQuery(request, "client_id"),
                    URI.create(requiredQuery(request, "redirect_uri")),
                    requiredQuery(request, "state")
                ),
                request.cookie(configuration.cookieName()).orElse(null)
            );
            if (result.loginRequired()) {
                response.redirect("/login?transaction_id=" + encode(result.transactionId()));
            } else {
                response.redirect(result.redirectUri().toString());
            }
        } catch (IllegalArgumentException exception) {
            error(response, new SsoException(SsoException.Code.INVALID_REQUEST, "Invalid authorization request"));
        } catch (SsoException exception) {
            error(response, exception);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(SsoHandler.class);
}
