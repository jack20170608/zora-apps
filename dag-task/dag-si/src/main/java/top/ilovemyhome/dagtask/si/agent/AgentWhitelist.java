package top.ilovemyhome.dagtask.si.agent;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an agent whitelist entry for controlling which agents
 * are allowed to register based on IP segment or agentId.
 */
public class AgentWhitelist {

    private Long id;
    private String ipSegment;
    private String agentId;
    private String description;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public enum Field {
        id("id", true),
        ipSegment("ip_segment"),
        agentId("agent_id"),
        description("description"),
        enabled("enabled"),
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

    private AgentWhitelist(Long id, String ipSegment, String agentId,
                           String description, boolean enabled,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ipSegment = ipSegment;
        this.agentId = agentId;
        this.description = description;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AgentWhitelist whitelist) {
        return new Builder()
            .withId(whitelist.getId())
            .withIpSegment(whitelist.getIpSegment())
            .withAgentId(whitelist.getAgentId())
            .withDescription(whitelist.getDescription())
            .withEnabled(whitelist.isEnabled())
            .withCreatedAt(whitelist.getCreatedAt())
            .withUpdatedAt(whitelist.getUpdatedAt());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIpSegment() {
        return ipSegment;
    }

    public void setIpSegment(String ipSegment) {
        this.ipSegment = ipSegment;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
        AgentWhitelist that = (AgentWhitelist) o;
        return enabled == that.enabled
            && Objects.equals(id, that.id)
            && Objects.equals(ipSegment, that.ipSegment)
            && Objects.equals(agentId, that.agentId)
            && Objects.equals(description, that.description)
            && Objects.equals(createdAt, that.createdAt)
            && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ipSegment, agentId, description, enabled, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "AgentWhitelist{" +
            "id=" + id +
            ", ipSegment='" + ipSegment + '\'' +
            ", agentId='" + agentId + '\'' +
            ", enabled=" + enabled +
            '}';
    }

    public static class Builder {
        private Long id;
        private String ipSegment;
        private String agentId;
        private String description;
        private boolean enabled = true;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withIpSegment(String ipSegment) {
            this.ipSegment = ipSegment;
            return this;
        }

        public Builder withAgentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
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

        public AgentWhitelist build() {
            return new AgentWhitelist(id, ipSegment, agentId, description,
                enabled, createdAt, updatedAt);
        }
    }
}
