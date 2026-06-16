package top.ilovemyhome.dagtask.scheduler.application;

import top.ilovemyhome.dagtask.scheduler.port.out.AgentStatusRepository;
import top.ilovemyhome.dagtask.si.agent.AgentStatusReport;

import java.util.Objects;

/**
 * Application service for processing periodic agent heartbeat / status reports.
 * <p>
 * Replaces the legacy {@code AgentRegistryService.reportAgentStatus}.
 * The in-memory cache from the legacy implementation is intentionally omitted
 * (TD-3); this service always reads from the repository. If performance
 * degrades, a caching port can be added in step 3.
 * </p>
 */
public class AgentHeartbeatService implements top.ilovemyhome.dagtask.scheduler.port.in.AgentHeartbeatUseCase {

    private final AgentStatusRepository agentStatusRepository;

    public AgentHeartbeatService(AgentStatusRepository agentStatusRepository) {
        this.agentStatusRepository = Objects.requireNonNull(agentStatusRepository, "agentStatusRepository must not be null");
    }

    @Override
    public boolean reportAgentStatus(AgentStatusReport statusReport) {
        if (statusReport == null || statusReport.agentId() == null || statusReport.agentId().isBlank()) {
            return false;
        }

        if (!agentStatusRepository.exists(statusReport.agentId())) {
            return false;
        }

        agentStatusRepository.updateStatus(
            statusReport.agentId(),
            statusReport.running(),
            statusReport.pendingTasks(),
            statusReport.runningTasks(),
            statusReport.finishedTasks()
        );

        return true;
    }
}
