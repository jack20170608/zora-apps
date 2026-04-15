package top.ilovemyhome.dagtask.core.dispatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.DispatchResult;
import top.ilovemyhome.dagtask.si.TaskDispatchRecord;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.AgentRegistryItem;
import top.ilovemyhome.dagtask.si.dto.AgentRegistrySearchCriteria;
import top.ilovemyhome.dagtask.si.dto.OperationRequest;
import top.ilovemyhome.dagtask.si.dto.SubmitRequest;
import top.ilovemyhome.dagtask.si.enums.DispatchStatus;
import top.ilovemyhome.dagtask.si.enums.OpsType;
import top.ilovemyhome.dagtask.si.enums.TaskType;
import top.ilovemyhome.dagtask.si.persistence.AgentRegistryDao;
import top.ilovemyhome.dagtask.si.persistence.TaskDispatchDao;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static top.ilovemyhome.dagtask.si.Constants.*;

/**
 * Main dispatcher that selects an appropriate agent and dispatches a ready task
 * for execution on that agent.
 * <p>
 * This class:
 * <ol>
 *     <li>Queries the agent registry for all currently active agents</li>
 *     <li>Filters agents to only those that support the task's execution key</li>
 *     <li>Uses the configured load balancing strategy to select the best candidate</li>
 *     <li>Sends an HTTP submission request to the selected agent's endpoint</li>
 *     <li>Handles response and returns dispatch result</li>
 * </ol>
 * </p>
 * <p>
 * Filtering logic:
 * <ul>
 *     <li>Only includes agents that are marked as running (active)</li>
 *     <li>Only includes agents whose {@link AgentRegistryItem#getSupportedExecutionKeys()} contains
 *         the task's {@link TaskRecord#getExecutionKey()}</li>
 *     <li>Excludes agents that have already reached their maximum concurrent task limit</li>
 * </ul>
 * </p>
 * <p>
 * Usage:
 * <pre>{@code
 * TaskDispatcher dispatcher = new TaskDispatcher(
 *     agentRegistryDao,
 *     new LeastLoadLoadBalance(),
 *     objectMapper
 * );
 * DispatchResult result = dispatcher.dispatch(task);
 * if (result.isSuccess()) {
 *     // Task submitted successfully, agent will execute it
 * } else {
 *     // No available agent, task remains in READY state
 * }
 * }</pre>
 * </p>
 */
public class TaskDispatcher {

    private final AgentRegistryDao agentRegistryDao;
    private final TaskDispatchDao taskDispatchDao;
    private final LoadBalanceStrategy loadBalanceStrategy;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private static final Logger logger = LoggerFactory.getLogger(TaskDispatcher.class);

    /**
     * Creates a new TaskDispatcher with default HTTP client.
     *
     * @param agentRegistryDao DAO for accessing agent registry
     * @param taskDispatchDao DAO for tracking dispatch information
     * @param loadBalanceStrategy strategy for selecting among candidate agents
     * @param objectMapper Jackson object mapper for JSON serialization
     */
    public TaskDispatcher(AgentRegistryDao agentRegistryDao,
                          TaskDispatchDao taskDispatchDao,
                          LoadBalanceStrategy loadBalanceStrategy,
                          ObjectMapper objectMapper) {
        this(agentRegistryDao, taskDispatchDao, loadBalanceStrategy, objectMapper,
            HttpClient.newHttpClient());
    }

