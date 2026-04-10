package top.ilovemyhome.dagtask.core.dispatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskDispatchRecord;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.AgentRegistryItem;
import top.ilovemyhome.dagtask.si.dto.SubmitRequest;
import top.ilovemyhome.dagtask.si.persistence.AgentRegistryDao;
import top.ilovemyhome.dagtask.si.persistence.TaskDispatchDao;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        List<AgentRegistryItem> activeAgents = agentRegistryDao.findAllActive();
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
    public boolean killTask(Long taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");

        return taskDispatchDao.findByTaskId(taskId)
            .stream().filter(d -> d.getStatus() == TaskDispatchRecord.DispatchStatus.DISPATCHED)
            .findFirst()
            .map(dispatch -> {
                // Find the agent info
                String agentId = dispatch.getAgentId();
                return agentRegistryDao.findByAgentId(agentId)
                    .map(agent -> killTask(taskId, agent))
                    .orElse(false);
            })
            .orElse(false);
    }

    /**
     * Requests a task to be killed on its executing agent.
     *
     * @param taskId the task ID to kill
     * @param agent the agent that is executing the task
     * @return true if the kill request was accepted by the agent
     */
    public boolean killTask(Long taskId, AgentRegistryItem agent) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(agent, "agent must not be null");

        String url = buildAgentUrl(agent.getAgentUrl()) + "/api/kill/" + taskId;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Kill request accepted for task {} on agent {}", taskId, agent.getAgentId());
                return true;
            }

            logger.warn("Kill request failed for task {} on agent {}, status code: {}",
                taskId, agent.getAgentId(), statusCode);
            return false;
        } catch (IOException | InterruptedException e) {
            logger.error("IOException sending kill request to agent {} for task {}",
                agent.getAgentId(), taskId, e);
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
     * @return true if the force-ok request was accepted by the agent
     */
    public boolean forceOkTask(Long taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        return taskDispatchDao.findByTaskId(taskId)
            .stream()
            .filter(d -> d.getStatus() == TaskDispatchRecord.DispatchStatus.DISPATCHED)
            .findFirst()
            .map(dispatch -> {
                String agentId = dispatch.getAgentId();
                return agentRegistryDao.findByAgentId(agentId)
                    .map(agent -> forceOkTask(taskId, agent))
                    .orElse(false);
            })
            .orElse(false);
    }

    /**
     * Requests a task to be forcefully marked as successful on its executing agent.
     *
     * @param taskId the task ID to force ok
     * @param agent the agent that is executing the task
     * @return true if the force-ok request was accepted by the agent
     */
    public boolean forceOkTask(Long taskId, AgentRegistryItem agent) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(agent, "agent must not be null");

        String url = buildAgentUrl(agent.getAgentUrl()) + "/api/force-ok/" + taskId;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Force-ok request accepted for task {} on agent {}", taskId, agent.getAgentId());
                return true;
            }

            logger.warn("Force-ok request failed for task {} on agent {}, status code: {}",
                taskId, agent.getAgentId(), statusCode);
            return false;
        } catch (IOException | InterruptedException e) {
            logger.error("IOException sending force-ok request to agent {} for task {}",
                agent.getAgentId(), taskId, e);
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
        String submitUrl = buildAgentUrl(agentUrl) + "/api/submit";

        // Build the submission request matching what the agent expects
        // The executionClass is the executionKey - agent uses this to instantiate the correct executor
        SubmitRequest submitRequest = new SubmitRequest(
            task.getId(),
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
            .withStatus(TaskDispatchRecord.DispatchStatus.DISPATCHED)
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
                taskDispatchDao.updateStatus(task.getId(), TaskDispatchRecord.DispatchStatus.ACCEPTED);
                logger.info("Task {} dispatched successfully to agent {} at {}",
                    task.getId(), agent.getAgentId(), agentUrl);
                return DispatchResult.success(agent, task.getId());
            }

            if (statusCode == 429) {
                // Too Many Requests - agent's pending queue is full
                taskDispatchDao.updateStatus(task.getId(), TaskDispatchRecord.DispatchStatus.REJECTED);
                logger.warn("Agent {} rejected task {}: pending queue is full (429)",
                    agent.getAgentId(), task.getId());
                return DispatchResult.agentQueueFull(agent);
            }

            if (statusCode == 400) {
                taskDispatchDao.updateStatus(task.getId(), TaskDispatchRecord.DispatchStatus.REJECTED);
                logger.warn("Agent {} rejected task {}: bad request (400), response: {}",
                    agent.getAgentId(), task.getId(), response.body());
                return DispatchResult.badRequest(agent, response.body());
            }

            taskDispatchDao.updateStatus(task.getId(), TaskDispatchRecord.DispatchStatus.FAILED);
            logger.warn("Agent {} rejected task {}: unexpected status code {}",
                agent.getAgentId(), task.getId(), statusCode);
            return DispatchResult.unexpectedHttpStatus(agent, statusCode, response.body());
        } catch (IOException e) {
            taskDispatchDao.updateStatus(task.getId(), TaskDispatchRecord.DispatchStatus.FAILED);
            logger.error("IOException connecting to agent {} at {} for task {}",
                agent.getAgentId(), agentUrl, task.getId(), e);
            return DispatchResult.connectionFailed(agent, e.getMessage());
        } catch (InterruptedException e) {
            taskDispatchDao.updateStatus(task.getId(), TaskDispatchRecord.DispatchStatus.FAILED);
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
        List<AgentRegistryItem> active = agentRegistryDao.findAllActive();
        return filterCandidates(active, executionKey).size();
    }

    /**
     * Gets all available candidate agents for a given execution key.
     *
     * @param executionKey the execution key
     * @return list of available candidate agents (active + supports execution key + has capacity)
     */
    public List<AgentRegistryItem> getAvailableCandidates(String executionKey) {
        List<AgentRegistryItem> active = agentRegistryDao.findAllActive();
        List<AgentRegistryItem> filtered = filterCandidates(active, executionKey);
        return filterByCapacity(filtered);
    }

    // =========================================================================
    // Result record for dispatch operations
    // =========================================================================

    /**
     * Result of a task dispatch operation.
     * <p>
     * Contains information about whether the dispatch succeeded, and if not,
     * what the reason for failure was.
     * </p>
     *
     * @param success whether dispatch was successful
     * @param selectedAgent the agent that accepted the task (null if failed)
     * @param dispatchedTaskId the ID of the dispatched task (null if failed)
     * @param message human-readable message describing the outcome
     */
    public record DispatchResult(
        boolean success,
        AgentRegistryItem selectedAgent,
        Long dispatchedTaskId,
        String message
    ) {

        public static DispatchResult success(AgentRegistryItem agent, Long taskId) {
            return new DispatchResult(true, agent, taskId,
                String.format("Task %d dispatched successfully to agent %s at %s",
                    taskId, agent.getAgentId(), agent.getAgentUrl()));
        }

        public static DispatchResult noAvailableAgent(String reason) {
            return new DispatchResult(false, null, null,
                "No available agents: " + reason);
        }

        public static DispatchResult noCandidateForExecutionKey(String executionKey) {
            return new DispatchResult(false, null, null,
                "No active agents available that support execution key: " + executionKey);
        }

        public static DispatchResult allCandidatesAtCapacity(String executionKey) {
            return new DispatchResult(false, null, null,
                "All agents supporting " + executionKey + " are at full concurrent capacity");
        }

        public static DispatchResult selectionFailed() {
            return new DispatchResult(false, null, null,
                "Load balancing strategy failed to select an agent from candidates");
        }

        public static DispatchResult serializationError(String message) {
            return new DispatchResult(false, null, null,
                "Failed to serialize request: " + message);
        }

        public static DispatchResult agentQueueFull(AgentRegistryItem agent) {
            return new DispatchResult(false, agent, null,
                String.format("Agent %s at %s has full pending queue",
                    agent.getAgentId(), agent.getAgentUrl()));
        }

        public static DispatchResult badRequest(AgentRegistryItem agent, String response) {
            return new DispatchResult(false, agent, null,
                String.format("Agent %s returned 400 Bad Request: %s",
                    agent.getAgentId(), response));
        }

        public static DispatchResult unexpectedHttpStatus(AgentRegistryItem agent, int statusCode, String body) {
            return new DispatchResult(false, agent, null,
                String.format("Agent %s returned unexpected status code %d: %s",
                    agent.getAgentId(), statusCode, body));
        }

        public static DispatchResult connectionFailed(AgentRegistryItem agent, String error) {
            return new DispatchResult(false, agent, null,
                String.format("Connection failed to agent %s at %s: %s",
                    agent.getAgentId(), agent.getAgentUrl(), error));
        }

        public static DispatchResult interrupted() {
            return new DispatchResult(false, null, null,
                "Dispatch was interrupted by another thread");
        }
    }
}
