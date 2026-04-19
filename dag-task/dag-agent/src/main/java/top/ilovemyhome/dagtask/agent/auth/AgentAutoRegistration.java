package top.ilovemyhome.dagtask.agent.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Coordinates the automatic agent registration process when no token exists locally.
 * <p>
 * When the agent starts up and no token is found in local storage, this class
 * initiates the registration flow with the scheduling server:
 * <ol>
 *     <li>Generate a random nonce for replay protection</li>
 *     <li>Send registration request to server's public registration endpoint</li>
 *     <li>If already approved (auto-approval via whitelist), the server will
 *         immediately push the token back to our callback URL</li>
 *     <li>If pending, waits for admin approval - the token will be pushed later</li>
 * </ol>
 * </p>
 */
public class AgentAutoRegistration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentAutoRegistration.class);

    private final AgentConfiguration config;
    private final LocalTokenStorage tokenStorage;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final SecureRandom random = new SecureRandom();

    // Track pending registration completion
    private final CompletableFuture<String> registrationFuture = new CompletableFuture<>();

    public AgentAutoRegistration(AgentConfiguration config,
                                  LocalTokenStorage tokenStorage,
                                  ObjectMapper objectMapper) {
        this.config = config;
        this.tokenStorage = tokenStorage;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Start the auto-registration process if no token is currently available.
     *
     * @return the token if already loaded from storage or obtained via registration,
     *         null if registration is pending and token will be delivered later via callback
     */
    public String startIfNeeded() {
        // Check if we already have a token in config
        if (config.getToken() != null && !config.getToken().isBlank()) {
            LOGGER.info("Token already present in configuration, skipping auto-registration");
            return config.getToken();
        }

        // Check if we have a token saved locally
        String storedToken = tokenStorage.load();
        if (storedToken != null && !storedToken.isBlank()) {
            LOGGER.info("Using token loaded from local storage");
            config.setToken(storedToken);
            return storedToken;
        }

        if (!config.isAutoRegister()) {
            LOGGER.info("Auto-registration is disabled, no token available");
            return null;
        }

        // Start registration
        LOGGER.info("No existing token found, starting auto-registration with server");
        boolean started = initiateRegistration();

        if (!started) {
            LOGGER.error("Failed to initiate auto-registration");
            return null;
        }

        // Wait a bit for the token to be pushed back in case of auto-approval
        try {
            String token = registrationFuture.get(30, TimeUnit.SECONDS);
            if (token != null) {
                LOGGER.info("Auto-registration completed successfully, token received");
                config.setToken(token);
                tokenStorage.save(token);
                return token;
            }
        } catch (TimeoutException e) {
            LOGGER.info("Registration still pending approval from administrator. " +
                "Token will be delivered via callback when approved.");
        } catch (Exception e) {
            LOGGER.error("Error waiting for registration completion", e);
        }

        // Return null - registration is still pending
        return null;
    }

    /**
     * Complete the registration when token is received via callback.
     *
     * @param token the JWT token received from the server
     */
    public void completeRegistration(String token) {
        LOGGER.info("Completing auto-registration with received token");
        registrationFuture.complete(token);
        config.setToken(token);
        tokenStorage.save(token);
    }

    /**
     * Generate a random nonce for this registration.
     * <p>
     * The server must include the same nonce in the callback request to prove
     * that the callback is legitimate and not a replay attack.
     *
     * @return random nonce string
     */
    private String generateNonce() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Send the registration request to the server.
     *
     * @return true if request was sent successfully
     */
    private boolean initiateRegistration() {
        String nonce = generateNonce();
        String registrationEndpoint = config.getDagServerUrl() + "/api/v1/scheduler/registration/start";

        AgentRegistrationRequest request = new AgentRegistrationRequest(
            config.getAgentId(),
            getDescription(),
            getLabels(),
            config.getAgentUrl()
        );

        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(registrationEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                LOGGER.info("Registration request sent successfully, status={}", status);
                return true;
            } else {
                LOGGER.error("Registration request failed with status code {}, body={}",
                    status, response.body());
                return false;
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to send registration request", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String getDescription() {
        return System.getProperty("agent.description", "Auto-registered agent");
    }

    private Map<String, String> getLabels() {
        // Could add system information here (hostname, os, etc.)
        return Map.of(
            "hostname", System.getenv().getOrDefault("HOSTNAME", "unknown"),
            "startedAt", Instant.now().toString()
        );
    }
}