    /**
     * Creates a new TaskDispatcher with a provided HTTP client.
     *
     * @param agentRegistryDao DAO for accessing agent registry
     * @param taskDispatchDao DAO for tracking dispatch information
     * @param loadBalanceStrategy strategy for selecting among candidate agents
     * @param objectMapper Jackson object mapper for JSON serialization
     * @param httpClient HTTP client to use for sending requests to agents
     */
    public TaskDispatcher(AgentRegistryDao agentRegistryDao,
                          TaskDispatchDao taskDispatchDao,
                          LoadBalanceStrategy loadBalanceStrategy,
                          ObjectMapper objectMapper,
                          HttpClient httpClient) {
        this.agentRegistryDao = Objects.requireNonNull(agentRegistryDao, "agentRegistryDao must not be null");
        this.taskDispatchDao = Objects.requireNonNull(taskDispatchDao, "taskDispatchDao must not be null");
        this.loadBalanceStrategy = Objects.requireNonNull(loadBalanceStrategy, "loadBalanceStrategy must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.httpClient = httpClient;
    }

    /**
     * Dispatches a ready task to an appropriate agent for execution.
     * <p>
     * This method:
     * <ol>
     *     <li>Finds all active agents that support the task's execution key</li>
     *     <li>Filters out agents that are already at full capacity</li>
     *     <li>Uses load balancing strategy to select the best candidate</li>
     *     <li>Sends the task submission request to the selected agent</li>
     * </ol>
     * </p>
     *
     * @param task the ready task to dispatch
     * @return the dispatch result indicating success or failure with details
     */
    public DispatchResult dispatch(TaskRecord task) {
        Objects.requireNonNull(task, "task must not be null");
        String executionKey = task.getExecutionKey();

        // Step 1: Get all active agents
        List<AgentRegistryItem> activeAgents = findAllActiveAgents();
        if (activeAgents.isEmpty()) {
            logger.warn("No active agents available for task {}", task.getId());
            return DispatchResult.noAvailableAgent("No active agents registered in the system");
        }

        // Step 2: Filter by execution key support
        List<AgentRegistryItem> candidates = filterCandidates(activeAgents, executionKey);
        if (candidates.isEmpty()) {
            logger.warn("No active agents support execution key '{}' for task {}",
                executionKey, task.getId());
            return DispatchResult.noCandidateForExecutionKey(executionKey);
        }

        // Step 3: Filter by capacity (exclude agents that are full)
        candidates = filterByCapacity(candidates);
        if (candidates.isEmpty()) {
            logger.warn("All agents supporting execution key '{}' are at full capacity", executionKey);
            return DispatchResult.allCandidatesAtCapacity(executionKey);
        }

        // Step 4: Use load balancing strategy to select one agent
        AgentRegistryItem selected = loadBalanceStrategy.select(candidates);
        if (selected == null) {
            logger.warn("Load balance strategy returned null for {} candidates", candidates.size());
            return DispatchResult.selectionFailed();
        }

        // Step 5: Submit the task to the selected agent
        return submitToAgent(task, selected);
    }

    /**
     * Requests a task to be killed on its executing agent.
     * <p>
     * This overload automatically looks up the agent from the dispatch tracking table.
     *
     * @param taskId the task ID to kill
     * @return true if the kill request was accepted by the agent
     */
    public boolean killTask(Long taskId, String dealer, String reason) {
        Objects.requireNonNull(taskId, "taskId must not be null");

        return taskDispatchDao.findByTaskId(taskId)
            .stream().filter(d -> d.getStatus() == DispatchStatus.DISPATCHED)
            .findFirst()
            .map(dispatch -> {
                // Find the agent info
                String agentId = dispatch.getAgentId();
                return findAgentByAgentId(agentId)
                    .map(agent -> killTask(dispatch, dealer, reason))
                    .orElse(false);
            })
            .orElse(false);
    }

    public boolean killTask(TaskDispatchRecord dispatchItem, String dealer, String reason) {
        Objects.requireNonNull(dispatchItem, "dispatchDetail must not be null");
        var taskId = dispatchItem.getTaskId();
        var agentId = dispatchItem.getAgentId();
        String url = buildAgentUrl(dispatchItem.getAgentUrl()) + API_VERSION + API_KILL ;
        OperationRequest operationRequest = new OperationRequest(dispatchItem.getTaskId(), OpsType.KILL, true, reason, dealer, Instant.now());
        String body = JacksonUtil.toJson(operationRequest);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Kill request accepted for task {} on agent {}", taskId, agentId);
                return true;
            }
            logger.warn("Kill request failed for task {} on agent {}, status code: {}", taskId, agentId, statusCode);
            return false;
        } catch (IOException | InterruptedException e) {
            logger.error("IOException sending kill request to agent {} for task {}", agentId, taskId, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Requests a task to be forcefully marked as successful on its executing agent.
     * <p>
     * This overload automatically looks up the agent from the dispatch tracking table.
     *
     * @param taskId the task ID to force ok
     * @param dealer the user who performed the operation
     * @param reason the reason for forcefully marking the task as ok
     * @return true if the force-ok request was accepted by the agent
     */
    public boolean forceOkTask(Long taskId, String dealer, String reason) {
        Objects.requireNonNull(taskId, "taskId must not be null");

        return taskDispatchDao.findByTaskId(taskId)
            .stream().filter(d -> d.getStatus() == DispatchStatus.DISPATCHED)
            .findFirst()
            .map(dispatch -> {
                // Find the agent info
                String agentId = dispatch.getAgentId();
                return findAgentByAgentId(agentId)
                    .map(agent -> forceOkTask(dispatch, dealer, reason))
                    .orElse(false);
            })
            .orElse(false);
    }

    /**
     * Requests a task to be forcefully marked as successful on its executing agent.
     *
     * @param dispatchItem the dispatch record containing task and agent information
     * @param dealer the user who performed the operation
     * @param reason the reason for forcefully marking the task as ok
     * @return true if the force-ok request was accepted by the agent
     */
    public boolean forceOkTask(TaskDispatchRecord dispatchItem, String dealer, String reason) {
        Objects.requireNonNull(dispatchItem, "dispatchItem must not be null");
        var taskId = dispatchItem.getTaskId();
        var agentId = dispatchItem.getAgentId();
        String url = buildAgentUrl(dispatchItem.getAgentUrl()) + API_VERSION + API_FORCE_OK;
        OperationRequest operationRequest = new OperationRequest(dispatchItem.getTaskId(), OpsType.FORCE_OK, true, reason, dealer, Instant.now());
        String body = JacksonUtil.toJson(operationRequest);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Force-ok request accepted for task {} on agent {}", taskId, agentId);
                return true;
            }
            logger.warn("Force-ok request failed for task {} on agent {}, status code: {}", taskId, agentId, statusCode);
            return false;
        } catch (IOException | InterruptedException e) {
            logger.error("IOException sending force-ok request to agent {} for task {}", agentId, taskId, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Finds all candidate agents that:
     * <ul>
     *     <li>are currently running (active)</li>
     *     <li>support the requested execution key</li>
     * </ul>
     *
     * @param activeAgents list of all active agents
     * @param executionKey the execution key to filter by
     * @return filtered list of candidate agents
     */
    private List<AgentRegistryItem> filterCandidates(List<AgentRegistryItem> activeAgents, String executionKey) {
        return activeAgents.stream()
            .filter(agent -> agent.getSupportedExecutionKeys() == null
                || agent.getSupportedExecutionKeys().isEmpty()
                || agent.getSupportedExecutionKeys().contains(executionKey))
            .collect(Collectors.toList());
    }

    /**
     * Filters out agents that have already reached their maximum concurrent task limit.
     * An agent is considered available if {@code runningTasks < maxConcurrentTasks}.
     *
     * @param candidates the pre-filtered candidate list
     * @return filtered list containing only agents with available capacity
     */
    private List<AgentRegistryItem> filterByCapacity(List<AgentRegistryItem> candidates) {
        return candidates.stream()
            .filter(agent -> agent.getRunningTasks() < agent.getMaxConcurrentTasks())
            .collect(Collectors.toList());
    }

    /**
     * Submits a task to the selected agent via HTTP POST.
     * Also records the dispatch information in the tracking table.
     *
     * @param task the task to submit
     * @param agent the selected agent
     * @return the dispatch result
     */
    private DispatchResult submitToAgent(TaskRecord task, AgentRegistryItem agent) {
        String agentUrl = agent.getAgentUrl();
        String submitUrl = buildAgentUrl(agentUrl) + API_VERSION + API_SUBMIT;

        // Build the submission request matching what the agent expects
        // The executionClass is the executionKey - agent uses this to instantiate the correct executor
        SubmitRequest submitRequest = new SubmitRequest(
            task.getId(),
            TaskType.JAVA_CLASS_NAME,
            task.getExecutionKey(),
            task.getInput()
        );

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(submitRequest);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize submission request for task {}", task.getId(), e);
            return DispatchResult.serializationError(e.getMessage());
        }

        // Create and save dispatch record before sending the request
        TaskDispatchRecord dispatchRecord = TaskDispatchRecord.builder()
            .withTaskId(task.getId())
            .withAgentId(agent.getAgentId())
            .withAgentUrl(agent.getAgentUrl())
            .withStatus(DispatchStatus.DISPATCHED)
            .build();
        taskDispatchDao.create(dispatchRecord);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(submitUrl))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            // Update dispatch status based on agent response
            if (statusCode == 202) {
                // Accepted - task is queued
                taskDispatchDao.updateStatus(task.getId(), DispatchStatus.ACCEPTED);
                logger.info("Task {} dispatched successfully to agent {} at {}",
                    task.getId(), agent.getAgentId(), agentUrl);
                return DispatchResult.success(agent, task.getId());
            }

            if (statusCode == 429) {
                // Too Many Requests - agent's pending queue is full
                taskDispatchDao.updateStatus(task.getId(), DispatchStatus.REJECTED);
                logger.warn("Agent {} rejected task {}: pending queue is full (429)",
                    agent.getAgentId(), task.getId());
                return DispatchResult.agentQueueFull(agent);
            }

            if (statusCode == 400) {
                taskDispatchDao.updateStatus(task.getId(), DispatchStatus.REJECTED);
                logger.warn("Agent {} rejected task {}: bad request (400), response: {}",
                    agent.getAgentId(), task.getId(), response.body());
                return DispatchResult.badRequest(agent, response.body());
            }

            taskDispatchDao.updateStatus(task.getId(), DispatchStatus.FAILED);
            logger.warn("Agent {} rejected task {}: unexpected status code {}",
                agent.getAgentId(), task.getId(), statusCode);
            return DispatchResult.unexpectedHttpStatus(agent, statusCode, response.body());
        } catch (IOException e) {
            taskDispatchDao.updateStatus(task.getId(), DispatchStatus.FAILED);
            logger.error("IOException connecting to agent {} at {} for task {}",
                agent.getAgentId(), agentUrl, task.getId(), e);
            return DispatchResult.connectionFailed(agent, e.getMessage());
        } catch (InterruptedException e) {
            taskDispatchDao.updateStatus(task.getId(), DispatchStatus.FAILED);
            logger.error("Interrupted while connecting to agent {} for task {}",
                agent.getAgentId(), task.getId(), e);
            Thread.currentThread().interrupt();
            return DispatchResult.interrupted();
        }
    }

