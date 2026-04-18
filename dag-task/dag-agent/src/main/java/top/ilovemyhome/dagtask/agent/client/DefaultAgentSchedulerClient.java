package top.ilovemyhome.dagtask.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.Constants;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;
import top.ilovemyhome.zora.httpclient.HttpClients;
import top.ilovemyhome.zora.httpclient.RestClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * HTTP-based implementation of {@link AgentSchedulerClient} that communicates with the DAG scheduling server.
 * Uses Java 11 HttpClient to send HTTP requests and Jackson for JSON serialization.
 */
public class DefaultAgentSchedulerClient implements AgentSchedulerClient {

    private final AgentConfiguration config;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    /**
     * Creates a DefaultAgentSchedulerClient with default HttpClient and ObjectMapper.
     *
     * @param config the agent configuration
     */
    public DefaultAgentSchedulerClient(AgentConfiguration config) {
        this.config = config;
        this.restClient = RestClient.restClient(false, config.getBaseUrl(), null
            , (code) -> code < 300, 3, Duration.ofSeconds(5));
        this.objectMapper = new ObjectMapper();
    }

    public DefaultAgentSchedulerClient(AgentConfiguration config, ObjectMapper objectMapper) {
        this.config = config;
        this.restClient = RestClient.restClient(false, config.getBaseUrl(), null
            , (code) -> code < 300, 3, Duration.ofSeconds(5));
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
            HttpResponse<String> response = restClient.post(Constants.API_SCHEDULER + Constants.API_REGISTER, null
                , Map.of("Content-Type", MediaType.APPLICATION_JSON), json).get(60, TimeUnit.SECONDS);
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
        } catch (ExecutionException | IOException | TimeoutException e) {
            LOGGER.error("Failed to register agent with DAG server", e);
            return Response.serverError()
                .entity(e.getMessage())
                .build();
        } catch (InterruptedException e) {
            LOGGER.error("Registration request was interrupted", e);
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
            HttpResponse<String> response = restClient.post(Constants.API_SCHEDULER + Constants.API_UNREGISTER, null
                , Map.of("Content-Type", MediaType.APPLICATION_JSON), json).get(60, TimeUnit.SECONDS);
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
        } catch (ExecutionException | IOException | TimeoutException e) {
            LOGGER.error("Failed to unregister agent from DAG server", e);
            return Response.serverError()
                .entity(e.getMessage())
                .build();
        } catch (InterruptedException e) {
            LOGGER.error("Unregistration request was interrupted", e);
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
            HttpResponse<String> response = restClient.post(Constants.API_SCHEDULER + Constants.API_REPORT_RESULT, null
                , Map.of("Content-Type", MediaType.APPLICATION_JSON), json).get(60, TimeUnit.SECONDS);
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                LOGGER.debug("Result {} reported successfully", json);
            } else {
                LOGGER.warn("Result {} report failed with status {}", json, response.statusCode());
            }
            return Response.status(response.statusCode())
                .entity(response.body())
                .build();
        } catch (ExecutionException | IOException | TimeoutException e) {
            LOGGER.error("Failed to report result for task {}", results.size(), e);
            return Response.serverError()
                .entity(e.getMessage())
                .build();
        } catch (InterruptedException e) {
            LOGGER.error("Report task result request was interrupted", e);
            Thread.currentThread().interrupt();
            return Response.serverError()
                .entity(e.getMessage())
                .build();
        }
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAgentSchedulerClient.class);
}
