package top.ilovemyhome.zorasso.muserver.api;

import io.muserver.CookieBuilder;
import io.muserver.MuRequest;
import io.muserver.MuResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zorasso.muserver.application.AppContext;
import top.ilovemyhome.zorasso.muserver.api.dto.RegisteredClientResponse;
import top.ilovemyhome.zorasso.muserver.api.dto.RestResponse;
import top.ilovemyhome.zorasso.muserver.config.SsoConfiguration;
import top.ilovemyhome.zorasso.si.AuthenticatedIdentity;
import top.ilovemyhome.zorasso.si.SsoException;
import top.ilovemyhome.zorasso.si.SsoService;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/sso")
public final class SsoHandler {

    public SsoHandler(AppContext appContext) {
        this.configuration = appContext.configuration();
        this.ssoService = appContext.ssoService();
    }

    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpHeaders headers) {
        Cookie sessionCookie = headers.getCookies().get(configuration.cookieName());
        ssoService.logout(sessionCookie == null ? null : sessionCookie.getValue());
        return restResponseBuilder(200, RestResponse.success(Map.of("loggedOut", true)))
            .header("Set-Cookie", expiredSessionCookie())
            .build();
    }

    @POST
    @Path("/token/exchange")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exchange(
        @FormParam("code") String code,
        @FormParam("client_id") String clientId,
        @FormParam("client_secret") String clientSecret,
        @FormParam("redirect_uri") String redirectUri
    ) {
        var identity = ssoService.exchange(
            code,
            clientId,
            Optional.ofNullable(clientSecret).orElse("").toCharArray(),
            URI.create(Optional.ofNullable(redirectUri).orElse(""))
        );
        return restResponseBuilder(200, RestResponse.success(identity)).build();
    }

    @GET
    @Path("/clients")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clients(@Context HttpHeaders headers) {
        Cookie sessionCookie = headers.getCookies().get(configuration.cookieName());
        AuthenticatedIdentity identity = ssoService.requireIdentity(
            sessionCookie == null ? null : sessionCookie.getValue()
        );
        if (!identity.roles().contains(ADMIN_ROLE)) {
            throw new SsoException(SsoException.Code.ACCESS_DENIED, "Administrator role is required");
        }
        List<RegisteredClientResponse> clients = configuration.clients().stream()
            .map(RegisteredClientResponse::from)
            .sorted(Comparator.comparing(RegisteredClientResponse::clientId))
            .toList();
        return restResponseBuilder(200, RestResponse.success(clients)).build();
    }

    public void authorize(MuRequest request, MuResponse response, Map<String, String> pathParams) {
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
        } catch (SsoException exception) {
            reject(response, exception);
        } catch (IllegalArgumentException exception) {
            reject(response, new SsoException(SsoException.Code.INVALID_REQUEST, "Invalid authorization request"));
        }
    }

    public void loginPage(MuRequest request, MuResponse response, Map<String, String> pathParams) {
        secureHeaders(response);
        SsoService.LoginPage page = ssoService.prepareLogin(requiredQuery(request, "transaction_id"));
        response.status(200);
        response.contentType("text/html; charset=utf-8");
        response.write("""
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <title>Zora SSO Login</title>
            </head>
            <body>
              <main>
                <h1>Sign in to Zora</h1>
                <p>Client: %s</p>
                <form method="post" action="/login">
                  <input type="hidden" name="transaction_id" value="%s">
                  <input type="hidden" name="csrf_token" value="%s">
                  <label>Username <input name="username" autocomplete="username" required maxlength="128"></label>
                  <label>Password <input type="password" name="password" autocomplete="current-password" required maxlength="1024"></label>
                  <button type="submit">Sign in</button>
                </form>
              </main>
            </body>
            </html>
            """.formatted(escape(page.clientId()), escape(page.transactionId()), escape(page.csrfToken())));
    }

    public void login(MuRequest request, MuResponse response, Map<String, String> pathParams) {
        secureHeaders(response);
        try {
            Map<String, String> form = parseForm(request);
            char[] password = form.getOrDefault("password", "").toCharArray();
            SsoService.LoginResult result = ssoService.login(
                form.get("transaction_id"),
                form.get("csrf_token"),
                form.get("username"),
                password,
                request.clientIP()
            );
            response.addCookie(sessionCookie(result.sessionId(), 7_200));
            response.redirect(result.redirectUri().toString());
        } catch (SsoException exception) {
            reject(response, exception);
        } catch (IllegalArgumentException exception) {
            reject(response, new SsoException(SsoException.Code.INVALID_REQUEST, "Invalid login request"));
        }
    }


    private io.muserver.Cookie sessionCookie(String value, long maxAgeSeconds) {
        return CookieBuilder.newCookie()
            .withName(configuration.cookieName())
            .withValue(value)
            .withPath("/")
            .withMaxAgeInSeconds(maxAgeSeconds)
            .withSameSite("Lax")
            .secure(configuration.secureCookie())
            .httpOnly(true)
            .build();
    }

    private String expiredSessionCookie() {
        return configuration.cookieName() + "=; Path=/; Max-Age=0; SameSite=Lax; HttpOnly"
            + (configuration.secureCookie() ? "; Secure" : "");
    }

    private static Map<String, String> parseForm(MuRequest request) {
        String contentType = Optional.ofNullable(request.headers().get("content-type")).orElse("");
        if (!contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/x-www-form-urlencoded")) {
            throw new SsoException(SsoException.Code.INVALID_REQUEST, "Form content type is required");
        }
        Map<String, String> values = new HashMap<>();
        String body;
        try {
            body = request.readBodyAsString();
        } catch (IOException exception) {
            throw new SsoException(SsoException.Code.INVALID_REQUEST, "Unable to read request body");
        }
        if (body.length() > 8_192) {
            throw new SsoException(SsoException.Code.INVALID_REQUEST, "Request is too large");
        }
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length == 2 ? decode(parts[1]) : "";
            if (values.putIfAbsent(key, value) != null) {
                throw new SsoException(SsoException.Code.INVALID_REQUEST, "Duplicate form parameter");
            }
        }
        return values;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
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

    private static void reject(MuResponse response, SsoException exception) {
        int status = switch (exception.code()) {
            case LOGIN_REJECTED, INVALID_SESSION -> 401;
            case ACCESS_DENIED -> 403;
            case RATE_LIMITED -> 429;
            default -> 400;
        };
        LOGGER.warn("SSO request rejected with code {}", exception.code());
        response.status(status);
        response.contentType("text/plain; charset=utf-8");
        response.write(exception.code().name().toLowerCase(java.util.Locale.ROOT));
    }


    private static Response.ResponseBuilder restResponseBuilder(int status, RestResponse<?> body) {
        return Response.status(status)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .header("Cache-Control", "no-store")
            .header("Pragma", "no-cache")
            .header("X-Content-Type-Options", "nosniff")
            .header("X-Frame-Options", "DENY")
            .header("Content-Security-Policy",
                "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'")
            .entity(body);
    }


    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private final SsoConfiguration configuration;
    private final SsoService ssoService;

    private static final String ADMIN_ROLE = "ADMIN";
    private static final Logger LOGGER = LoggerFactory.getLogger(SsoHandler.class);
}
