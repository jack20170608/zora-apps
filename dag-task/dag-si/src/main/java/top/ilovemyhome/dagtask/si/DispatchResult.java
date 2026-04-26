package top.ilovemyhome.dagtask.si;

import top.ilovemyhome.dagtask.si.agent.AgentStatus;

/**
 * Result of a task dispatch operation.
 * <p>
 * Contains information about whether the dispatch succeeded, and if not,
 * what the reason for failure was.
 * </p>
 *
 * @param success          whether dispatch was successful
 * @param selectedAgent    the agent that accepted the task (null if failed)
 * @param dispatchedTaskId the ID of the dispatched task (null if failed)
 * @param message          human-readable message describing the outcome
 */
public record DispatchResult(
    boolean success,
    AgentStatus selectedAgent,
    Long dispatchedTaskId,
    String message) {

    public static DispatchResult success(AgentStatus agent, Long taskId) {
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

    public static DispatchResult agentQueueFull(AgentStatus agent) {
        return new DispatchResult(false, agent, null,
            String.format("Agent %s at %s has full pending queue",
                agent.getAgentId(), agent.getAgentUrl()));
    }

    public static DispatchResult badRequest(AgentStatus agent, String response) {
        return new DispatchResult(false, agent, null,
            String.format("Agent %s returned 400 Bad Request: %s",
                agent.getAgentId(), response));
    }

    public static DispatchResult unexpectedHttpStatus(AgentStatus agent, int statusCode, String body) {
        return new DispatchResult(false, agent, null,
            String.format("Agent %s returned unexpected status code %d: %s",
                agent.getAgentId(), statusCode, body));
    }

    public static DispatchResult connectionFailed(AgentStatus agent, String error) {
        return new DispatchResult(false, agent, null,
            String.format("Connection failed to agent %s at %s: %s",
                agent.getAgentId(), agent.getAgentUrl(), error));
    }

    public static DispatchResult interrupted() {
        return new DispatchResult(false, null, null,
            "Dispatch was interrupted by another thread");
    }
}
