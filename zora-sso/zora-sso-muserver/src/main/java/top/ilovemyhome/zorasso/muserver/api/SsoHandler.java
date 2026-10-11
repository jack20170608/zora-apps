package top.ilovemyhome.zorasso.muserver.api;


import io.muserver.MuRequest;
import io.muserver.MuResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zorasso.muserver.application.AppContext;
import top.ilovemyhome.zorasso.si.SsoException;
import top.ilovemyhome.zorasso.si.SsoService;

import java.net.URI;

@Path("/sso")
public class SsoHandler {

    public SsoHandler(AppContext appContext) {
        this.ssoService = appContext.ssoService();
    }

    @Path("/authorize")
    @POST
    private Response authorize(@Context MuRequest request) {
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
        return response;
    }


    private static String requiredQuery(MuRequest request, String name) {
        String value = request.query().get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing query parameter");
        }
        return value;
    }

    private static void secureHeaders(MuResponse response) {
        response.headers().set("Cache-Control", "no-store");
        response.headers().set("Pragma", "no-cache");
        response.headers().set("X-Content-Type-Options", "nosniff");
        response.headers().set("X-Frame-Options", "DENY");
        response.headers().set("Content-Security-Policy",
            "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'");
    }


    private final SsoService ssoService;

    private static final Logger logger = LoggerFactory.getLogger(SsoHandler.class);
}
