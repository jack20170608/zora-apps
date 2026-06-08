package top.ilovemyhome.dagtask.scheduler.port.out;

import top.ilovemyhome.dagtask.si.agent.Agent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for {@link Agent} persistence. Implementations live in
 * dag-scheduler-adapter-persistence-jdbc (or any other persistence adapter).
 */
public interface AgentRepository {

    /** Create a new agent and return its generated ID. */
    Long create(Agent agent);

    Optional<Agent> findByAgentId(String agentId);

    List<Agent> findByStatus(Agent.Status status);

    void updateStatus(String agentId, Agent.Status status);

    void updateHeartbeat(String agentId, Instant heartbeatAt);

    boolean exists(String agentId);
}
