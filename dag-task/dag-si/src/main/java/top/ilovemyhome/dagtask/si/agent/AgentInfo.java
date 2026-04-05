package top.ilovemyhome.dagtask.si.agent;

import java.time.Instant;
import java.util.List;

/**
 * Immutable record that stores information about a registered agent in the scheduling center.
 * Contains both the original registration information and current runtime status.
 *
 * @param agentId unique identifier for this agent instance
 * @param agentUrl the URL where this agent can be reached from the server
 * @param maxConcurrentTasks maximum number of concurrent tasks this agent can execute
 * @param maxPendingTasks maximum number of pending tasks this agent can hold in queue
 * @param supportedExecutionKeys list of execution keys (task types) that this agent supports
 * @param registeredAt timestamp when this agent was registered
 * @param lastHeartbeatAt timestamp of the last status report (heartbeat)
 * @param running whether the agent is currently believed to be running
 * @param pendingTasks current number of pending tasks waiting in queue
 * @param runningTasks current number of running tasks
 * @param finishedTasks total number of finished tasks since registration
 */
public record AgentInfo(
    String agentId,
    String agentUrl,
    int maxConcurrentTasks,
    int maxPendingTasks,
    List<String> supportedExecutionKeys,
    Instant registeredAt,
    Instant lastHeartbeatAt,
    boolean running,
    int pendingTasks,
    int runningTasks,
    int finishedTasks
) {

    /**
     * Creates an AgentInfo from a new registration.
     * Sets registration time to current time and initializes status with zero counts.
     *
     * @param registration the registration information from the agent
     * @return a new AgentInfo instance
     */
    public static AgentInfo fromRegistration(AgentRegistration registration) {
        Instant now = Instant.now();
        return new AgentInfo(
            registration.agentId(),
            registration.agentUrl(),
            registration.maxConcurrentTasks(),
            registration.maxPendingTasks(),
            registration.supportedExecutionKeys(),
            now,
            now,
            true,
            0,
            0,
            0
        );
    }

    /**
     * Creates a copy of this AgentInfo with updated status from a status report.
     *
     * @param statusReport the status report from the agent
     * @return a new AgentInfo instance with updated status
     */
    public AgentInfo withUpdatedStatus(AgentStatusReport statusReport) {
        return new AgentInfo(
            this.agentId(),
            this.agentUrl(),
            this.maxConcurrentTasks(),
            this.maxPendingTasks(),
            this.supportedExecutionKeys(),
            this.registeredAt(),
            Instant.now(),
            statusReport.running(),
            statusReport.pendingTasks(),
            statusReport.runningTasks(),
            statusReport.finishedTasks()
        );
    }

    /**
     * Creates a copy of this AgentInfo marked as unregistered (not running).
     *
     * @return a new AgentInfo instance marked as not running
     */
    public AgentInfo withUnregistered() {
        return new AgentInfo(
            this.agentId(),
            this.agentUrl(),
            this.maxConcurrentTasks(),
            this.maxPendingTasks(),
            this.supportedExecutionKeys(),
            this.registeredAt(),
            Instant.now(),
            false,
            this.pendingTasks(),
            this.runningTasks(),
            this.finishedTasks()
        );
    }
}
