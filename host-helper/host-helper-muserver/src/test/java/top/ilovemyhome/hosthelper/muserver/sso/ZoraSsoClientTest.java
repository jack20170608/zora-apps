package top.ilovemyhome.hosthelper.muserver.sso;

import io.muserver.Method;
import io.muserver.MuServer;
import io.muserver.MuServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoraSsoClientTest {

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void buildsAuthorizationUriFromTrustedConfiguration() {
        SsoClientConfiguration configuration = configuration(URI.create("http://localhost:9080"), 65_536);
        ZoraSsoClient client = new ZoraSsoClient(configuration);

        URI authorizationUri = client.authorizationUri("state with spaces");

        assertEquals("/authorize", authorizationUri.getPath());
        assertTrue(authorizationUri.getRawQuery().contains("client_id=host-helper"));
        assertTrue(authorizationUri.getRawQuery().contains("state=state+with+spaces"));
        assertTrue(authorizationUri.getRawQuery().contains("redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fauth%2Fsso%2Fcallback"));
    }

    @Test
    void exchangesCodeAndParsesIdentity() {
        server = MuServerBuilder.httpServer()
            .withHttpPort(0)
            .addHandler(Method.POST, "/token/exchange", (request, response, pathParams) -> {
                response.contentType("application/json");
                response.write("""
                    {"subject":"user-1","username":"alice","displayName":"Alice",
                     "roles":["USER"],"authenticatedAt":"2026-10-11T00:00:00Z"}
                    """);
            })
            .start();
        ZoraSsoClient client = new ZoraSsoClient(configuration(server.uri(), 65_536));

        SsoIdentity identity = client.exchange("one-time-code");

        assertEquals("user-1", identity.subject());
        assertEquals("alice", identity.username());
        assertEquals(Instant.parse("2026-10-11T00:00:00Z"), identity.authenticatedAt());
        assertEquals(java.util.Set.of("USER"), identity.roles());
    }

    @Test
    void rejectsOversizedExchangeResponse() {
        server = MuServerBuilder.httpServer()
            .withHttpPort(0)
            .addHandler(Method.POST, "/token/exchange", (request, response, pathParams) -> {
                response.contentType("application/json");
                response.write("x".repeat(257));
            })
            .start();
        ZoraSsoClient client = new ZoraSsoClient(configuration(server.uri(), 256));

        SsoAuthenticationException exception = assertThrowsExactly(
            SsoAuthenticationException.class,
            () -> client.exchange("one-time-code")
        );

        assertEquals("SSO response exceeded the configured size limit", exception.getMessage());
    }

    @Test
    void rejectsNonLocalHttpConfiguration() {
        IllegalArgumentException exception = assertThrowsExactly(
            IllegalArgumentException.class,
            () -> configuration(URI.create("http://sso.example.com"), 65_536)
        );

        assertTrue(exception.getMessage().contains("HTTPS"));
    }

    private static SsoClientConfiguration configuration(URI baseUri, int maxResponseBytes) {
        return new SsoClientConfiguration(
            baseUri,
            "host-helper",
            "client-secret".toCharArray(),
            URI.create("http://localhost:8000/auth/sso/callback"),
            "/auth/sso/login",
            "/auth/sso/callback",
            Duration.ofSeconds(5),
            Duration.ofMinutes(5),
            100,
            maxResponseBytes,
            false,
            Map.of("USER", List.of("read"))
        );
    }

    private MuServer server;
}
