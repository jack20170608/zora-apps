package top.ilovemyhome.zorasso.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.zorasso.si.AuthenticatedIdentity;
import top.ilovemyhome.zorasso.si.SsoException;
import top.ilovemyhome.zorasso.si.SsoService;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryLocalSsoServiceTest {

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-09-19T08:00:00Z"));
        hasher = new Pbkdf2PasswordHasher();
        LocalUser user = new LocalUser(
            "user-1",
            "alice",
            "Alice",
            hasher.hash(USER_PASSWORD.toCharArray()),
            Set.of("USER"),
            true
        );
        RegisteredClient client = new RegisteredClient(
            "app-a",
            hasher.hash(CLIENT_SECRET.toCharArray()),
            Set.of(REDIRECT_URI),
            true
        );
        service = new InMemoryLocalSsoService(
            new LocalIdentityAuthenticator(List.of(user), hasher, clock),
            hasher,
            List.of(client),
            clock,
            new java.security.SecureRandom(),
            Duration.ofMinutes(5),
            Duration.ofHours(2),
            Duration.ofHours(8),
            Duration.ofSeconds(30)
        );
    }

    @Test
    void completesLoginAndConsumesCodeOnlyOnce() {
        LoginFlow flow = login();

        AuthenticatedIdentity identity = service.exchange(
            flow.code(),
            "app-a",
            CLIENT_SECRET.toCharArray(),
            REDIRECT_URI
        );

        assertThat(identity.username()).isEqualTo("alice");
        assertThat(identity.roles()).containsExactly("USER");
        assertThatThrownBy(() -> service.exchange(
            flow.code(),
            "app-a",
            CLIENT_SECRET.toCharArray(),
            REDIRECT_URI
        )).isInstanceOf(SsoException.class)
            .extracting(exception -> ((SsoException) exception).code())
            .isEqualTo(SsoException.Code.INVALID_CODE);
    }

    @Test
    void reusesCentralSessionWithoutAnotherPasswordPrompt() {
        LoginFlow flow = login();

        SsoService.AuthorizationStart second = service.beginAuthorization(
            new SsoService.AuthorizationRequest("app-a", REDIRECT_URI, "second-state"),
            flow.sessionId()
        );

        assertThat(second.loginRequired()).isFalse();
        assertThat(second.redirectUri().toString()).contains("code=").contains("state=second-state");
    }

    @Test
    void rejectsUnregisteredRedirectWithoutRedirecting() {
        assertThatThrownBy(() -> service.beginAuthorization(
            new SsoService.AuthorizationRequest(
                "app-a",
                URI.create("https://attacker.example/callback"),
                "state"
            ),
            null
        )).isInstanceOf(SsoException.class)
            .extracting(exception -> ((SsoException) exception).code())
            .isEqualTo(SsoException.Code.INVALID_REDIRECT_URI);
    }

    @Test
    void wrongClientBindingDoesNotDestroyValidCode() {
        LoginFlow flow = login();

        assertThatThrownBy(() -> service.exchange(
            flow.code(),
            "app-a",
            CLIENT_SECRET.toCharArray(),
            URI.create("https://app.example/other")
        )).isInstanceOf(SsoException.class);

        assertThat(service.exchange(
            flow.code(),
            "app-a",
            CLIENT_SECRET.toCharArray(),
            REDIRECT_URI
        ).username()).isEqualTo("alice");
    }

    @Test
    void concurrentCodeExchangeHasExactlyOneWinner() throws Exception {
        LoginFlow flow = login();
        Callable<Boolean> exchange = () -> {
            try {
                service.exchange(flow.code(), "app-a", CLIENT_SECRET.toCharArray(), REDIRECT_URI);
                return true;
            } catch (SsoException exception) {
                return false;
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(exchange, exchange));
            long successes = results.stream().filter(result -> {
                try {
                    return result.get();
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }).count();
            assertThat(successes).isEqualTo(1);
        }
    }

    @Test
    void expiredCodeIsRejected() {
        LoginFlow flow = login();
        clock.advance(Duration.ofSeconds(31));

        assertThatThrownBy(() -> service.exchange(
            flow.code(),
            "app-a",
            CLIENT_SECRET.toCharArray(),
            REDIRECT_URI
        )).isInstanceOf(SsoException.class);
    }

    private LoginFlow login() {
        SsoService.AuthorizationStart start = service.beginAuthorization(
            new SsoService.AuthorizationRequest("app-a", REDIRECT_URI, "original-state"),
            null
        );
        SsoService.LoginPage page = service.prepareLogin(start.transactionId());
        SsoService.LoginResult result = service.login(
            page.transactionId(),
            page.csrfToken(),
            "Alice",
            USER_PASSWORD.toCharArray(),
            "127.0.0.1"
        );
        return new LoginFlow(result.sessionId(), queryParameter(result.redirectUri(), "code"));
    }

    private static String queryParameter(URI uri, String name) {
        for (String pair : uri.getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts[0].equals(name)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("Missing query parameter: " + name);
    }

    private record LoginFlow(String sessionId, String code) {
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static final String USER_PASSWORD = "correct horse battery staple";
    private static final String CLIENT_SECRET = "client-secret-value";
    private static final URI REDIRECT_URI = URI.create("https://app.example/callback");

    private MutableClock clock;
    private Pbkdf2PasswordHasher hasher;
    private InMemoryLocalSsoService service;
}
