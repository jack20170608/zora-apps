package top.ilovemyhome.dagtask.si.agent;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores information about a registered agent in the scheduling center.
 * <p>
 * Contains both the original registration information and current runtime status.
 * This entity is persisted to the database for survival across server restarts.
 * </p>
 */
public class AgentInfo {

    private Long id;
    private String agentId;
    private String agentUrl;
    private int maxConcurrentTasks;
    private int maxPendingTasks;
    private List<String> supportedExecutionKeys;
    private Instant registeredAt;
    private Instant lastHeartbeatAt;
    private boolean running;
    private int pendingTasks;
    private int runningTasks;
    private int finishedTasks;

    /**
     * Field definitions for database column mapping.
     */
    public enum Field {
        id("id", true),
        agentId("agent_id"),
        agentUrl("agent_url"),
        maxConcurrentTasks("max_concurrent_tasks"),
        maxPendingTasks("max_pending_tasks"),
        supportedExecutionKeys("supported_execution_keys"),
        registeredAt("registered_at"),
        lastHeartbeatAt("last_heartbeat_at"),
        running("running"),
        pendingTasks("pending_tasks"),
        runningTasks("running_tasks"),
        finishedTasks("finished_tasks");

        private final String dbColumn;
        private final boolean isId;

        Field(String dbColumn) {
            this.dbColumn = dbColumn;
            this.isId = false;
        }

        Field(String dbColumn, boolean isId) {
            this.dbColumn = dbColumn;
            this.isId = isId;
        }

        public String getDbColumn() {
            return dbColumn;
        }

        public boolean isId() {
            return isId;
        }
    }

    public static final Map<String, String> FIELD_COLUMN_MAP
        = Collections.unmodifiableMap(Stream.of(Field.values())
        .collect(Collectors.toMap(Field::name, Field::getDbColumn)));

    public static final String ID_FIELD = Field.id.name();

    private AgentInfo(Long id, String agentId, String agentUrl, int maxConcurrentTasks, int maxPendingTasks, List<String> supportedExecutionKeys, Instant registeredAt, Instant lastHeartbeatAt, boolean running, int pendingTasks, int runningTasks, int finishedTasks) {
        this.id = id;
        this.agentId = agentId;
        this.agentUrl = agentUrl;
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.maxPendingTasks = maxPendingTasks;
        this.supportedExecutionKeys = supportedExecutionKeys;
        this.registeredAt = registeredAt;
        this.lastHeartbeatAt = lastHeartbeatAt;
        this.running = running;
        this.pendingTasks = pendingTasks;
        this.runningTasks = runningTasks;
        this.finishedTasks = finishedTasks;
    }

    /**
     * Creates an AgentInfo from a new registration.
     * Sets registration time to current time and initializes status with zero counts.
     *
     * @param registration the registration information from the agent
     * @return a new AgentInfo instance
     */
    public static AgentInfo fromRegistration(AgentRegistration registration) {
        Instant now = Instant.now();
        return builder()
            .withId(null)
            .withAgentId(registration.agentId())
            .withAgentUrl(registration.agentUrl())
            .withMaxConcurrentTasks(registration.maxConcurrentTasks())
            .withMaxPendingTasks(registration.maxPendingTasks())
            .withSupportedExecutionKeys(registration.supportedExecutionKeys())
            .withRegisteredAt(now)
            .withLastHeartbeatAt(now)
            .withRunning(true)
            .withPendingTasks(0)
            .withRunningTasks(0)
            .withFinishedTasks(0)
            .build();
    }

    /**
     * Creates a copy of this AgentInfo with updated status from a status report.
     *
     * @param statusReport the status report from the agent
     * @return a new AgentInfo instance with updated status
     */
    public AgentInfo withUpdatedStatus(AgentStatusReport statusReport) {
        return builder(this)
            .withLastHeartbeatAt(Instant.now())
            .withRunning(statusReport.running())
            .withPendingTasks(statusReport.pendingTasks())
            .withRunningTasks(statusReport.runningTasks())
            .withFinishedTasks(statusReport.finishedTasks())
            .build();
    }

    /**
     * Creates a copy of this AgentInfo marked as unregistered (not running).
     *
     * @return a new AgentInfo instance marked as not running
     */
    public AgentInfo withUnregistered() {
        return builder(this)
            .withLastHeartbeatAt(Instant.now())
            .withRunning(false)
            .build();
    }

