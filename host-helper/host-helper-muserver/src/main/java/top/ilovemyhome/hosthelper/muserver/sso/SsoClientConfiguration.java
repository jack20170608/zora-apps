package top.ilovemyhome.hosthelper.muserver.sso;

import com.typesafe.config.Config;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class SsoClientConfiguration {

    public static SsoClientConfiguration from(Config root) {
        Config config = root.getConfig("sso");
        Config mappings = config.getConfig("role-mappings");
        Map<String, List<String>> roleMappings = mappings.root().keySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                role -> role,
                role -> List.copyOf(mappings.getStringList(role))
            ));
        return new SsoClientConfiguration(
            URI.create(config.getString("base-uri")),
            config.getString("client-id"),
            config.getString("client-secret").toCharArray(),
            URI.create(config.getString("redirect-uri")),
            config.getString("login-path"),
            config.getString("callback-path"),
            config.getDuration("request-timeout"),
            config.getDuration("state-ttl"),
            config.getInt("max-pending-states"),
            config.getInt("max-response-bytes"),
            config.getBoolean("secure-cookie"),
            roleMappings
        );
    }

    SsoClientConfiguration(
        URI baseUri,
        String clientId,
        char[] clientSecret,
        URI redirectUri,
        String loginPath,
        String callbackPath,
        Duration requestTimeout,
        Duration stateTtl,
        int maxPendingStates,
        int maxResponseBytes,
        boolean secureCookie,
        Map<String, List<String>> roleMappings
    ) {
        this.baseUri = requireHttpUri(baseUri, "baseUri");
        this.clientId = requireText(clientId, "clientId");
        this.clientSecret = Arrays.copyOf(Objects.requireNonNull(clientSecret, "clientSecret"), clientSecret.length);
        if (this.clientSecret.length == 0 ) {
            throw new IllegalArgumentException("clientSecret must be provided securely");
        }
        this.redirectUri = requireHttpUri(redirectUri, "redirectUri");
        this.loginPath = requirePath(loginPath, "loginPath");
        this.callbackPath = requirePath(callbackPath, "callbackPath");
        if (!this.redirectUri.getPath().equals(this.callbackPath)) {
            throw new IllegalArgumentException("redirectUri path must equal callbackPath");
        }
        this.requestTimeout = requirePositive(requestTimeout, "requestTimeout");
        this.stateTtl = requirePositive(stateTtl, "stateTtl");
        if (maxPendingStates < 1) {
            throw new IllegalArgumentException("maxPendingStates must be positive");
        }
        if (maxResponseBytes < 1 || maxResponseBytes > 1_048_576) {
            throw new IllegalArgumentException("maxResponseBytes must be between 1 and 1048576");
        }
        this.maxPendingStates = maxPendingStates;
        this.maxResponseBytes = maxResponseBytes;
        this.secureCookie = secureCookie;
        this.roleMappings = roleMappings.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                entry -> requireText(entry.getKey(), "SSO role"),
                entry -> List.copyOf(entry.getValue())
            ));
        if (this.roleMappings.isEmpty()) {
            throw new IllegalArgumentException("roleMappings must not be empty");
        }
        if (!isLocalhost(this.redirectUri) && !this.secureCookie) {
            throw new IllegalArgumentException("Non-local redirect URI requires a secure cookie");
        }
    }

    public URI authorizationEndpoint() {
        return baseUri.resolve("/authorize");
    }

    public URI exchangeEndpoint() {
        return baseUri.resolve("/sso/token/exchange");
    }

    public String clientId() {
        return clientId;
    }

    public char[] clientSecret() {
        return Arrays.copyOf(clientSecret, clientSecret.length);
    }

    public URI redirectUri() {
        return redirectUri;
    }

    public String loginPath() {
        return loginPath;
    }

    public String callbackPath() {
        return callbackPath;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public Duration stateTtl() {
        return stateTtl;
    }

    public int maxPendingStates() {
        return maxPendingStates;
    }

    public int maxResponseBytes() {
        return maxResponseBytes;
    }

    public boolean secureCookie() {
        return secureCookie;
    }

    public Map<String, List<String>> roleMappings() {
        return roleMappings;
    }

    private static URI requireHttpUri(URI uri, String name) {
        Objects.requireNonNull(uri, name);
        if (!uri.isAbsolute() || uri.getHost() == null || uri.getFragment() != null
            || (!"https".equalsIgnoreCase(uri.getScheme()) && !isLocalhost(uri))) {
            throw new IllegalArgumentException(name + " must be an absolute HTTPS URI without a fragment");
        }
        return uri;
    }

    private static boolean isLocalhost(URI uri) {
        return "localhost".equalsIgnoreCase(uri.getHost())
            || "127.0.0.1".equals(uri.getHost())
            || "::1".equals(uri.getHost());
    }

    private static String requirePath(String value, String name) {
        String path = requireText(value, name);
        if (!path.startsWith("/") || path.startsWith("//") || path.contains("?") || path.contains("#")) {
            throw new IllegalArgumentException(name + " must be an absolute path");
        }
        return path;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private final URI baseUri;
    private final String clientId;
    private final char[] clientSecret;
    private final URI redirectUri;
    private final String loginPath;
    private final String callbackPath;
    private final Duration requestTimeout;
    private final Duration stateTtl;
    private final int maxPendingStates;
    private final int maxResponseBytes;
    private final boolean secureCookie;
    private final Map<String, List<String>> roleMappings;
}
