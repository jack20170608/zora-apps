package top.ilovemyhome.dagtask.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * HTTP-based implementation of {@link AgentSchedulerClient} that communicates with the DAG scheduling server.
 * Uses Java 11 HttpClient to send HTTP requests and Jackson for JSON serialization.
 */
public class DefaultAgentSchedulerClient implements AgentSchedulerClient {

    private final AgentConfiguration config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a DefaultAgentSchedulerClient with default HttpClient and ObjectMapper.
     *
     * @param config the agent configuration
     */
    public DefaultAgentSchedulerClient(AgentConfiguration config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a DefaultAgentSchedulerClient with injected dependencies.
     * Useful for testing with mocks.
     *
     * @param config the agent configuration
     * @param httpClient the HTTP client to use
     * @param objectMapper the object mapper for JSON serialization
     */
    public DefaultAgentSchedulerClient(AgentConfiguration config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Registers the agent with the DAG scheduling server by sending registration information.
     *
     * @param registration the agent registration information containing agent ID, URL, and capacity
     * @return the HTTP response from the server containing status and any response data
     */
    @Override
    public Response register(AgentRegisterRequest registration) {
        try {
            String json = objectMapper.writeValueAsString(registration);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getDagServerUrl() + "/api/agent/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                int taskCount = registration.supportedExecutionKeys().size();
                LOGGER.info("Agent {} successfully registered with DAG server at {}, supported {} execution keys",
                        registration.agentId(), config.getDagServerUrl(), taskCount);
            } else {
                LOGGER.error("Agent {} registration failed with status {}", registration.agentId(), response.statusCode());
            }
            return Response.status(response.statusCode())
                    .entity(response.body())
                    .build();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to register agent with DAG server", e);
            Thread.currentThread().interrupt();
            return Response.serverError()
                    .entity(e.getMessage())
                    .build();
        }
    }

    /**
     * Unregisters the agent from the DAG scheduling server.
     *
     * @param unregistration the unregistration information containing just the agent ID
     * @return the HTTP response from the server
     */
    @Override
    public Response unregister(AgentUnregistration unregistration) {
        try {
            String json = objectMapper.writeValueAsString(unregistration);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getDagServerUrl() + "/api/agent/unregister"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                LOGGER.info("Agent {} successfully unregistered from DAG server at {}",
                        unregistration.agentId(), config.getDagServerUrl());
            } else {
                LOGGER.error("Agent {} unregistration failed with status {}", unregistration.agentId(), response.statusCode());
            }
            return Response.status(response.statusCode())
                    .entity(response.body())
                    .build();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to unregister agent from DAG server", e);
            Thread.currentThread().interrupt();
            return Response.serverError()
                    .entity(e.getMessage())
                    .build();
        }
    }

    /**
     * Reports the result of a task execution back to the DAG scheduling server.
     *
     * @param results the task result report containing agent ID, task ID, success status, and output
     * @return the HTTP response from the server
     */
    @Override
    public Response reportTaskResult(List<TaskExecuteResult> results) {
        try {
            String json = objectMapper.writeValueAsString(results);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getDagServerUrl() + "/api/agent/report"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                LOGGER.debug("Result {} reported successfully", json);
            } else {
                LOGGER.warn("Result {} report failed with status {}", json, response.statusCode());
            }
            return Response.status(response.statusCode())
                    .entity(response.body())
                    .build();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to report result for task {}", results.size(), e);
            Thread.currentThread().interrupt();
            return Response.serverError()
                    .entity(e.getMessage())
                    .build();
        }
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAgentSchedulerClient.class);
}
