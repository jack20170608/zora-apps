package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.agent.AgentRegistryItem;
import top.ilovemyhome.dagtask.si.dto.AgentRegistrySearchCriteria;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.util.List;

/**
 * DAO interface for persisting and retrieving agent registry information to/from database.
 * This interface extends {@link BaseDao} with AgentInfo specific operations.
 *
 * Implementations handle the actual database storage of registered agent information,
 * allowing the registry to survive server restarts.
 */
public interface AgentRegistryDao extends BaseDao<AgentRegistryItem> {

    List<AgentRegistryItem> search(AgentRegistrySearchCriteria criteria);

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

    /**
     * Find an agent by token ID.
     *
     * @param tokenId the unique token identifier
     * @return the agent registry item if found, empty otherwise
     */
    java.util.Optional<AgentRegistryItem> findByTokenId(String tokenId);

    /**
     * Revoke a token by token ID.
     *
     * @param tokenId the unique token identifier
     * @param revokedBy the administrator who revoked this token
     */
    void revokeToken(String tokenId, String revokedBy);
}
