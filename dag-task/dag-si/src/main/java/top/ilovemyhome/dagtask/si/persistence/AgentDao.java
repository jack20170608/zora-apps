package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.agent.Agent;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AgentDao extends BaseDao<Agent> {

    Optional<Agent> findByAgentId(String agentId);

    List<Agent> findByStatus(Agent.Status status);

    void updateStatus(String agentId, Agent.Status status);

    void updateHeartbeat(String agentId, Instant heartbeatAt);

    boolean exists(String agentId);
}
