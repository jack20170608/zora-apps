package top.ilovemyhome.zorasso.core;

import top.ilovemyhome.zorasso.si.AuthenticatedIdentity;
import top.ilovemyhome.zorasso.si.IdentityAuthenticator;
import top.ilovemyhome.zorasso.si.SsoException;
import top.ilovemyhome.zorasso.si.SsoService;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static top.ilovemyhome.zorasso.si.SsoException.Code.*;

public final class InMemoryLocalSsoService implements SsoService {

    public InMemoryLocalSsoService(
        IdentityAuthenticator identityAuthenticator,
        PasswordHasher passwordHasher,
        Collection<RegisteredClient> clients
    ) {
        this(identityAuthenticator, passwordHasher, clients, Clock.systemUTC(), new SecureRandom(),
            Duration.ofMinutes(5), Duration.ofHours(2), Duration.ofHours(8), Duration.ofSeconds(30));
    }

    InMemoryLocalSsoService(
        IdentityAuthenticator identityAuthenticator,
        PasswordHasher passwordHasher,
        Collection<RegisteredClient> clients,
        Clock clock,
        SecureRandom random,
        Duration transactionTtl,
        Duration idleSessionTtl,
        Duration absoluteSessionTtl,
        Duration codeTtl
    ) {
        this.identityAuthenticator = Objects.requireNonNull(identityAuthenticator);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.clients = clients.stream().collect(Collectors.toUnmodifiableMap(
            RegisteredClient::clientId,
            Function.identity(),
            (left, right) -> {
                throw new IllegalArgumentException("duplicate clientId: " + left.clientId());
            }
        ));
        this.clock = Objects.requireNonNull(clock);
        this.random = Objects.requireNonNull(random);
        this.transactionTtl = requirePositive(transactionTtl, "transactionTtl");
        this.idleSessionTtl = requirePositive(idleSessionTtl, "idleSessionTtl");
        this.absoluteSessionTtl = requirePositive(absoluteSessionTtl, "absoluteSessionTtl");
        this.codeTtl = requirePositive(codeTtl, "codeTtl");
    }

    @Override
    public AuthorizationStart beginAuthorization(AuthorizationRequest request, String sessionId) {
        ValidatedRequest validated = validate(request);
        ActiveSession activeSession = findSession(sessionId);
        if (activeSession != null) {
            return AuthorizationStart.redirect(issueCodeRedirect(validated, activeSession));
        }
        String transactionId = randomToken();
        transactions.put(digest(transactionId), new Transaction(
            validated.client().clientId(),
            validated.redirectUri(),
            validated.state(),
            clock.instant().plus(transactionTtl),
            null
        ));
        return AuthorizationStart.loginRequired(transactionId);
    }

    @Override
    public LoginPage prepareLogin(String transactionId) {
        Transaction transaction = requireTransaction(transactionId);
        String csrfToken = randomToken();
        transactions.put(digest(transactionId), transaction.withCsrfDigest(digest(csrfToken)));
        return new LoginPage(transactionId, csrfToken, transaction.clientId());
    }

    @Override
    public LoginResult login(
        String transactionId,
        String csrfToken,
        String username,
        char[] password,
        String source
    ) {
        String rateKey = normalizeRateKey(username, source);
        if (isRateLimited(rateKey)) {
            throw new SsoException(RATE_LIMITED, "Login temporarily unavailable");
        }
        Transaction transaction = requireTransaction(transactionId);
        if (transaction.csrfDigest() == null || !MessageDigest.isEqual(
            transaction.csrfDigest().getBytes(StandardCharsets.US_ASCII),
            digest(csrfToken).getBytes(StandardCharsets.US_ASCII)
        )) {
            recordFailure(rateKey);
            throw new SsoException(INVALID_TRANSACTION, "Invalid login transaction");
        }
        AuthenticatedIdentity identity;
        try {
            identity = identityAuthenticator.authenticate(username, password);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
        if (identity == null) {
            recordFailure(rateKey);
            throw new SsoException(LOGIN_REJECTED, "Username or password is incorrect");
        }
        failures.remove(rateKey);
        if (!transactions.remove(digest(transactionId), transaction)) {
            throw new SsoException(INVALID_TRANSACTION, "Login transaction was already consumed");
        }
        Instant now = clock.instant();
        String sessionId = randomToken();
        Session session = new Session(identity, now, now.plus(idleSessionTtl), now.plus(absoluteSessionTtl));
        String sessionDigest = digest(sessionId);
        sessions.put(sessionDigest, session);
        URI redirect = issueCodeRedirect(
            new ValidatedRequest(clients.get(transaction.clientId()), transaction.redirectUri(), transaction.state()),
            new ActiveSession(sessionDigest, session)
        );
        return new LoginResult(sessionId, redirect);
    }

    @Override
    public AuthenticatedIdentity exchange(
        String code,
        String clientId,
        char[] clientSecret,
        URI redirectUri
    ) {
        RegisteredClient client = clients.get(clientId);
        String configuredHash = client == null ? DUMMY_SECRET_HASH : client.clientSecretHash();
        boolean validSecret;
        try {
            validSecret = passwordHasher.verify(clientSecret, configuredHash)
                && client != null && client.enabled();
        } finally {
            if (clientSecret != null) {
                Arrays.fill(clientSecret, '\0');
            }
        }
        if (!validSecret) {
            throw new SsoException(INVALID_CLIENT_CREDENTIALS, "Invalid client credentials");
        }
        String codeKey = digest(code);
        Instant now = clock.instant();
        AtomicReference<CodeGrant> consumed = new AtomicReference<>();
        codes.compute(codeKey, (ignored, grant) -> {
            if (grant == null) {
                return null;
            }
            if (!now.isBefore(grant.expiresAt())) {
                return null;
            }
            if (grant.clientId().equals(clientId)
                && grant.redirectUri().equals(redirectUri)
                && findSessionByDigest(grant.sessionDigest()) != null) {
                consumed.set(grant);
                return null;
            }
            return grant;
        });
        CodeGrant grant = consumed.get();
        if (grant == null) {
            throw new SsoException(INVALID_CODE, "Invalid or expired authorization code");
        }
        return grant.identity();
    }

    @Override
    public void logout(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            sessions.remove(digest(sessionId));
        }
    }

