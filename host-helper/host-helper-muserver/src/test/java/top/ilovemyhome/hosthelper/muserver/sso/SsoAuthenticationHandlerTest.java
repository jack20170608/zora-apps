package top.ilovemyhome.hosthelper.muserver.sso;

import io.muserver.Method;
import io.muserver.MuServer;
import io.muserver.MuServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.zora.muserver.security.authenticator.JwtAuthenticator;
import top.ilovemyhome.zora.muserver.security.core.User;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SsoAuthenticationHandlerTest {

    @BeforeEach
    void setUp() {
        SsoClientConfiguration configuration = new SsoClientConfiguration(
            URI.create("http://localhost:9080"),
            "host-helper",
            "client-secret".toCharArray(),
            URI.create("http://localhost:8000/auth/sso/callback"),
            "/auth/sso/login",
            "/auth/sso/callback",
            Duration.ofSeconds(5),
            Duration.ofMinutes(5),
            100,
            65_536,
            false,
            Map.of("USER", List.of("read"))
        );
        ZoraSsoClient ssoClient = mock(ZoraSsoClient.class);
        when(ssoClient.authorizationUri(any())).thenAnswer(invocation ->
            URI.create("http://localhost:9080/authorize?state=" + invocation.getArgument(0, String.class)));
        when(ssoClient.exchange("valid-code")).thenReturn(new SsoIdentity(
            "user-1",
            "alice",
            "Alice",
            Set.of("USER"),
            Instant.parse("2026-10-11T00:00:00Z")
        ));
        JwtAuthenticator jwtAuthenticator = mock(JwtAuthenticator.class);
        when(jwtAuthenticator.generateJwt(any(User.class))).thenReturn("signed-jwt");
        SsoAuthenticationHandler handler = new SsoAuthenticationHandler(
            configuration,
            ssoClient,
            jwtAuthenticator,
            "hosthelperjwt",
            3600,
            "/hosthelper/index.html",
            Clock.fixed(Instant.parse("2026-10-11T00:01:00Z"), ZoneOffset.UTC),
            new SecureRandom()
        );
        server = MuServerBuilder.httpServer()
            .withHttpPort(0)
            .addHandler(Method.GET, "/auth/sso/login", handler::begin)
            .addHandler(Method.GET, "/auth/sso/callback", handler::callback)
            .start();
        httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void consumesStateOnceAndCreatesApplicationSession() throws Exception {
        HttpResponse<String> begin = get("/auth/sso/login");
        String state = query(URI.create(begin.headers().firstValue("location").orElseThrow()), "state");

        HttpResponse<String> callback = get("/auth/sso/callback?state=" + state + "&code=valid-code");
        HttpResponse<String> replay = get("/auth/sso/callback?state=" + state + "&code=valid-code");

        assertEquals(302, callback.statusCode());
        assertEquals(
            "/hosthelper/index.html",
            URI.create(callback.headers().firstValue("location").orElseThrow()).getPath()
        );
        String cookie = callback.headers().firstValue("set-cookie").orElseThrow();
        String normalizedCookie = cookie.toLowerCase(Locale.ROOT);
        assertTrue(cookie.contains("hosthelperjwt=signed-jwt"));
        assertTrue(normalizedCookie.contains("httponly"));
        assertTrue(normalizedCookie.contains("samesite=lax"));
        assertEquals(401, replay.statusCode());
    }

    @Test
    void rejectsCallbackWithoutMatchingState() throws Exception {
        HttpResponse<String> response = get("/auth/sso/callback?state=unknown&code=valid-code");

        assertEquals(401, response.statusCode());
        assertEquals("no-store", response.headers().firstValue("cache-control").orElseThrow());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return httpClient.send(
            HttpRequest.newBuilder(server.uri().resolve(path)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private static String query(URI uri, String name) {
        for (String pair : uri.getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts[0].equals(name)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("Missing query parameter " + name);
    }

    private MuServer server;
    private HttpClient httpClient;
}