    /**
     * Creates a new builder initialized with default values.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder initialized from an existing AgentInfo.
     *
     * @param info the existing AgentInfo to copy values from
     * @return a new builder initialized with the info's values
     */
    public static Builder builder(AgentInfo info) {
        return new Builder()
            .withId(info.getId())
            .withAgentId(info.getAgentId())
            .withAgentUrl(info.getAgentUrl())
            .withMaxConcurrentTasks(info.getMaxConcurrentTasks())
            .withMaxPendingTasks(info.getMaxPendingTasks())
            .withSupportedExecutionKeys(info.getSupportedExecutionKeys())
            .withRegisteredAt(info.getRegisteredAt())
            .withLastHeartbeatAt(info.getLastHeartbeatAt())
            .withRunning(info.isRunning())
            .withPendingTasks(info.getPendingTasks())
            .withRunningTasks(info.getRunningTasks())
            .withFinishedTasks(info.getFinishedTasks());
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentUrl() {
        return agentUrl;
    }

    public void setAgentUrl(String agentUrl) {
        this.agentUrl = agentUrl;
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    public int getMaxPendingTasks() {
        return maxPendingTasks;
    }

    public void setMaxPendingTasks(int maxPendingTasks) {
        this.maxPendingTasks = maxPendingTasks;
    }

    public List<String> getSupportedExecutionKeys() {
        return supportedExecutionKeys;
    }

    public void setSupportedExecutionKeys(List<String> supportedExecutionKeys) {
        this.supportedExecutionKeys = supportedExecutionKeys;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public int getPendingTasks() {
        return pendingTasks;
    }

    public void setPendingTasks(int pendingTasks) {
        this.pendingTasks = pendingTasks;
    }

    public int getRunningTasks() {
        return runningTasks;
    }

    public void setRunningTasks(int runningTasks) {
        this.runningTasks = runningTasks;
    }

    public int getFinishedTasks() {
        return finishedTasks;
    }

    public void setFinishedTasks(int finishedTasks) {
        this.finishedTasks = finishedTasks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentInfo agentInfo = (AgentInfo) o;
        return maxConcurrentTasks == agentInfo.maxConcurrentTasks
            && maxPendingTasks == agentInfo.maxPendingTasks
            && running == agentInfo.running
            && pendingTasks == agentInfo.pendingTasks
            && runningTasks == agentInfo.runningTasks
            && finishedTasks == agentInfo.finishedTasks
            && Objects.equals(id, agentInfo.id)
            && Objects.equals(agentId, agentInfo.agentId)
            && Objects.equals(agentUrl, agentInfo.agentUrl)
            && Objects.equals(supportedExecutionKeys, agentInfo.supportedExecutionKeys)
            && Objects.equals(registeredAt, agentInfo.registeredAt)
            && Objects.equals(lastHeartbeatAt, agentInfo.lastHeartbeatAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, agentId, agentUrl, maxConcurrentTasks, maxPendingTasks,
            supportedExecutionKeys, registeredAt, lastHeartbeatAt, running,
            pendingTasks, runningTasks, finishedTasks);
    }

    @Override
    public String toString() {
        return "AgentInfo{" +
            "id=" + id +
            ", agentId='" + agentId + '\'' +
            ", agentUrl='" + agentUrl + '\'' +
            ", maxConcurrentTasks=" + maxConcurrentTasks +
            ", maxPendingTasks=" + maxPendingTasks +
            ", supportedExecutionKeys=" + supportedExecutionKeys +
            ", registeredAt=" + registeredAt +
            ", lastHeartbeatAt=" + lastHeartbeatAt +
            ", running=" + running +
            ", pendingTasks=" + pendingTasks +
            ", runningTasks=" + runningTasks +
            ", finishedTasks=" + finishedTasks +
            '}';
    }

    /**
     * Builder for AgentInfo.
     */
    public static class Builder {
        private Long id;
        private String agentId;
        private String agentUrl;
        private int maxConcurrentTasks;
        private int maxPendingTasks;
        private List<String> supportedExecutionKeys;
        private Instant registeredAt;
        private Instant lastHeartbeatAt;
        private boolean running;
        private int pendingTasks;
        private int runningTasks;
        private int finishedTasks;

        private Builder() {
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withAgentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder withAgentUrl(String agentUrl) {
            this.agentUrl = agentUrl;
            return this;
        }

        public Builder withMaxConcurrentTasks(int maxConcurrentTasks) {
            this.maxConcurrentTasks = maxConcurrentTasks;
            return this;
        }

        public Builder withMaxPendingTasks(int maxPendingTasks) {
            this.maxPendingTasks = maxPendingTasks;
            return this;
        }

        public Builder withSupportedExecutionKeys(List<String> supportedExecutionKeys) {
            this.supportedExecutionKeys = supportedExecutionKeys;
            return this;
        }

        public Builder withRegisteredAt(Instant registeredAt) {
            this.registeredAt = registeredAt;
            return this;
        }

        public Builder withLastHeartbeatAt(Instant lastHeartbeatAt) {
            this.lastHeartbeatAt = lastHeartbeatAt;
            return this;
        }

        public Builder withRunning(boolean running) {
            this.running = running;
            return this;
        }

        public Builder withPendingTasks(int pendingTasks) {
            this.pendingTasks = pendingTasks;
            return this;
        }

        public Builder withRunningTasks(int runningTasks) {
            this.runningTasks = runningTasks;
            return this;
        }

        public Builder withFinishedTasks(int finishedTasks) {
            this.finishedTasks = finishedTasks;
            return this;
        }

        /**
         * Builds a new AgentInfo with the current builder values.
         *
         * @return the built AgentInfo instance
         */
        public AgentInfo build() {
            return new AgentInfo(id, agentId, agentUrl, maxConcurrentTasks, maxPendingTasks
                , supportedExecutionKeys, registeredAt, lastHeartbeatAt, running, pendingTasks
                , runningTasks, finishedTasks);
        }
    }
}
