package top.ilovemyhome.springsecsso.server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizationServerTest {

    @BeforeAll
    void startServer() {
        context = SpringApplication.run(AuthorizationServerApplication.class,
            "--server.port=0",
            "--lab.user-password=local-test-password-123",
            "--lab.client-a-secret=local-test-client-a-secret",
            "--lab.client-b-secret=local-test-client-b-secret");
        port = context.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
        registrations = context.getBean(RegisteredClientRepository.class);
        encoder = context.getBean(PasswordEncoder.class);
    }

    @AfterAll
    void stopServer() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void publishesOidcDiscoveryAndOnlyPublicSigningKeys() throws Exception {
        var discovery = get("/.well-known/openid-configuration");
        var keys = get("/oauth2/jwks");
        assertAll("public protocol metadata",
            () -> assertEquals(200, discovery.statusCode()),
            () -> assertTrue(discovery.body().contains("http://localhost:9100")),
            () -> assertTrue(discovery.body().contains("end_session_endpoint")),
            () -> assertEquals(200, keys.statusCode()),
            () -> assertFalse(keys.body().contains("\"d\":"), "Private RSA parameters must not be published")
        );
    }

    @Test
    void rejectsLoginWithoutCsrf() throws Exception {
        var page = get("/login");
        var request = HttpRequest.newBuilder(uri("/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("username=researcher&password=local-test-password-123"))
            .build();
        var rejected = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertAll("login security",
            () -> assertEquals(200, page.statusCode()),
            () -> assertTrue(page.body().contains("name=\"_csrf\"")),
            () -> assertEquals(403, rejected.statusCode())
        );
    }

    @Test
    void rejectsUnregisteredRedirectWithoutRedirectingToIt() throws Exception {
        String redirect = URLEncoder.encode("https://attacker.example/callback", StandardCharsets.UTF_8);
        var response = get("/oauth2/authorize?response_type=code&client_id=app-a&scope=openid"
            + "&redirect_uri=" + redirect + "&state=test-state&code_challenge=challenge&code_challenge_method=S256");
        assertAll("redirect allowlist",
            () -> assertEquals(400, response.statusCode()),
            () -> assertTrue(response.headers().firstValue("Location").isEmpty())
        );
    }

    @Test
    void registersIndependentConfidentialClientsWithPkce() {
        var a = registrations.findByClientId("app-a");
        var b = registrations.findByClientId("app-b");
        assertNotNull(a);
        assertNotNull(b);
        assertAll("client registrations",
            () -> assertTrue(a.getClientSettings().isRequireProofKey()),
            () -> assertTrue(b.getClientSettings().isRequireProofKey()),
            () -> assertEquals(java.util.Set.of("http://localhost:9101/login/oauth2/code/lab"), a.getRedirectUris()),
            () -> assertEquals(java.util.Set.of("http://localhost:9102/login/oauth2/code/lab"), b.getRedirectUris()),
            () -> assertTrue(encoder.matches("local-test-client-a-secret", a.getClientSecret())),
            () -> assertFalse(encoder.matches("local-test-client-a-secret", b.getClientSecret()))
        );
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private int port;
    private RegisteredClientRepository registrations;
    private PasswordEncoder encoder;
    private ConfigurableApplicationContext context;
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
}
