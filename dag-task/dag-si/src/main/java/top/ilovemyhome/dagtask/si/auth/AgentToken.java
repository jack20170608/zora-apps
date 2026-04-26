package top.ilovemyhome.dagtask.si.auth;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an agent authentication token credential.
 * Each token is bound to exactly one agent via {@code agentId}.
 */
public class AgentToken {

    private Long id;
    private String tokenId;
    private String agentId;
    private String name;
    private String description;
    private String createdBy;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean revoked;
    private Instant revokedAt;
    private String revokedBy;

    public enum Field {
        id("id", true),
        tokenId("token_id"),
        agentId("agent_id"),
        name("name"),
        description("description"),
        createdBy("created_by"),
        createdAt("created_at"),
        expiresAt("expires_at"),
        revoked("revoked"),
        revokedAt("revoked_at"),
        revokedBy("revoked_by");

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

    private AgentToken(Long id, String tokenId, String agentId, String name, String description,
                       String createdBy, Instant createdAt, Instant expiresAt,
                       boolean revoked, Instant revokedAt, String revokedBy) {
        this.id = id;
        this.tokenId = tokenId;
        this.agentId = agentId;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
        this.revokedBy = revokedBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AgentToken token) {
        return new Builder()
            .withId(token.getId())
            .withTokenId(token.getTokenId())
            .withAgentId(token.getAgentId())
            .withName(token.getName())
            .withDescription(token.getDescription())
            .withCreatedBy(token.getCreatedBy())
            .withCreatedAt(token.getCreatedAt())
            .withExpiresAt(token.getExpiresAt())
            .withRevoked(token.isRevoked())
            .withRevokedAt(token.getRevokedAt())
            .withRevokedBy(token.getRevokedBy());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentToken that = (AgentToken) o;
        return revoked == that.revoked
            && Objects.equals(id, that.id)
            && Objects.equals(tokenId, that.tokenId)
            && Objects.equals(agentId, that.agentId)
            && Objects.equals(name, that.name)
            && Objects.equals(description, that.description)
            && Objects.equals(createdBy, that.createdBy)
            && Objects.equals(createdAt, that.createdAt)
            && Objects.equals(expiresAt, that.expiresAt)
            && Objects.equals(revokedAt, that.revokedAt)
            && Objects.equals(revokedBy, that.revokedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tokenId, agentId, name, description, createdBy,
            createdAt, expiresAt, revoked, revokedAt, revokedBy);
    }

    @Override
    public String toString() {
        return "AgentToken{" +
            "id=" + id +
            ", tokenId='" + tokenId + '\'' +
            ", agentId='" + agentId + '\'' +
            ", name='" + name + '\'' +
            ", revoked=" + revoked +
            ", expiresAt=" + expiresAt +
            '}';
    }

    public static class Builder {
        private Long id;
        private String tokenId;
        private String agentId;
        private String name;
        private String description;
        private String createdBy;
        private Instant createdAt;
        private Instant expiresAt;
        private boolean revoked;
        private Instant revokedAt;
        private String revokedBy;

        private Builder() {
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withTokenId(String tokenId) {
            this.tokenId = tokenId;
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

        public Builder withCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder withCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder withExpiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder withRevoked(boolean revoked) {
            this.revoked = revoked;
            return this;
        }

        public Builder withRevokedAt(Instant revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }

        public Builder withRevokedBy(String revokedBy) {
            this.revokedBy = revokedBy;
            return this;
        }

        public AgentToken build() {
            return new AgentToken(id, tokenId, agentId, name, description, createdBy,
                createdAt, expiresAt, revoked, revokedAt, revokedBy);
        }
    }
}
