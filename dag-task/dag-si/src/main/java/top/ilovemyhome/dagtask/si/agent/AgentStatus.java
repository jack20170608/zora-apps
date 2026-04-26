package top.ilovemyhome.dagtask.si.agent;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the runtime status of a registered agent.
 * Static identity is stored separately in {@link Agent}.
 */
public class AgentStatus {

    private Long id;
    private String agentId;
    private String agentUrl;
    private int maxConcurrentTasks;
    private int maxPendingTasks;
    private String supportedExecutionKeys;
    private boolean running;
    private int pendingTasks;
    private int runningTasks;
    private int finishedTasks;
    private Instant lastHeartbeatAt;

    public enum Field {
        id("id", true),
        agentId("agent_id"),
        agentUrl("agent_url"),
        maxConcurrentTasks("max_concurrent_tasks"),
        maxPendingTasks("max_pending_tasks"),
        supportedExecutionKeys("supported_execution_keys"),
        running("running"),
        pendingTasks("pending_tasks"),
        runningTasks("running_tasks"),
        finishedTasks("finished_tasks"),
        lastHeartbeatAt("last_heartbeat_at");

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

    private AgentStatus(Long id, String agentId, String agentUrl, int maxConcurrentTasks,
                        int maxPendingTasks, String supportedExecutionKeys, boolean running,
                        int pendingTasks, int runningTasks, int finishedTasks,
                        Instant lastHeartbeatAt) {
        this.id = id;
        this.agentId = agentId;
        this.agentUrl = agentUrl;
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.maxPendingTasks = maxPendingTasks;
        this.supportedExecutionKeys = supportedExecutionKeys;
        this.running = running;
        this.pendingTasks = pendingTasks;
        this.runningTasks = runningTasks;
        this.finishedTasks = finishedTasks;
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AgentStatus status) {
        return new Builder()
            .withId(status.getId())
            .withAgentId(status.getAgentId())
            .withAgentUrl(status.getAgentUrl())
            .withMaxConcurrentTasks(status.getMaxConcurrentTasks())
            .withMaxPendingTasks(status.getMaxPendingTasks())
            .withSupportedExecutionKeys(status.getSupportedExecutionKeys())
            .withRunning(status.isRunning())
            .withPendingTasks(status.getPendingTasks())
            .withRunningTasks(status.getRunningTasks())
            .withFinishedTasks(status.getFinishedTasks())
            .withLastHeartbeatAt(status.getLastHeartbeatAt());
    }

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

    public String getSupportedExecutionKeys() {
        return supportedExecutionKeys;
    }

    public void setSupportedExecutionKeys(String supportedExecutionKeys) {
        this.supportedExecutionKeys = supportedExecutionKeys;
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

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentStatus that = (AgentStatus) o;
        return maxConcurrentTasks == that.maxConcurrentTasks
            && maxPendingTasks == that.maxPendingTasks
            && running == that.running
            && pendingTasks == that.pendingTasks
            && runningTasks == that.runningTasks
            && finishedTasks == that.finishedTasks
            && Objects.equals(id, that.id)
            && Objects.equals(agentId, that.agentId)
            && Objects.equals(agentUrl, that.agentUrl)
            && Objects.equals(supportedExecutionKeys, that.supportedExecutionKeys)
            && Objects.equals(lastHeartbeatAt, that.lastHeartbeatAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, agentId, agentUrl, maxConcurrentTasks, maxPendingTasks,
            supportedExecutionKeys, running, pendingTasks, runningTasks, finishedTasks,
            lastHeartbeatAt);
    }

    @Override
    public String toString() {
        return "AgentStatus{" +
            "id=" + id +
            ", agentId='" + agentId + '\'' +
            ", running=" + running +
            ", pendingTasks=" + pendingTasks +
            ", runningTasks=" + runningTasks +
            ", finishedTasks=" + finishedTasks +
            '}';
    }

    public static class Builder {
        private Long id;
        private String agentId;
        private String agentUrl;
        private int maxConcurrentTasks;
        private int maxPendingTasks;
        private String supportedExecutionKeys;
        private boolean running;
        private int pendingTasks;
        private int runningTasks;
        private int finishedTasks;
        private Instant lastHeartbeatAt;

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

        public Builder withSupportedExecutionKeys(String supportedExecutionKeys) {
            this.supportedExecutionKeys = supportedExecutionKeys;
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

        public Builder withLastHeartbeatAt(Instant lastHeartbeatAt) {
            this.lastHeartbeatAt = lastHeartbeatAt;
            return this;
        }

        public AgentStatus build() {
            return new AgentStatus(id, agentId, agentUrl, maxConcurrentTasks, maxPendingTasks,
                supportedExecutionKeys, running, pendingTasks, runningTasks, finishedTasks,
                lastHeartbeatAt);
        }
    }
}
