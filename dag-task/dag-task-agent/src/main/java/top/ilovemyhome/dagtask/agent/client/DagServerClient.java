package top.ilovemyhome.dagtask.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Client for communicating with the DAG scheduling server.
 */
public class DagServerClient {

    private final AgentConfiguration config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DagServerClient(AgentConfiguration config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public DagServerClient(AgentConfiguration config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Register this agent with the DAG server.
     *
     * @return true if registration succeeded
     */
    public boolean register() {
        try {
            Map<String, Object> registration = Map.of(
                    "agentId", config.getAgentId(),
                    "agentUrl", config.getBaseUrl(),
                    "maxConcurrentTasks", config.getMaxConcurrentTasks(),
                    "maxPendingTasks", config.getMaxPendingTasks(),
                    "supportedExecutionKeys", config.getSupportedExecutionKeys()
            );

            String json = objectMapper.writeValueAsString(registration);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getDagServerUrl() + "/api/agent/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                int taskCount = config.getSupportedExecutionKeys().size();
                LOGGER.info("Agent {} successfully registered with DAG server at {}, supported {} execution keys",
                        config.getAgentId(), config.getDagServerUrl(), taskCount);
            } else {
                LOGGER.error("Agent {} registration failed with status {}", config.getAgentId(), response.statusCode());
            }
            return success;
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to register agent with DAG server", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Report task execution result back to server.
     *
     * @param taskId the task ID
     * @param success whether execution succeeded
     * @param output the output JSON
     * @return true if report succeeded
     */
    public boolean reportResult(long taskId, boolean success, String output) {
        try {
            Map<String, Object> result = Map.of(
                    "agentId", config.getAgentId(),
                    "taskId", taskId,
                    "success", success,
                    "output", output
            );

            String json = objectMapper.writeValueAsString(result);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getDagServerUrl() + "/api/agent/report"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to report result for task {}", taskId, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DagServerClient.class);
}
