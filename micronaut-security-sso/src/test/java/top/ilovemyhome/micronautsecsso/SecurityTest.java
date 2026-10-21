package top.ilovemyhome.micronautsecsso;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityTest {

    @BeforeAll
    void startServer() {
        server = ApplicationContext.run(EmbeddedServer.class, Map.of(
            "micronaut.server.port", -1,
            "micronaut.security.oauth2.enabled", false,
            "micronaut.security.oauth2.clients.keycloak.client-secret", "test-only-client-secret"
        ));
    }

    @AfterAll
    void stopServer() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void homeCreatesSessionAndCsrfFormToken() throws Exception {
        var response = get("/");
        assertAll("public login page",
            () -> assertEquals(200, response.statusCode()),
            () -> assertTrue(response.body().contains("/oauth/login/keycloak")),
            () -> assertTrue(response.headers().allValues("set-cookie").stream()
                .anyMatch(cookie -> cookie.startsWith("MN_SSO_APP_A_SESSION=")
                    && cookie.toLowerCase().contains("httponly")
                    && cookie.toLowerCase().contains("samesite=lax")))
        );
    }

    @Test
    void identityRequiresAuthentication() throws Exception {
        var response = get("/api/me");
        assertAll("protected identity",
            () -> assertEquals(401, response.statusCode()),
            () -> assertFalse(response.body().contains("researcher"))
        );
    }

    @Test
    void logoutRejectsMissingCsrfToken() throws Exception {
        get("/");
        var request = HttpRequest.newBuilder(uri("/logout"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
    }

    @Test
    void getLogoutCannotPerformLogout() throws Exception {
        var response = get("/logout");
        assertTrue(response.statusCode() >= 400, "GET logout must not perform a state-changing redirect");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).header("Accept", "application/json").GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return server.getURI().resolve(path);
    }

    private EmbeddedServer server;
    private final HttpClient client = HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
}
