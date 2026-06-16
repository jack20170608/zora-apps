package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.si.agent.AgentStatusReport;

/**
 * Receive periodic heartbeats / status reports from registered agents.
 * <p>
 * Wraps the legacy {@code AgentRegistryService.reportAgentStatus} method.
 * </p>
 */
public interface AgentHeartbeatUseCase {

    /**
     * Update the status of an agent. Called periodically by agents to report current
     * queue statistics and health status.
     *
     * @param statusReport the agent status report with current queue metrics
     * @return {@code true} if status was updated successfully, {@code false} if agent not found
     */
    boolean reportAgentStatus(AgentStatusReport statusReport);
}
