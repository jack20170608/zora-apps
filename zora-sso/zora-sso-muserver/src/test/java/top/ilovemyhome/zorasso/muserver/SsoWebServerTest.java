package top.ilovemyhome.zorasso.muserver;

import io.muserver.MuServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.zorasso.core.InMemoryLocalSsoService;
import top.ilovemyhome.zorasso.core.LocalIdentityAuthenticator;
import top.ilovemyhome.zorasso.core.LocalUser;
import top.ilovemyhome.zorasso.core.Pbkdf2PasswordHasher;
import top.ilovemyhome.zorasso.core.RegisteredClient;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SsoWebServerTest {

    @BeforeEach
    void setUp() {
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        LocalUser user = new LocalUser(
            "user-1",
            "alice",
            "Alice",
            hasher.hash("user-password".toCharArray()),
            Set.of("USER"),
            true
        );
        RegisteredClient registeredClient = new RegisteredClient(
            "app-a",
            hasher.hash("client-secret".toCharArray()),
            Set.of(CALLBACK),
            true
        );
        SsoConfiguration configuration = new SsoConfiguration(
            "test",
            "127.0.0.1",
            0,
            URI.create("http://localhost"),
            "ZORA_SSO_SESSION",
            false,
            true,
            List.of(user),
            List.of(registeredClient)
        );
        InMemoryLocalSsoService service = new InMemoryLocalSsoService(
            new LocalIdentityAuthenticator(List.of(user), hasher, Clock.systemUTC()),
            hasher,
            List.of(registeredClient)
        );
        server = new SsoWebServer(configuration, service).start();
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
        assertThat(exchange.body()).contains("\"username\":\"alice\"").doesNotContain("client-secret");

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
