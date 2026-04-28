package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.agent.AgentWhitelist;
import top.ilovemyhome.dagtask.si.dto.AgentWhitelistSearchCriteria;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.util.List;

public interface AgentWhitelistDao extends BaseDao<AgentWhitelist> {

    /**
     * Search whitelist entries using dynamic search criteria.
     *
     * @param criteria the search criteria to use
     * @return list of whitelist entries matching the search criteria
     */
    List<AgentWhitelist> search(AgentWhitelistSearchCriteria criteria);

    /**
     * Find all enabled whitelist entries.
     *
     * @return list of enabled whitelist entries
     */
    List<AgentWhitelist> findAllEnabled();

    /**
     * Check if the given agentId is allowed by any enabled whitelist entry.
     * Uses exact SQL match for efficiency.
     *
     * @param agentId the agent identifier
     * @return true if allowed by whitelist, false otherwise
     */
    boolean existsByAgentId(String agentId);

    /**
     * Find enabled IP segments related to the given agentId.
     * Includes entries specifically for this agent and generic entries (agent_id is null).
     * Used for CIDR matching in Java layer.
     *
     * @param agentId the agent identifier
     * @return list of non-null ip_segment values
     */
    List<String> findIpSegmentsByAgentId(String agentId);
}
