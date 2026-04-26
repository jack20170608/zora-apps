package top.ilovemyhome.dagtask.si.agent;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a registered agent's static identity information.
 * Runtime status is stored separately in {@link AgentStatus}.
 */
public class Agent {

    private Long id;
    private String agentId;
    private String name;
    private String description;
    private String labelsJson;
    private Status status;
    private Instant registeredAt;
    private Instant lastHeartbeatAt;
    private Instant createdAt;
    private Instant updatedAt;

    public enum Status {
        PENDING, ACTIVE, INACTIVE
    }

    public enum Field {
        id("id", true),
        agentId("agent_id"),
        name("name"),
        description("description"),
        labelsJson("labels"),
        status("status"),
        registeredAt("registered_at"),
        lastHeartbeatAt("last_heartbeat_at"),
        createdAt("created_at"),
        updatedAt("updated_at");

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

    private Agent(Long id, String agentId, String name, String description, String labelsJson,
                  Status status, Instant registeredAt, Instant lastHeartbeatAt,
                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.agentId = agentId;
        this.name = name;
        this.description = description;
        this.labelsJson = labelsJson;
        this.status = status;
        this.registeredAt = registeredAt;
        this.lastHeartbeatAt = lastHeartbeatAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Agent agent) {
        return new Builder()
            .withId(agent.getId())
            .withAgentId(agent.getAgentId())
            .withName(agent.getName())
            .withDescription(agent.getDescription())
            .withLabelsJson(agent.getLabelsJson())
            .withStatus(agent.getStatus())
            .withRegisteredAt(agent.getRegisteredAt())
            .withLastHeartbeatAt(agent.getLastHeartbeatAt())
            .withCreatedAt(agent.getCreatedAt())
            .withUpdatedAt(agent.getUpdatedAt());
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLabelsJson() {
        return labelsJson;
    }

    public void setLabelsJson(String labelsJson) {
        this.labelsJson = labelsJson;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Agent agent = (Agent) o;
        return Objects.equals(id, agent.id)
            && Objects.equals(agentId, agent.agentId)
            && Objects.equals(name, agent.name)
            && Objects.equals(description, agent.description)
            && Objects.equals(labelsJson, agent.labelsJson)
            && status == agent.status
            && Objects.equals(registeredAt, agent.registeredAt)
            && Objects.equals(lastHeartbeatAt, agent.lastHeartbeatAt)
            && Objects.equals(createdAt, agent.createdAt)
            && Objects.equals(updatedAt, agent.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, agentId, name, description, labelsJson, status,
            registeredAt, lastHeartbeatAt, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Agent{" +
            "id=" + id +
            ", agentId='" + agentId + '\'' +
            ", name='" + name + '\'' +
            ", status=" + status +
            ", registeredAt=" + registeredAt +
            '}';
    }

    public static class Builder {
        private Long id;
        private String agentId;
        private String name;
        private String description;
        private String labelsJson;
        private Status status;
        private Instant registeredAt;
        private Instant lastHeartbeatAt;
        private Instant createdAt;
        private Instant updatedAt;

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

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withLabelsJson(String labelsJson) {
            this.labelsJson = labelsJson;
            return this;
        }

        public Builder withStatus(Status status) {
            this.status = status;
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

        public Builder withCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder withUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Agent build() {
            return new Agent(id, agentId, name, description, labelsJson, status,
                registeredAt, lastHeartbeatAt, createdAt, updatedAt);
        }
    }
}
