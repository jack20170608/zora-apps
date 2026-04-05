package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.agent.AgentInfo;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for persisting and retrieving agent registry information to/from database.
 * This interface extends {@link BaseDao} with AgentInfo specific operations.
 *
 * Implementations handle the actual database storage of registered agent information,
 * allowing the registry to survive server restarts.
 */
public interface AgentRegistryDao extends BaseDao<AgentInfo> {

    /**
     * Find an agent by its agent ID.
     *
     * @param agentId the unique identifier of the agent
     * @return the agent information if found, empty otherwise
     */
    Optional<AgentInfo> findByAgentId(String agentId);

    /**
     * List all agents that are currently marked as running (active).
     *
     * @return a list of all active agents
     */
    List<AgentInfo> findAllActive();

    /**
     * List all agents regardless of their running status.
     *
     * @return a list of all agents in the registry
     */
    List<AgentInfo> findAll();

    /**
     * Delete an agent from the registry by agent ID.
     *
     * @param agentId the unique identifier of the agent to delete
     * @return the number of rows deleted (0 or 1)
     */
    int deleteByAgentId(String agentId);

    /**
     * Update the heartbeat timestamp and status for an agent.
     *
     * @param agentId the agent identifier
     * @param running whether the agent is running
     * @param pendingTasks current number of pending tasks
     * @param runningTasks current number of running tasks
     * @param finishedTasks total number of finished tasks
     * @return the number of rows updated (0 or 1)
     */
    int updateStatus(String agentId, boolean running, int pendingTasks, int runningTasks, int finishedTasks);

    /**
     * Mark an agent as unregistered (not running).
     *
     * @param agentId the agent identifier
     * @return the number of rows updated (0 or 1)
     */
    int markUnregistered(String agentId);

    /**
     * Check if an agent with the given ID already exists.
     *
     * @param agentId the agent identifier to check
     * @return true if the agent exists, false otherwise
     */
    boolean exists(String agentId);
}
