package top.ilovemyhome.dagtask.scheduler.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentDispatcher;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentUnreachableException;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.dagtask.si.dto.SubmitRequest;
import top.ilovemyhome.dagtask.si.enums.TaskType;

import static top.ilovemyhome.dagtask.si.Constants.API_SUBMIT;
import static top.ilovemyhome.dagtask.si.Constants.API_VERSION;

/**
 * HTTP implementation of the AgentDispatcher outbound port.
 */
public class HttpAgentDispatcher implements AgentDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAgentDispatcher.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpAgentDispatcher(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newHttpClient());
    }

    public HttpAgentDispatcher(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public DispatchAck dispatch(AgentStatus targetAgent, TaskRecord task) {
        Objects.requireNonNull(targetAgent, "targetAgent must not be null");
        Objects.requireNonNull(task, "task must not be null");
        String submitUrl = buildAgentUrl(targetAgent.getAgentUrl()) + API_VERSION + API_SUBMIT;
        SubmitRequest submitRequest = new SubmitRequest(
            task.getId(), task.getName(), TaskType.JAVA_CLASS_NAME, task.getExecutionKey(), task.getInput());
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(submitRequest);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize submission request for task {}", task.getId(), e);
            return new DispatchAck(false, e.getMessage());
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(submitUrl))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return toAck(targetAgent, task, response);
        } catch (IOException e) {
            throw new AgentUnreachableException(
                "IOException connecting to agent " + targetAgent.getAgentId() + " at " + targetAgent.getAgentUrl(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentUnreachableException("Interrupted while connecting to agent " + targetAgent.getAgentId(), e);
        }
    }

    private DispatchAck toAck(AgentStatus targetAgent, TaskRecord task, HttpResponse<String> response) {
        int statusCode = response.statusCode();
        if (statusCode == 202) {
            LOGGER.info("Task {} dispatched successfully to agent {}", task.getId(), targetAgent.getAgentId());
            return new DispatchAck(true, "");
        }
        if (statusCode == 429) {
            return new DispatchAck(false, "pending queue is full (429)");
        }
        if (statusCode == 400) {
            return new DispatchAck(false, "bad request (400): " + response.body());
        }
        return new DispatchAck(false, "unexpected status code " + statusCode + ": " + response.body());
    }

    private static String buildAgentUrl(String agentUrl) {
        if (agentUrl.endsWith("/")) {
            return agentUrl.substring(0, agentUrl.length() - 1);
        }
        return agentUrl;
    }
}
