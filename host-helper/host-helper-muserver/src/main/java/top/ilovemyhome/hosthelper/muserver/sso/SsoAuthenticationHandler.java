package top.ilovemyhome.hosthelper.muserver.sso;

import io.muserver.CookieBuilder;
import io.muserver.MuRequest;
import io.muserver.MuResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.hosthelper.muserver.application.AppContext;
import top.ilovemyhome.zora.muserver.security.AppSecurityContext;
import top.ilovemyhome.zora.muserver.security.authenticator.JwtAuthenticator;
import top.ilovemyhome.zora.muserver.security.core.User;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class SsoAuthenticationHandler {

    public SsoAuthenticationHandler(AppContext appContext) {
        this(appContext, SsoClientConfiguration.from(appContext.getConfig()));
    }

    private SsoAuthenticationHandler(AppContext appContext, SsoClientConfiguration configuration) {
        this(
            configuration,
            new ZoraSsoClient(configuration),
            appContext.getBean("appSecurityContext", AppSecurityContext.class).getJwtAuthenticator(),
            appContext.getConfig().getString("cookie.name"),
            appContext.getConfig().getDuration("jwt.ttl", TimeUnit.SECONDS),
            "/" + appContext.getConfig().getString("server.contextPath") + "/index.html",
            Clock.systemUTC(),
            new SecureRandom()
        );
    }

    SsoAuthenticationHandler(
        SsoClientConfiguration configuration,
        ZoraSsoClient ssoClient,
        JwtAuthenticator jwtAuthenticator,
        String cookieName,
        long cookieMaxAgeSeconds,
        String successPath,
        Clock clock,
        SecureRandom secureRandom
    ) {
        this.configuration = configuration;
        this.ssoClient = ssoClient;
        this.jwtAuthenticator = jwtAuthenticator;
        this.cookieName = cookieName;
        this.cookieMaxAgeSeconds = cookieMaxAgeSeconds;
        this.successPath = successPath;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    public void begin(MuRequest request, MuResponse response, Map<String, String> pathParams) {
        secureHeaders(response);
        String state;
        synchronized (pendingStates) {
            removeExpiredStates();
            if (pendingStates.size() >= configuration.maxPendingStates()) {
                reject(response, 503, "SSO login is temporarily unavailable");
                return;
            }
            state = newState();
            pendingStates.put(state, clock.instant().plus(configuration.stateTtl()));
        }
        response.redirect(ssoClient.authorizationUri(state).toString());
    }

    public void callback(MuRequest request, MuResponse response, Map<String, String> pathParams) {
        secureHeaders(response);
        String state = request.query().get("state");
        String code = request.query().get("code");
        Instant expiresAt = state == null ? null : pendingStates.remove(state);
        if (expiresAt == null || !expiresAt.isAfter(clock.instant()) || code == null || code.isBlank()) {
            reject(response, 401, "SSO callback was rejected");
            return;
        }
        try {
            SsoIdentity identity = ssoClient.exchange(code);
            List<String> roles = identity.roles().stream()
                .flatMap(role -> configuration.roleMappings().getOrDefault(role, List.of()).stream())
                .distinct()
                .toList();
            if (roles.isEmpty()) {
                reject(response, 403, "The authenticated user is not authorized for host-helper");
                return;
            }
            User user = new User(
                identity.subject(),
                identity.username(),
                identity.displayName(),
                roles,
                "",
                Map.of(
                    "ssoSubject", identity.subject(),
                    "authenticatedAt", identity.authenticatedAt().toString()
                )
            );
            String jwt = jwtAuthenticator.generateJwt(user);
            response.addCookie(CookieBuilder.newCookie()
                .withName(cookieName)
                .withValue(jwt)
                .withMaxAgeInSeconds(cookieMaxAgeSeconds)
                .withSameSite("Lax")
                .withPath("/")
                .secure(configuration.secureCookie())
                .httpOnly(true)
                .build());
            response.redirect(successPath);
        } catch (SsoAuthenticationException exception) {
            LOGGER.warn("SSO authentication failed: {}", exception.getMessage());
            reject(response, 401, "SSO authentication failed");
        } catch (RuntimeException exception) {
            LOGGER.error("Unable to create the host-helper session after SSO authentication", exception);
            reject(response, 500, "Unable to create an authenticated session");
        }
    }

    private void removeExpiredStates() {
        Instant now = clock.instant();
        pendingStates.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }

    private String newState() {
        byte[] bytes = new byte[32];
        String state;
        do {
            secureRandom.nextBytes(bytes);
            state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (pendingStates.containsKey(state));
        return state;
    }

    private static void secureHeaders(MuResponse response) {
        response.headers().set("Cache-Control", "no-store");
        response.headers().set("Pragma", "no-cache");
        response.headers().set("X-Content-Type-Options", "nosniff");
        response.headers().set("X-Frame-Options", "DENY");
        response.headers().set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
    }

    private static void reject(MuResponse response, int status, String message) {
        response.status(status);
        response.contentType("text/plain; charset=utf-8");
        response.write(message);
    }

    private final SsoClientConfiguration configuration;
    private final ZoraSsoClient ssoClient;
    private final JwtAuthenticator jwtAuthenticator;
    private final String cookieName;
    private final long cookieMaxAgeSeconds;
    private final String successPath;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final Map<String, Instant> pendingStates = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(SsoAuthenticationHandler.class);
}