    private ValidatedRequest validate(AuthorizationRequest request) {
        if (request == null || request.clientId() == null || request.redirectUri() == null
            || request.state() == null || request.state().isBlank() || request.state().length() > 512) {
            throw new SsoException(INVALID_REQUEST, "Invalid authorization request");
        }
        RegisteredClient client = clients.get(request.clientId());
        if (client == null || !client.enabled()) {
            throw new SsoException(UNKNOWN_CLIENT, "Unknown client");
        }
        if (!client.redirectUris().contains(request.redirectUri())) {
            throw new SsoException(INVALID_REDIRECT_URI, "Redirect URI is not registered");
        }
        return new ValidatedRequest(client, request.redirectUri(), request.state());
    }

    private Transaction requireTransaction(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new SsoException(INVALID_TRANSACTION, "Invalid login transaction");
        }
        Transaction transaction = transactions.get(digest(transactionId));
        if (transaction == null || !clock.instant().isBefore(transaction.expiresAt())) {
            transactions.remove(digest(transactionId));
            throw new SsoException(INVALID_TRANSACTION, "Invalid or expired login transaction");
        }
        return transaction;
    }

    private ActiveSession findSession(String rawSessionId) {
        if (rawSessionId == null || rawSessionId.isBlank()) {
            return null;
        }
        return findSessionByDigest(digest(rawSessionId));
    }

    private ActiveSession findSessionByDigest(String sessionDigest) {
        Session session = sessions.get(sessionDigest);
        if (session == null) {
            return null;
        }
        Instant now = clock.instant();
        if (!now.isBefore(session.idleExpiresAt()) || !now.isBefore(session.absoluteExpiresAt())) {
            sessions.remove(sessionDigest, session);
            return null;
        }
        Session refreshed = session.withIdleExpiresAt(min(now.plus(idleSessionTtl), session.absoluteExpiresAt()));
        sessions.replace(sessionDigest, session, refreshed);
        return new ActiveSession(sessionDigest, refreshed);
    }

    private URI issueCodeRedirect(ValidatedRequest request, ActiveSession activeSession) {
        String rawCode = randomToken();
        codes.put(digest(rawCode), new CodeGrant(
            request.client().clientId(),
            request.redirectUri(),
            activeSession.digest(),
            activeSession.session().identity(),
            clock.instant().plus(codeTtl)
        ));
        String separator = request.redirectUri().getRawQuery() == null ? "?" : "&";
        return URI.create(request.redirectUri() + separator + "code=" + encode(rawCode)
            + "&state=" + encode(request.state()));
    }

    private boolean isRateLimited(String key) {
        Instant threshold = clock.instant().minus(LOGIN_FAILURE_WINDOW);
        Deque<Instant> attempts = failures.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            attempts.removeIf(attempt -> attempt.isBefore(threshold));
            return attempts.size() >= MAX_LOGIN_FAILURES;
        }
    }

    private void recordFailure(String key) {
        Deque<Instant> attempts = failures.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            attempts.addLast(clock.instant());
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String digest(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(rawValue.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String normalizeRateKey(String username, String source) {
        String normalized;
        try {
            normalized = LocalUser.normalizeUsername(username);
        } catch (IllegalArgumentException exception) {
            normalized = "<invalid>";
        }
        return normalized + "|" + (source == null ? "<unknown>" : source);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private static Duration requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }

    private record ValidatedRequest(RegisteredClient client, URI redirectUri, String state) {
    }

    private record Transaction(
        String clientId,
        URI redirectUri,
        String state,
        Instant expiresAt,
        String csrfDigest
    ) {
        Transaction withCsrfDigest(String value) {
            return new Transaction(clientId, redirectUri, state, expiresAt, value);
        }
    }

    private record Session(
        AuthenticatedIdentity identity,
        Instant createdAt,
        Instant idleExpiresAt,
        Instant absoluteExpiresAt
    ) {
        Session withIdleExpiresAt(Instant value) {
            return new Session(identity, createdAt, value, absoluteExpiresAt);
        }
    }

    private record ActiveSession(String digest, Session session) {
    }

    private record CodeGrant(
        String clientId,
        URI redirectUri,
        String sessionDigest,
        AuthenticatedIdentity identity,
        Instant expiresAt
    ) {
    }

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(5);
    private static final String DUMMY_SECRET_HASH =
        "pbkdf2-sha256$100000$AAAAAAAAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    private final IdentityAuthenticator identityAuthenticator;
    private final PasswordHasher passwordHasher;
    private final Map<String, RegisteredClient> clients;
    private final Clock clock;
    private final SecureRandom random;
    private final Duration transactionTtl;
    private final Duration idleSessionTtl;
    private final Duration absoluteSessionTtl;
    private final Duration codeTtl;
    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, CodeGrant> codes = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
}
