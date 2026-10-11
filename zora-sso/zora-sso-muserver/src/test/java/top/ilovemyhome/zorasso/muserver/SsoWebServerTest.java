package top.ilovemyhome.zorasso.muserver;

import com.typesafe.config.ConfigFactory;
import io.muserver.MuServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.zorasso.core.Pbkdf2PasswordHasher;
import top.ilovemyhome.zorasso.muserver.application.AppContext;
import top.ilovemyhome.zorasso.muserver.application.SsoWebServer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SsoWebServerTest {

    @BeforeEach
    void setUp() {
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        String config = """
            zora-sso {
              environment = test
              local-auth { enabled = true, production-allowed = false }
              server { host = "127.0.0.1", port = 0, public-base-url = "http://localhost" }
              cookie { name = "ZORA_SSO_SESSION", secure = false }
              users = [
                {
                  subject = "user-1"
                  username = "alice"
                  display-name = "Alice"
                  password-hash = "%s"
                  roles = ["USER", "ADMIN"]
                  enabled = true
                },
                {
                  subject = "user-2"
                  username = "bob"
                  display-name = "Bob"
                  password-hash = "%s"
                  roles = ["USER"]
                  enabled = true
                }
              ]
              clients = [{
                client-id = "app-a"
                client-secret-hash = "%s"
                redirect-uris = ["%s"]
                enabled = true
              }]
            }
            """.formatted(
            hasher.hash("user-password".toCharArray()),
            hasher.hash("bob-password".toCharArray()),
            hasher.hash("client-secret".toCharArray()),
            CALLBACK
        );
        AppContext appContext = new AppContext("test", ConfigFactory.parseString(config));
        appContext.init();
        server = SsoWebServer.start(appContext);
        client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void completesAuthorizationLoginAndExchangeFlow() throws Exception {
        HttpResponse<String> authorize = get("/authorize?client_id=app-a&redirect_uri="
            + encode(CALLBACK.toString()) + "&state=client-state", null);
        assertThat(authorize.statusCode()).isEqualTo(302);
        assertThat(authorize.headers().firstValue("location")).hasValueSatisfying(
            location -> assertThat(URI.create(location).getPath()).isEqualTo("/login")
        );

        HttpResponse<String> loginPage = get(authorize.headers().firstValue("location").orElseThrow(), null);
        String transactionId = hidden(loginPage.body(), "transaction_id");
        String csrfToken = hidden(loginPage.body(), "csrf_token");
        assertThat(loginPage.headers().firstValue("content-security-policy")).isPresent();

        String loginBody = form(MapBuilder.of(
            "transaction_id", transactionId,
            "csrf_token", csrfToken,
            "username", "alice",
            "password", "user-password"
        ));
        HttpResponse<String> login = post("/login", loginBody, null);
        assertThat(login.statusCode()).isEqualTo(302);
        String cookie = login.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
        assertThat(login.headers().firstValue("set-cookie").orElseThrow())
            .containsIgnoringCase("HttpOnly")
            .contains("SameSite=Lax");
        URI callback = URI.create(login.headers().firstValue("location").orElseThrow());
        String code = query(callback, "code");
        assertThat(query(callback, "state")).isEqualTo("client-state");

        HttpResponse<String> exchange = post("/token/exchange", form(MapBuilder.of(
            "code", code,
            "client_id", "app-a",
            "client_secret", "client-secret",
            "redirect_uri", CALLBACK.toString()
        )), cookie);
        assertThat(exchange.statusCode()).isEqualTo(200);
        assertThat(exchange.body())
            .containsPattern("\"success\"\\s*:\\s*true")
            .containsPattern("\"username\"\\s*:\\s*\"alice\"")
            .doesNotContain("client-secret");

        HttpResponse<String> clients = get("/clients", cookie);
        assertThat(clients.statusCode()).isEqualTo(200);
        assertThat(clients.body())
            .containsPattern("\"clientId\"\\s*:\\s*\"app-a\"")
            .contains(CALLBACK.toString())
            .doesNotContain("clientSecret", "client-secret");

        HttpResponse<String> secondAuthorize = get("/authorize?client_id=app-a&redirect_uri="
            + encode(CALLBACK.toString()) + "&state=second", cookie);
        assertThat(secondAuthorize.statusCode()).isEqualTo(302);
        assertThat(secondAuthorize.headers().firstValue("location").orElseThrow())
            .startsWith(CALLBACK.toString())
            .contains("state=second");
    }

    @Test
    void rejectsOpenRedirectWithoutLocationHeader() throws Exception {
        HttpResponse<String> response = get("/authorize?client_id=app-a&redirect_uri="
            + encode("https://attacker.example/callback") + "&state=test", null);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("location")).isEmpty();
        assertThat(response.body()).contains("invalid_redirect_uri");
    }

    @Test
    void servesHealthAndLogoutAsRestEndpoints() throws Exception {
        HttpResponse<String> health = get("/health", null);
        HttpResponse<String> logout = post("/logout", "", "ZORA_SSO_SESSION=unknown");

        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.headers().firstValue("content-type")).hasValueSatisfying(
            contentType -> assertThat(contentType).startsWith("application/json")
        );
        assertThat(health.headers().firstValue("cache-control")).hasValue("no-store");
        assertThat(health.body())
            .containsPattern("\"success\"\\s*:\\s*true")
            .containsPattern("\"status\"\\s*:\\s*\"UP\"");

        assertThat(logout.statusCode()).isEqualTo(200);
        assertThat(logout.headers().firstValue("set-cookie")).hasValueSatisfying(
            cookie -> assertThat(cookie)
                .contains("ZORA_SSO_SESSION=")
                .contains("Max-Age=0")
                .containsIgnoringCase("HttpOnly")
        );
        assertThat(logout.body())
            .containsPattern("\"success\"\\s*:\\s*true")
            .containsPattern("\"loggedOut\"\\s*:\\s*true");
    }

    @Test
    void rejectsClientListWithoutAuthenticatedSession() throws Exception {
        HttpResponse<String> response = get("/clients", null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body())
            .containsPattern("\"success\"\\s*:\\s*false")
            .contains("A valid SSO session is required")
            .doesNotContain("clientSecret", "client-secret", CALLBACK.toString());
    }

    @Test
    void rejectsClientListWithoutAdministratorRole() throws Exception {
        HttpResponse<String> authorize = get("/authorize?client_id=app-a&redirect_uri="
            + encode(CALLBACK.toString()) + "&state=client-state", null);
        HttpResponse<String> loginPage = get(authorize.headers().firstValue("location").orElseThrow(), null);
        HttpResponse<String> login = post("/login", form(MapBuilder.of(
            "transaction_id", hidden(loginPage.body(), "transaction_id"),
            "csrf_token", hidden(loginPage.body(), "csrf_token"),
            "username", "bob",
            "password", "bob-password"
        )), null);
        String cookie = login.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];

        HttpResponse<String> response = get("/clients", cookie);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body())
            .containsPattern("\"success\"\\s*:\\s*false")
            .contains("Administrator role is required")
            .doesNotContain("clientSecret", "client-secret", CALLBACK.toString());
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(server.uri().resolve(path)).GET();
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body, String cookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(server.uri().resolve(path))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String hidden(String html, String name) {
        var matcher = Pattern.compile("name=\"" + name + "\" value=\"([^\"]+)\"").matcher(html);
        if (!matcher.find()) {
            throw new AssertionError("Missing hidden field " + name);
        }
        return matcher.group(1);
    }

    private static String query(URI uri, String name) {
        for (String pair : uri.getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts[0].equals(name)) {
                return java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("Missing query parameter " + name);
    }

    private static String form(java.util.Map<String, String> values) {
        return values.entrySet().stream()
            .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
            .collect(java.util.stream.Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static final class MapBuilder {
        private static java.util.Map<String, String> of(String... values) {
            java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
            for (int index = 0; index < values.length; index += 2) {
                result.put(values[index], values[index + 1]);
            }
            return result;
        }
    }

    private static final URI CALLBACK = URI.create("https://app.example/callback");

    private MuServer server;
    private HttpClient client;
}
