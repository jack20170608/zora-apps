package top.ilovemyhome.hosthelper.muserver.sso;


import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import top.ilovemyhome.zora.httpclient.HttpRetryClient;
import top.ilovemyhome.zora.httpclient.RestClient;

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
        this.restClient = new RestClient(
            configuration.exchangeEndpoint().resolve("/").toString(),
            HttpRetryClient.builder(httpClient).withMaxAttempts(1).build()
        );
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
            CompletableFuture<HttpResponse<String>> exchange = restClient.post(
                "sso/token/exchange",
                null,
                Map.of("Content-Type", "application/x-www-form-urlencoded", "Accept", "application/json"),
                form
            );
            HttpResponse<String> response;
            try {
                response = exchange.get(configuration.requestTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException exception) {
                exchange.cancel(true);
                throw new SsoAuthenticationException("SSO code exchange timed out", exception);
            }
            if (response.body().getBytes(StandardCharsets.UTF_8).length > configuration.maxResponseBytes()) {
                throw new SsoAuthenticationException("SSO response exceeded the configured size limit");
            }
            if (response.statusCode() != 200) {
                throw new SsoAuthenticationException("SSO code exchange was rejected");
            }
            String contentType = response.headers().firstValue("content-type").orElse("");
            if (!contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
                throw new SsoAuthenticationException("SSO returned an unexpected content type");
            }
            return parseIdentity(response.body());
        } catch (IOException exception) {
            throw new SsoAuthenticationException("SSO code exchange failed", exception);
        } catch (ExecutionException exception) {
            throw new SsoAuthenticationException("SSO code exchange failed", exception.getCause());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SsoAuthenticationException("SSO code exchange was interrupted", exception);
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    private SsoIdentity parseIdentity(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        if (!root.path("success").asBoolean() || !root.path("data").isObject()) {
            throw new SsoAuthenticationException("SSO returned an invalid identity response");
        }
        JsonNode identity = root.path("data");
        Set<String> roles = new HashSet<>();
        JsonNode roleNodes = identity.path("roles");
        if (!roleNodes.isArray()) {
            throw new SsoAuthenticationException("SSO identity roles are invalid");
        }
        roleNodes.forEach(node -> roles.add(requireText(node.textValue(), "role")));
        try {
            return new SsoIdentity(
                requireText(identity.path("subject").textValue(), "subject"),
                requireText(identity.path("username").textValue(), "username"),
                requireText(identity.path("displayName").textValue(), "displayName"),
                Set.copyOf(roles),
                Instant.parse(requireText(identity.path("authenticatedAt").textValue(), "authenticatedAt"))
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
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
}
