package top.ilovemyhome.dagtask.scheduler.port.out;

import top.ilovemyhome.dagtask.si.agent.AgentStatus;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for {@link AgentStatus} persistence. Implementations live in
 * dag-scheduler-adapter-persistence-jdbc (or any other persistence adapter).
 */
public interface AgentStatusRepository {

    /** Create a new agent status record and return its generated ID. */
    Long create(AgentStatus agentStatus);

    Optional<AgentStatus> findByAgentId(String agentId);

    List<AgentStatus> findAllActive();

    int updateStatus(String agentId, boolean running, int pendingTasks, int runningTasks, int finishedTasks);

    int markUnregistered(String agentId);

    boolean exists(String agentId);
}