    /**
     * Builds the agent base URL, ensuring it has the correct format without trailing slash.
     *
     * @param agentUrl the raw agent URL from registration
     * @return the normalized URL
     */
    private String buildAgentUrl(String agentUrl) {
        String url = agentUrl;
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Gets the current number of active agents that support a given execution key.
     * Useful for monitoring and diagnostics.
     *
     * @param executionKey the execution key to count
     * @return number of available agents for this execution key
     */
    public int countAvailableAgents(String executionKey) {
        List<AgentRegistryItem> active = findAllActiveAgents();
        return filterCandidates(active, executionKey).size();
    }


    /**
     * Gets all available candidate agents for a given execution key.
     *
     * @param executionKey the execution key
     * @return list of available candidate agents (active + supports execution key + has capacity)
     */
    public List<AgentRegistryItem> getAvailableCandidates(String executionKey) {
        List<AgentRegistryItem> active = findAllActiveAgents();
        List<AgentRegistryItem> filtered = filterCandidates(active, executionKey);
        return filterByCapacity(filtered);
    }

    /**
     * Finds all currently active (running) agents from the registry.
     * Active agents are those marked as running in the database.
     *
     * @return list of all active agents
     */
    public List<AgentRegistryItem> findAllActiveAgents() {
        AgentRegistrySearchCriteria criteria = AgentRegistrySearchCriteria.builder()
                .withRunning(true)
                .build();
        return agentRegistryDao.search(criteria);
    }

    /**
     * Finds an agent by its unique agent ID.
     *
     * @param agentId the unique agent identifier to search for
     * @return an Optional containing the agent if found, empty otherwise
     */
    public java.util.Optional<AgentRegistryItem> findAgentByAgentId(String agentId) {
        AgentRegistrySearchCriteria criteria = AgentRegistrySearchCriteria.builder()
                .withAgentId(agentId)
                .build();
        return agentRegistryDao.search(criteria)
                .stream()
                .findFirst();
    }

    // =========================================================================
    // Result record for dispatch operations
    // =========================================================================


}
