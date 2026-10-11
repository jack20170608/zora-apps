package top.ilovemyhome.hosthelper.muserver.sso;


import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ZoraSsoClient {

    public ZoraSsoClient(SsoClientConfiguration configuration) {
        this(
            configuration,
            HttpClient.newBuilder()
                .connectTimeout(configuration.requestTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(),
            new ObjectMapper()
        );
    }

    ZoraSsoClient(SsoClientConfiguration configuration, HttpClient httpClient, ObjectMapper objectMapper) {
        this.configuration = configuration;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public URI authorizationUri(String state) {
        String query = "client_id=" + encode(configuration.clientId())
            + "&redirect_uri=" + encode(configuration.redirectUri().toString())
            + "&state=" + encode(requireText(state, "state"));
        return URI.create(configuration.authorizationEndpoint() + "?" + query);
    }

    public SsoIdentity exchange(String code) {
        char[] secret = configuration.clientSecret();
        try {
            String form = "code=" + encode(requireText(code, "code"))
                + "&client_id=" + encode(configuration.clientId())
                + "&client_secret=" + encode(new String(secret))
                + "&redirect_uri=" + encode(configuration.redirectUri().toString());
            HttpRequest request = HttpRequest.newBuilder(configuration.exchangeEndpoint())
                .timeout(configuration.requestTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                byte[] bytes = body.readNBytes(configuration.maxResponseBytes() + 1);
                if (bytes.length > configuration.maxResponseBytes()) {
                    throw new SsoAuthenticationException("SSO response exceeded the configured size limit");
                }
                if (response.statusCode() != 200) {
                    throw new SsoAuthenticationException("SSO code exchange was rejected");
                }
                String contentType = response.headers().firstValue("content-type").orElse("");
                if (!contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
                    throw new SsoAuthenticationException("SSO returned an unexpected content type");
                }
                return parseIdentity(bytes);
            }
        } catch (IOException exception) {
            throw new SsoAuthenticationException("SSO code exchange failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SsoAuthenticationException("SSO code exchange was interrupted", exception);
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    private SsoIdentity parseIdentity(byte[] json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        Set<String> roles = new HashSet<>();
        JsonNode roleNodes = root.path("roles");
        if (!roleNodes.isArray()) {
            throw new SsoAuthenticationException("SSO identity roles are invalid");
        }
        roleNodes.forEach(node -> roles.add(requireText(node.textValue(), "role")));
        try {
            return new SsoIdentity(
                requireText(root.path("subject").textValue(), "subject"),
                requireText(root.path("username").textValue(), "username"),
                requireText(root.path("displayName").textValue(), "displayName"),
                Set.copyOf(roles),
                Instant.parse(requireText(root.path("authenticatedAt").textValue(), "authenticatedAt"))
            );
        } catch (DateTimeParseException exception) {
            throw new SsoAuthenticationException("SSO identity authenticatedAt is invalid", exception);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new SsoAuthenticationException("SSO identity " + name + " is missing");
        }
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private final SsoClientConfiguration configuration;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
}
