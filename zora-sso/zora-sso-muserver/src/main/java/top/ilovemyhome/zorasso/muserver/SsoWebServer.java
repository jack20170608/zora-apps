package top.ilovemyhome.zorasso.muserver;

import io.muserver.CookieBuilder;
import io.muserver.Method;
import io.muserver.MuRequest;
import io.muserver.MuResponse;
import io.muserver.MuServer;
import io.muserver.MuServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zorasso.si.AuthenticatedIdentity;
import top.ilovemyhome.zorasso.si.SsoException;
import top.ilovemyhome.zorasso.si.SsoService;

import java.net.URI;
import java.net.URLDecoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class SsoWebServer {

    public SsoWebServer(SsoConfiguration configuration, SsoService ssoService) {
        this.configuration = configuration;
        this.ssoService = ssoService;
    }

    public MuServer start() {
        return MuServerBuilder.httpServer()
            .withHttpPort(configuration.port())
            .withInterface(configuration.host())
            .withIdleTimeout(30, TimeUnit.SECONDS)
            .withMaxRequestSize(16_384)
            .addHandler(Method.GET, "/health", this::health)
            .addHandler(Method.GET, "/authorize", this::authorize)
            .addHandler(Method.GET, "/login", this::loginPage)
            .addHandler(Method.POST, "/login", this::login)
            .addHandler(Method.POST, "/token/exchange", this::exchange)
            .addHandler(Method.POST, "/logout", this::logout)
            .start();
    }

    private void health(MuRequest request, MuResponse response, Map<String, String> pathParams) {
        secureHeaders(response);
        json(response, 200, "{\"status\":\"UP\",\"localAuthentication\":true}");
    }

    private void authorize(MuRequest request, MuResponse response, Map<String, String> pathParams) {
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

    private void loginPage(MuRequest request, MuResponse response, Map<String, String> pathParams) {
        secureHeaders(response);
        try {
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
        } catch (SsoException exception) {
            error(response, exception);
        }
    }

    private void login(MuRequest request, MuResponse response, Map<String, String> pathParams) {
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
            error(response, exception);
        }
    }

    private void exchange(MuRequest request, MuResponse response, Map<String, String> pathParams) {
        secureHeaders(response);
        try {
            Map<String, String> form = parseForm(request);
            char[] clientSecret = form.getOrDefault("client_secret", "").toCharArray();
            AuthenticatedIdentity identity = ssoService.exchange(
                form.get("code"),
                form.get("client_id"),
                clientSecret,
                URI.create(form.getOrDefault("redirect_uri", ""))
            );
            String roles = identity.roles().stream()
                .sorted()
                .map(role -> "\"" + jsonEscape(role) + "\"")
                .collect(java.util.stream.Collectors.joining(","));
            json(response, 200, "{\"subject\":\"" + jsonEscape(identity.subject())
                + "\",\"username\":\"" + jsonEscape(identity.username())
                + "\",\"displayName\":\"" + jsonEscape(identity.displayName())
                + "\",\"roles\":[" + roles + "],\"authenticatedAt\":\""
                + identity.authenticatedAt() + "\"}");
        } catch (IllegalArgumentException exception) {
            error(response, new SsoException(SsoException.Code.INVALID_REQUEST, "Invalid exchange request"));
        } catch (SsoException exception) {
            error(response, exception);
        }
    }

    private void logout(MuRequest request, MuResponse response, Map<String, String> pathParams) {
        secureHeaders(response);
        ssoService.logout(request.cookie(configuration.cookieName()).orElse(null));
        response.addCookie(sessionCookie("", 0));
        json(response, 200, "{\"loggedOut\":true}");
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

    private static void error(MuResponse response, SsoException exception) {
        int status = switch (exception.code()) {
            case LOGIN_REJECTED, INVALID_CLIENT_CREDENTIALS, INVALID_SESSION, INVALID_CODE -> 401;
            case RATE_LIMITED -> 429;
            default -> 400;
        };
        LOGGER.warn("SSO request rejected with code {}", exception.code());
        json(response, status, "{\"error\":\"" + exception.code().name().toLowerCase(java.util.Locale.ROOT) + "\"}");
    }

    private static void json(MuResponse response, int status, String body) {
        response.status(status);
        response.contentType("application/json; charset=utf-8");
        response.write(body);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SsoWebServer.class);

    private final SsoConfiguration configuration;
    private final SsoService ssoService;
}
