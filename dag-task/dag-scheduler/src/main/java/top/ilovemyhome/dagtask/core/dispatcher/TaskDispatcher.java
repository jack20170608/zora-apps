package top.ilovemyhome.dagtask.core.dispatcher;

import top.ilovemyhome.dagtask.si.DispatchResult;
import top.ilovemyhome.dagtask.si.TaskDispatchRecord;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;

import java.util.List;
import java.util.Optional;

/**
 * Dispatcher interface that selects an appropriate agent and dispatches ready tasks
 * for execution on that agent.
 */
public interface TaskDispatcher {

    /**
     * Dispatches a ready task to an appropriate agent for execution.
     *
     * @param task the ready task to dispatch
     * @return the dispatch result indicating success or failure with details
     */
    DispatchResult dispatch(TaskRecord task);

    /**
     * Requests a task to be killed on its executing agent.
     * This overload automatically looks up the agent from the dispatch tracking table.
     *
     * @param taskId the task ID to kill
     * @param dealer the user who performed the operation
     * @param reason the reason for killing the task
     * @return true if the kill request was accepted by the agent
     */
    boolean killTask(Long taskId, String dealer, String reason);

    /**
     * Requests a task to be killed on its executing agent.
     *
     * @param dispatchItem the dispatch record containing task and agent information
     * @param dealer the user who performed the operation
     * @param reason the reason for killing the task
     * @return true if the kill request was accepted by the agent
     */
    boolean killTask(TaskDispatchRecord dispatchItem, String dealer, String reason);

    /**
     * Requests a task to be forcefully marked as successful on its executing agent.
     * This overload automatically looks up the agent from the dispatch tracking table.
     *
     * @param taskId the task ID to force ok
     * @param dealer the user who performed the operation
     * @param reason the reason for forcefully marking the task as ok
     * @return true if the force-ok request was accepted by the agent
     */
    boolean forceOkTask(Long taskId, String dealer, String reason);

    /**
     * Requests a task to be forcefully marked as successful on its executing agent.
     *
     * @param dispatchItem the dispatch record containing task and agent information
     * @param dealer the user who performed the operation
     * @param reason the reason for forcefully marking the task as ok
     * @return true if the force-ok request was accepted by the agent
     */
    boolean forceOkTask(TaskDispatchRecord dispatchItem, String dealer, String reason);

    /**
     * Gets the current number of active agents that support a given execution key.
     * Useful for monitoring and diagnostics.
     *
     * @param executionKey the execution key to count
     * @return number of available agents for this execution key
     */
    int countAvailableAgents(String executionKey);

    /**
     * Gets all available candidate agents for a given execution key.
     *
     * @param executionKey the execution key
     * @return list of available candidate agents (active + supports execution key + has capacity)
     */
    List<AgentStatus> getAvailableCandidates(String executionKey);

    /**
     * Finds all currently active (running) agents from the registry.
     * Active agents are those marked as running in the database.
     *
     * @return list of all active agents
     */
    List<AgentStatus> findAllActiveAgents();

    /**
     * Finds an agent by its unique agent ID.
     *
     * @param agentId the unique agent identifier to search for
     * @return an Optional containing the agent if found, empty otherwise
     */
    Optional<AgentStatus> findAgentByAgentId(String agentId);
}
