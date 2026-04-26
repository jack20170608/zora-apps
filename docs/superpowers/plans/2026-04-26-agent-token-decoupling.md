# Agent-Token Decoupling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decouple agent identity, token credentials, and runtime status from `t_agent_registry` into independent tables (`t_agents`, `t_agent_tokens`, `t_agent_status`) with matching DAOs, then refactor `TokenService` and APIs to use the new model.

**Architecture:** Create three new tables via Flyway V2 migration. Introduce `AgentDao`, `AgentTokenDao`, `AgentStatusDao` following the existing JDBI `BaseDaoJdbiImpl` pattern (class + Builder + Field enum + FIELD_COLUMN_MAP). Refactor `TokenService` to require `agentId` on token generation and query `t_agent_tokens` directly. Update registration and management APIs and DI wiring.

**Tech Stack:** Java 25, Maven, JUnit 5, Mockito, JDBI v3, Flyway, PostgreSQL, EmbeddedPostgres (integration tests)

---

## File Structure

### New Files

| File | Module | Responsibility |
|------|--------|----------------|
| `V2__agent_token_decoupling.sql` | dag-si | Flyway migration: create new tables, migrate data |
| `Agent.java` | dag-si | Agent identity entity |
| `AgentStatus.java` | dag-si | Agent runtime status entity |
| `AgentToken.java` | dag-si | Token credential entity |
| `AgentDao.java` | dag-si | Agent DAO interface |
| `AgentTokenDao.java` | dag-si | Token DAO interface |
| `AgentStatusDao.java` | dag-si | Agent status DAO interface |
| `AgentDaoJdbiImpl.java` | dag-scheduler | Agent DAO JDBI implementation |
| `AgentTokenDaoJdbiImpl.java` | dag-scheduler | Token DAO JDBI implementation |
| `AgentStatusDaoJdbiImpl.java` | dag-scheduler | Agent status DAO JDBI implementation |
| `TokenServiceTest.java` | dag-scheduler | TokenService unit tests |

### Modified Files

| File | Module | Change |
|------|--------|--------|
| `GenerateTokenRequest.java` | dag-si | Add `agentId` field |
| `TokenService.java` | dag-scheduler | Use `AgentTokenDao` + `AgentDao`; add `agentId` validation |
| `TokenManagementApi.java` | dag-scheduler | Pass `agentId` to `TokenService` |
| `PublicRegistrationApi.java` | dag-scheduler | Create `Agent` record before generating token |
| `DagSchedulerBuilder.java` | dag-scheduler | Register new DAOs |
| `WebServerBootstrap.java` | dag-scheduler-muserver | Wire new DAOs into services |

---

## Task 1: Flyway Migration V2

**Files:**
- Create: `dag-task/dag-si/src/main/resources/db/migration/postgresql/V2__agent_token_decoupling.sql`

- [ ] **Step 1: Write V2 migration script**

```sql
-- ============================================================================
-- Flyway Migration: V2
-- Description: Decouple agent identity, token, and runtime status
-- ============================================================================

-- 1. Agent identity table
CREATE TABLE t_agents (
    id BIGSERIAL PRIMARY KEY,
    agent_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    labels JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Token credentials table (independent, bound to agent)
CREATE TABLE t_agent_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(64) NOT NULL UNIQUE,
    agent_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by VARCHAR(100),
    CONSTRAINT fk_agent_tokens_agent_id FOREIGN KEY (agent_id)
        REFERENCES t_agents(agent_id) ON DELETE CASCADE
);

-- 3. Agent runtime status table
CREATE TABLE t_agent_status (
    id BIGSERIAL PRIMARY KEY,
    agent_id VARCHAR(255) NOT NULL UNIQUE,
    agent_url VARCHAR(1024) NOT NULL,
    max_concurrent_tasks INTEGER NOT NULL,
    max_pending_tasks INTEGER NOT NULL,
    supported_execution_keys TEXT NOT NULL,
    running BOOLEAN NOT NULL DEFAULT TRUE,
    pending_tasks INTEGER NOT NULL DEFAULT 0,
    running_tasks INTEGER NOT NULL DEFAULT 0,
    finished_tasks INTEGER NOT NULL DEFAULT 0,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_agent_status_agent_id FOREIGN KEY (agent_id)
        REFERENCES t_agents(agent_id) ON DELETE CASCADE
);

-- 4. Migrate data from t_agent_registry
INSERT INTO t_agents (agent_id, name, status, registered_at, last_heartbeat_at, created_at)
SELECT
    agent_id,
    COALESCE(token_name, agent_id) AS name,
    CASE WHEN running THEN 'ACTIVE' ELSE 'INACTIVE' END AS status,
    registered_at,
    last_heartbeat_at,
    registered_at AS created_at
FROM t_agent_registry;

INSERT INTO t_agent_tokens (token_id, agent_id, name, description, created_by, created_at, expires_at, revoked, revoked_at, revoked_by)
SELECT
    token_id,
    agent_id,
    token_name,
    description,
    created_by,
    created_at,
    expires_at,
    revoked,
    revoked_at,
    revoked_by
FROM t_agent_registry
WHERE token_id IS NOT NULL AND token_id <> '';

INSERT INTO t_agent_status (agent_id, agent_url, max_concurrent_tasks, max_pending_tasks, supported_execution_keys, running, pending_tasks, running_tasks, finished_tasks, last_heartbeat_at)
SELECT
    agent_id,
    agent_url,
    max_concurrent_tasks,
    max_pending_tasks,
    supported_execution_keys,
    running,
    pending_tasks,
    running_tasks,
    finished_tasks,
    last_heartbeat_at
FROM t_agent_registry;

-- 5. Create indexes
CREATE INDEX idx_t_agents_status ON t_agents(status);
CREATE INDEX idx_t_agents_labels ON t_agents USING GIN(labels);
CREATE INDEX idx_agent_tokens_agent_id ON t_agent_tokens(agent_id);
CREATE INDEX idx_agent_tokens_token_id ON t_agent_tokens(token_id);
CREATE INDEX idx_agent_tokens_revoked ON t_agent_tokens(revoked);
CREATE INDEX idx_agent_status_running ON t_agent_status(running);
```

- [ ] **Step 2: Run Flyway migration test**

Run: `cd dag-task/dag-scheduler-muserver && mvn test -Dtest=FlywayMigrationTest`

Expected: Test passes. If you have an IDE or local Postgres, you can also verify manually that `t_agents`, `t_agent_tokens`, `t_agent_status` are created and populated with data migrated from `t_agent_registry`.

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-si/src/main/resources/db/migration/postgresql/V2__agent_token_decoupling.sql
git commit -m "feat: add Flyway V2 migration for agent-token decoupling"
```

---

## Task 2: Agent Entity

**Files:**
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/agent/Agent.java`

- [ ] **Step 1: Create Agent entity class**

```java
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
```

---

## Task 3: AgentToken Entity

**Files:**
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/auth/AgentToken.java`

- [ ] **Step 1: Create AgentToken entity class**

```java
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

    // Getters and Setters

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
```

---

## Task 4: AgentStatus Entity

**Files:**
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/agent/AgentStatus.java`

- [ ] **Step 1: Create AgentStatus entity class**

```java
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
```

---

## Task 5: Compile Entities

**Files:**
- All three entity files created in Tasks 2-4

- [ ] **Step 1: Compile dag-si module**

Run: `cd dag-task/dag-si && mvn compile`

Expected: Compiles successfully with no errors.

- [ ] **Step 2: Commit**

```bash
git add dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/agent/Agent.java
git add dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/agent/AgentStatus.java
git add dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/auth/AgentToken.java
git commit -m "feat: add Agent, AgentToken, AgentStatus entities"
```

---

## Task 6: Agent DAO

**Files:**
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/persistence/AgentDao.java`
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dao/AgentDaoJdbiImpl.java`

- [ ] **Step 1: Create AgentDao interface**

```java
package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.agent.Agent;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AgentDao extends BaseDao<Agent> {

    Optional<Agent> findByAgentId(String agentId);

    List<Agent> findByStatus(Agent.Status status);

    void updateStatus(String agentId, Agent.Status status);

    void updateHeartbeat(String agentId, Instant heartbeatAt);

    boolean exists(String agentId);
}
```

- [ ] **Step 2: Create AgentDaoJdbiImpl**

```java
package top.ilovemyhome.dagtask.core.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.dagtask.si.agent.Agent;
import top.ilovemyhome.dagtask.si.persistence.AgentDao;
import top.ilovemyhome.zora.jdbi.SqlGenerator;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AgentDaoJdbiImpl extends BaseDaoJdbiImpl<Agent> implements AgentDao {

    public AgentDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName("t_agents")
            .withIdField("id")
            .withIdAutoGenerate(true)
            .withFieldColumnMap(Agent.FIELD_COLUMN_MAP)
            .build(), jdbi);
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(Agent.class, new AgentRowMapper());
    }

    @Override
    public Optional<Agent> findByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format("select * from %s where agent_id = :agentId", table.getName());
        return find(sql, Map.of("agentId", agentId), null).stream().findAny();
    }

    @Override
    public List<Agent> findByStatus(Agent.Status status) {
        Objects.requireNonNull(status, "status must not be null");
        String sql = String.format("select * from %s where status = :status", table.getName());
        return find(sql, Map.of("status", status.name()), null);
    }

    @Override
    public void updateStatus(String agentId, Agent.Status status) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        String sql = String.format(
            "update %s set status = :status, updated_at = NOW() where agent_id = :agentId",
            table.getName()
        );
        update(sql, Map.of("agentId", agentId, "status", status.name()), null);
    }

    @Override
    public void updateHeartbeat(String agentId, Instant heartbeatAt) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format(
            "update %s set last_heartbeat_at = :heartbeatAt, updated_at = NOW() where agent_id = :agentId",
            table.getName()
        );
        update(sql, Map.of("agentId", agentId, "heartbeatAt", java.sql.Timestamp.from(heartbeatAt)), null);
    }

    @Override
    public boolean exists(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format("select count(*) from %s where agent_id = :agentId", table.getName());
        return count(sql, Map.of("agentId", agentId), null) > 0;
    }

    private static class AgentRowMapper implements RowMapper<Agent> {
        @Override
        public Agent map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return Agent.builder()
                .withId(rs.getLong(Agent.Field.id.getDbColumn()))
                .withAgentId(rs.getString(Agent.Field.agentId.getDbColumn()))
                .withName(rs.getString(Agent.Field.name.getDbColumn()))
                .withDescription(rs.getString(Agent.Field.description.getDbColumn()))
                .withLabelsJson(rs.getString(Agent.Field.labelsJson.getDbColumn()))
                .withStatus(Agent.Status.valueOf(rs.getString(Agent.Field.status.getDbColumn())))
                .withRegisteredAt(rs.getTimestamp(Agent.Field.registeredAt.getDbColumn()) != null
                    ? rs.getTimestamp(Agent.Field.registeredAt.getDbColumn()).toInstant() : null)
                .withLastHeartbeatAt(rs.getTimestamp(Agent.Field.lastHeartbeatAt.getDbColumn()) != null
                    ? rs.getTimestamp(Agent.Field.lastHeartbeatAt.getDbColumn()).toInstant() : null)
                .withCreatedAt(rs.getTimestamp(Agent.Field.createdAt.getDbColumn()) != null
                    ? rs.getTimestamp(Agent.Field.createdAt.getDbColumn()).toInstant() : null)
                .withUpdatedAt(rs.getTimestamp(Agent.Field.updatedAt.getDbColumn()) != null
                    ? rs.getTimestamp(Agent.Field.updatedAt.getDbColumn()).toInstant() : null)
                .build();
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/persistence/AgentDao.java
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dao/AgentDaoJdbiImpl.java
git commit -m "feat: add AgentDao and Jdbi implementation"
```

---

## Task 7: AgentToken DAO

**Files:**
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/persistence/AgentTokenDao.java`
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dao/AgentTokenDaoJdbiImpl.java`

- [ ] **Step 1: Create AgentTokenDao interface**

```java
package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.auth.AgentToken;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.util.List;
import java.util.Optional;

public interface AgentTokenDao extends BaseDao<AgentToken> {

    Optional<AgentToken> findByTokenId(String tokenId);

    List<AgentToken> findByAgentId(String agentId);

    List<AgentToken> findActiveByAgentId(String agentId);

    void revokeToken(String tokenId, String revokedBy);
}
```

- [ ] **Step 2: Create AgentTokenDaoJdbiImpl**

```java
package top.ilovemyhome.dagtask.core.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.dagtask.si.auth.AgentToken;
import top.ilovemyhome.dagtask.si.persistence.AgentTokenDao;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AgentTokenDaoJdbiImpl extends BaseDaoJdbiImpl<AgentToken> implements AgentTokenDao {

    public AgentTokenDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName("t_agent_tokens")
            .withIdField("id")
            .withIdAutoGenerate(true)
            .withFieldColumnMap(AgentToken.FIELD_COLUMN_MAP)
            .build(), jdbi);
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(AgentToken.class, new AgentTokenRowMapper());
    }

    @Override
    public Optional<AgentToken> findByTokenId(String tokenId) {
        Objects.requireNonNull(tokenId, "tokenId must not be null");
        String sql = String.format("select * from %s where token_id = :tokenId", table.getName());
        return find(sql, Map.of("tokenId", tokenId), null).stream().findAny();
    }

    @Override
    public List<AgentToken> findByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format("select * from %s where agent_id = :agentId", table.getName());
        return find(sql, Map.of("agentId", agentId), null);
    }

    @Override
    public List<AgentToken> findActiveByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format(
            "select * from %s where agent_id = :agentId and revoked = false and expires_at > NOW()",
            table.getName()
        );
        return find(sql, Map.of("agentId", agentId), null);
    }

    @Override
    public void revokeToken(String tokenId, String revokedBy) {
        Objects.requireNonNull(tokenId, "tokenId must not be null");
        String sql = String.format(
            "update %s set revoked = true, revoked_at = NOW(), revoked_by = :revokedBy where token_id = :tokenId",
            table.getName()
        );
        update(sql, Map.of("tokenId", tokenId, "revokedBy", revokedBy), null);
    }

    private static class AgentTokenRowMapper implements RowMapper<AgentToken> {
        @Override
        public AgentToken map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return AgentToken.builder()
                .withId(rs.getLong(AgentToken.Field.id.getDbColumn()))
                .withTokenId(rs.getString(AgentToken.Field.tokenId.getDbColumn()))
                .withAgentId(rs.getString(AgentToken.Field.agentId.getDbColumn()))
                .withName(rs.getString(AgentToken.Field.name.getDbColumn()))
                .withDescription(rs.getString(AgentToken.Field.description.getDbColumn()))
                .withCreatedBy(rs.getString(AgentToken.Field.createdBy.getDbColumn()))
                .withCreatedAt(rs.getTimestamp(AgentToken.Field.createdAt.getDbColumn()) != null
                    ? rs.getTimestamp(AgentToken.Field.createdAt.getDbColumn()).toInstant() : null)
                .withExpiresAt(rs.getTimestamp(AgentToken.Field.expiresAt.getDbColumn()) != null
                    ? rs.getTimestamp(AgentToken.Field.expiresAt.getDbColumn()).toInstant() : null)
                .withRevoked(rs.getBoolean(AgentToken.Field.revoked.getDbColumn()))
                .withRevokedAt(rs.getTimestamp(AgentToken.Field.revokedAt.getDbColumn()) != null
                    ? rs.getTimestamp(AgentToken.Field.revokedAt.getDbColumn()).toInstant() : null)
                .withRevokedBy(rs.getString(AgentToken.Field.revokedBy.getDbColumn()))
                .build();
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/persistence/AgentTokenDao.java
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dao/AgentTokenDaoJdbiImpl.java
git commit -m "feat: add AgentTokenDao and Jdbi implementation"
```

---

## Task 8: AgentStatus DAO

**Files:**
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/persistence/AgentStatusDao.java`
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dao/AgentStatusDaoJdbiImpl.java`

- [ ] **Step 1: Create AgentStatusDao interface**

```java
package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.util.Optional;

public interface AgentStatusDao extends BaseDao<AgentStatus> {

    Optional<AgentStatus> findByAgentId(String agentId);

    int updateStatus(String agentId, boolean running, int pendingTasks, int runningTasks, int finishedTasks);

    int markUnregistered(String agentId);

    boolean exists(String agentId);
}
```

- [ ] **Step 2: Create AgentStatusDaoJdbiImpl**

```java
package top.ilovemyhome.dagtask.core.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.dagtask.si.persistence.AgentStatusDao;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AgentStatusDaoJdbiImpl extends BaseDaoJdbiImpl<AgentStatus> implements AgentStatusDao {

    public AgentStatusDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName("t_agent_status")
            .withIdField("id")
            .withIdAutoGenerate(true)
            .withFieldColumnMap(AgentStatus.FIELD_COLUMN_MAP)
            .build(), jdbi);
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(AgentStatus.class, new AgentStatusRowMapper());
    }

    @Override
    public Optional<AgentStatus> findByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format("select * from %s where agent_id = :agentId", table.getName());
        return find(sql, Map.of("agentId", agentId), null).stream().findAny();
    }

    @Override
    public int updateStatus(String agentId, boolean running, int pendingTasks, int runningTasks, int finishedTasks) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format(
            "update %s set running = :running, pending_tasks = :pendingTasks, " +
            "running_tasks = :runningTasks, finished_tasks = :finishedTasks, last_heartbeat_at = NOW() " +
            "where agent_id = :agentId",
            table.getName()
        );
        return update(sql, Map.of(
            "agentId", agentId,
            "running", running,
            "pendingTasks", pendingTasks,
            "runningTasks", runningTasks,
            "finishedTasks", finishedTasks
        ), null);
    }

    @Override
    public int markUnregistered(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format(
            "update %s set running = false, last_heartbeat_at = NOW() where agent_id = :agentId",
            table.getName()
        );
        return update(sql, Map.of("agentId", agentId), null);
    }

    @Override
    public boolean exists(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format("select count(*) from %s where agent_id = :agentId", table.getName());
        return count(sql, Map.of("agentId", agentId), null) > 0;
    }

    private static class AgentStatusRowMapper implements RowMapper<AgentStatus> {
        @Override
        public AgentStatus map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return AgentStatus.builder()
                .withId(rs.getLong(AgentStatus.Field.id.getDbColumn()))
                .withAgentId(rs.getString(AgentStatus.Field.agentId.getDbColumn()))
                .withAgentUrl(rs.getString(AgentStatus.Field.agentUrl.getDbColumn()))
                .withMaxConcurrentTasks(rs.getInt(AgentStatus.Field.maxConcurrentTasks.getDbColumn()))
                .withMaxPendingTasks(rs.getInt(AgentStatus.Field.maxPendingTasks.getDbColumn()))
                .withSupportedExecutionKeys(rs.getString(AgentStatus.Field.supportedExecutionKeys.getDbColumn()))
                .withRunning(rs.getBoolean(AgentStatus.Field.running.getDbColumn()))
                .withPendingTasks(rs.getInt(AgentStatus.Field.pendingTasks.getDbColumn()))
                .withRunningTasks(rs.getInt(AgentStatus.Field.runningTasks.getDbColumn()))
                .withFinishedTasks(rs.getInt(AgentStatus.Field.finishedTasks.getDbColumn()))
                .withLastHeartbeatAt(rs.getTimestamp(AgentStatus.Field.lastHeartbeatAt.getDbColumn()) != null
                    ? rs.getTimestamp(AgentStatus.Field.lastHeartbeatAt.getDbColumn()).toInstant() : null)
                .build();
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/persistence/AgentStatusDao.java
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dao/AgentStatusDaoJdbiImpl.java
git commit -m "feat: add AgentStatusDao and Jdbi implementation"
```

---

## Task 9: Compile DAOs

**Files:**
- All DAO files created in Tasks 6-8

- [ ] **Step 1: Compile dag-scheduler module**

Run: `cd dag-task/dag-scheduler && mvn compile`

Expected: Compiles successfully.

- [ ] **Step 2: Commit**

```bash
git commit -m "chore: verify all new DAOs compile"
```

---

## Task 10: Refactor TokenService

**Files:**
- Modify: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/token/TokenService.java`
- Create: `dag-task/dag-scheduler/src/test/java/top/ilovemyhome/dagtask/scheduler/token/TokenServiceTest.java`

- [ ] **Step 1: Replace TokenService entirely**

```java
package top.ilovemyhome.dagtask.scheduler.token;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import top.ilovemyhome.dagtask.scheduler.config.JwtConfig;
import top.ilovemyhome.dagtask.si.auth.AgentToken;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;
import top.ilovemyhome.dagtask.si.persistence.AgentDao;
import top.ilovemyhome.dagtask.si.persistence.AgentTokenDao;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class TokenService {

    private final AgentTokenDao agentTokenDao;
    private final AgentDao agentDao;
    private final JwtConfig jwtConfig;

    public TokenService(AgentTokenDao agentTokenDao, AgentDao agentDao, JwtConfig jwtConfig) {
        this.agentTokenDao = agentTokenDao;
        this.agentDao = agentDao;
        this.jwtConfig = jwtConfig;
    }

    public record GenerateTokenResult(
        String tokenId,
        String name,
        String description,
        Instant issuedAt,
        Instant expiresAt,
        String createdBy
    ) {}

    public GenerateTokenResult generateToken(String agentId, String name, String description,
                                              int expiresInDays, String createdBy) {
        // Validate agent exists
        if (agentDao.findByAgentId(agentId).isEmpty()) {
            throw new IllegalArgumentException("Agent not found: " + agentId);
        }

        String tokenId = generateId();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiresInDays, ChronoUnit.DAYS);

        GenerateTokenResult result = new GenerateTokenResult(tokenId, name, description, issuedAt, expiresAt, createdBy);

        AgentToken tokenRecord = AgentToken.builder()
            .withTokenId(tokenId)
            .withAgentId(agentId)
            .withName(name)
            .withDescription(description)
            .withCreatedBy(createdBy)
            .withCreatedAt(issuedAt)
            .withExpiresAt(expiresAt)
            .withRevoked(false)
            .withRevokedAt(null)
            .withRevokedBy(null)
            .build();

        agentTokenDao.create(tokenRecord);
        return result;
    }

    public String generateJwt(GenerateTokenResult result) {
        return Jwts.builder()
            .setIssuer(jwtConfig.getIssuer())
            .setSubject("agent")
            .setId(result.tokenId())
            .setIssuedAt(Date.from(result.issuedAt()))
            .setExpiration(Date.from(result.expiresAt()))
            .claim("name", result.name())
            .signWith(jwtConfig.getPrivateKey(), SignatureAlgorithm.RS256)
            .compact();
    }

    public boolean validateToken(String jwt) {
        try {
            var claims = Jwts.parser()
                .setSigningKey(jwtConfig.getPublicKey())
                .build()
                .parseClaimsJws(jwt);

            String tokenId = claims.getBody().getId();
            Optional<AgentToken> tokenOpt = agentTokenDao.findByTokenId(tokenId);

            if (tokenOpt.isEmpty()) {
                return false;
            }

            AgentToken token = tokenOpt.get();
            Instant now = Instant.now();

            return !token.isRevoked() && token.getExpiresAt().isAfter(now);
        } catch (Exception e) {
            return false;
        }
    }

    public void revokeToken(String tokenId, String revokedBy) {
        agentTokenDao.revokeToken(tokenId, revokedBy);
    }

    public List<TokenInfo> listTokens() {
        List<AgentToken> tokens = agentTokenDao.findAll();
        List<TokenInfo> result = new ArrayList<>();
        for (AgentToken token : tokens) {
            result.add(new TokenInfo(
                token.getTokenId(),
                token.getName(),
                token.getDescription(),
                token.getCreatedBy(),
                token.getCreatedAt(),
                token.getExpiresAt(),
                token.isRevoked()
            ));
        }
        return result;
    }

    private String generateId() {
        byte[] bytes = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

- [ ] **Step 2: Create TokenServiceTest**

```java
package top.ilovemyhome.dagtask.scheduler.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.ilovemyhome.dagtask.scheduler.config.JwtConfig;
import top.ilovemyhome.dagtask.si.agent.Agent;
import top.ilovemyhome.dagtask.si.auth.AgentToken;
import top.ilovemyhome.dagtask.si.persistence.AgentDao;
import top.ilovemyhome.dagtask.si.persistence.AgentTokenDao;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    AgentTokenDao agentTokenDao;

    @Mock
    AgentDao agentDao;

    JwtConfig jwtConfig;

    TokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();
        jwtConfig = new JwtConfig("test-issuer", keyPair.getPublic(), keyPair.getPrivate());
        tokenService = new TokenService(agentTokenDao, agentDao, jwtConfig);
    }

    @Test
    void shouldGenerateTokenWhenAgentExists() {
        when(agentDao.findByAgentId("agent-01")).thenReturn(Optional.of(
            Agent.builder().withAgentId("agent-01").withName("Agent 01").build()
        ));
        when(agentTokenDao.create(any(AgentToken.class))).thenReturn(1L);

        var result = tokenService.generateToken("agent-01", "my-token", "desc", 30, "admin");

        assertThat(result.tokenId()).isNotBlank();
        assertThat(result.name()).isEqualTo("my-token");
        assertThat(result.expiresAt()).isAfter(result.issuedAt());
        verify(agentTokenDao).create(any(AgentToken.class));
    }

    @Test
    void shouldRejectWhenAgentNotFound() {
        when(agentDao.findByAgentId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            tokenService.generateToken("unknown", "my-token", "desc", 30, "admin")
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Agent not found");
    }

    @Test
    void shouldValidateActiveToken() {
        String tokenId = "test-token-id";
        AgentToken token = AgentToken.builder()
            .withTokenId(tokenId)
            .withAgentId("agent-01")
            .withName("token")
            .withCreatedBy("admin")
            .withCreatedAt(Instant.now())
            .withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
            .withRevoked(false)
            .build();
        when(agentTokenDao.findByTokenId(tokenId)).thenReturn(Optional.of(token));

        var genResult = new TokenService.GenerateTokenResult(tokenId, "token", "desc",
            Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS), "admin");
        String jwt = tokenService.generateJwt(genResult);

        assertThat(tokenService.validateToken(jwt)).isTrue();
    }

    @Test
    void shouldRejectRevokedToken() {
        String tokenId = "revoked-token-id";
        AgentToken token = AgentToken.builder()
            .withTokenId(tokenId)
            .withAgentId("agent-01")
            .withName("token")
            .withCreatedBy("admin")
            .withCreatedAt(Instant.now())
            .withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
            .withRevoked(true)
            .withRevokedAt(Instant.now())
            .build();
        when(agentTokenDao.findByTokenId(tokenId)).thenReturn(Optional.of(token));

        var genResult = new TokenService.GenerateTokenResult(tokenId, "token", "desc",
            Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS), "admin");
        String jwt = tokenService.generateJwt(genResult);

        assertThat(tokenService.validateToken(jwt)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        String tokenId = "expired-token-id";
        AgentToken token = AgentToken.builder()
            .withTokenId(tokenId)
            .withAgentId("agent-01")
            .withName("token")
            .withCreatedBy("admin")
            .withCreatedAt(Instant.now().minus(2, ChronoUnit.DAYS))
            .withExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
            .withRevoked(false)
            .build();
        when(agentTokenDao.findByTokenId(tokenId)).thenReturn(Optional.of(token));

        var genResult = new TokenService.GenerateTokenResult(tokenId, "token", "desc",
            Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS), "admin");
        String jwt = tokenService.generateJwt(genResult);

        assertThat(tokenService.validateToken(jwt)).isFalse();
    }

    @Test
    void shouldListTokens() {
        AgentToken token = AgentToken.builder()
            .withTokenId("t1")
            .withAgentId("agent-01")
            .withName("token1")
            .withCreatedBy("admin")
            .withCreatedAt(Instant.now())
            .withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
            .withRevoked(false)
            .build();
        when(agentTokenDao.findAll()).thenReturn(List.of(token));

        List<TokenInfo> result = tokenService.listTokens();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tokenId()).isEqualTo("t1");
    }

    @Test
    void shouldRevokeToken() {
        tokenService.revokeToken("token-123", "admin");
        verify(agentTokenDao).revokeToken("token-123", "admin");
    }
}
```

- [ ] **Step 3: Run TokenServiceTest**

Run: `cd dag-task/dag-scheduler && mvn test -Dtest=TokenServiceTest`

Expected: All 7 tests pass.

- [ ] **Step 4: Commit**

```bash
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/token/TokenService.java
git add dag-task/dag-scheduler/src/test/java/top/ilovemyhome/dagtask/scheduler/token/TokenServiceTest.java
git commit -m "feat: refactor TokenService to use AgentTokenDao and AgentDao"
```

---

## Task 11: Update GenerateTokenRequest

**Files:**
- Modify: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/auth/GenerateTokenRequest.java`

- [ ] **Step 1: Add agentId field**

Replace the entire file with:

```java
package top.ilovemyhome.dagtask.si.auth;

public record GenerateTokenRequest(
    String agentId,
    String name,
    String description,
    int expiresInDays
) {}
```

- [ ] **Step 2: Commit**

```bash
git add dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/auth/GenerateTokenRequest.java
git commit -m "feat: add agentId to GenerateTokenRequest"
```

---

## Task 12: Update TokenManagementApi

**Files:**
- Modify: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/token/TokenManagementApi.java`

- [ ] **Step 1: Pass agentId to TokenService**

Replace the `generateToken` method call block (lines 73-94) with:

```java
        LOGGER.info("Manual token generation requested: agentId={}, name={}, expiresInDays={}, createdBy={}",
            request.agentId(), request.name(), request.expiresInDays(), createdBy);

        var result = tokenService.generateToken(
            request.agentId(),
            request.name(),
            request.description(),
            request.expiresInDays(),
            createdBy
        );
```

Full file should look like:

```java
package top.ilovemyhome.dagtask.scheduler.token;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.Constants;
import top.ilovemyhome.dagtask.si.auth.GenerateTokenRequest;
import top.ilovemyhome.dagtask.si.auth.GenerateTokenResponse;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;

import java.util.List;

/**
 * Admin REST API endpoints for managing agent tokens.
 */
@Path(Constants.API_ADMIN)
@Produces(MediaType.APPLICATION_JSON)
public class TokenManagementApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenManagementApi.class);

    private final TokenService tokenService;

    @Inject
    public TokenManagementApi(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * List all existing tokens.
     */
    @GET
    @Path("/tokens")
    public Response listTokens() {
        List<TokenInfo> tokens = tokenService.listTokens();
        return Response.ok()
            .entity(ResEntityHelper.ok("Tokens retrieved successfully", tokens))
            .build();
    }

    /**
     * Manually generate a new token for a specific agent.
     */
    @POST
    @Path("/tokens/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateToken(GenerateTokenRequest request,
                                  @QueryParam("createdBy") String createdBy) {
        LOGGER.info("Manual token generation requested: agentId={}, name={}, expiresInDays={}, createdBy={}",
            request.agentId(), request.name(), request.expiresInDays(), createdBy);

        var result = tokenService.generateToken(
            request.agentId(),
            request.name(),
            request.description(),
            request.expiresInDays(),
            createdBy
        );
        String jwt = tokenService.generateJwt(result);

        GenerateTokenResponse.Data data = new GenerateTokenResponse.Data(
            jwt,
            result.tokenId(),
            result.expiresAt(),
            result.name()
        );
        GenerateTokenResponse response = new GenerateTokenResponse(true, data, null);

        return Response.ok()
            .entity(ResEntityHelper.ok("Token generated successfully", response))
            .build();
    }

    /**
     * Revoke an existing token.
     */
    @POST
    @Path("/tokens/{tokenId}/revoke")
    public Response revokeToken(@PathParam("tokenId") String tokenId,
                                @QueryParam("revokedBy") String revokedBy) {
        LOGGER.info("Token revocation requested: tokenId={}, revokedBy={}", tokenId, revokedBy);
        tokenService.revokeToken(tokenId, revokedBy);
        return Response.ok()
            .entity(ResEntityHelper.ok("Token revoked successfully", null))
            .build();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/token/TokenManagementApi.java
git commit -m "feat: pass agentId from GenerateTokenRequest to TokenService"
```

---

## Task 13: Update PublicRegistrationApi

**Files:**
- Modify: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/auth/PublicRegistrationApi.java`

- [ ] **Step 1: Inject AgentDao and create Agent before token generation**

Replace the entire file with:

```java
package top.ilovemyhome.dagtask.scheduler.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.Constants;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationRequest;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationResponse;
import top.ilovemyhome.dagtask.si.auth.TokenPushRequest;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;
import top.ilovemyhome.dagtask.si.agent.Agent;
import top.ilovemyhome.dagtask.si.persistence.AgentDao;
import top.ilovemyhome.dagtask.scheduler.config.AutoApproveConfig;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Public REST API endpoint for agent self-registration.
 */
@Path(Constants.API_SCHEDULER)
@Produces(MediaType.APPLICATION_JSON)
public class PublicRegistrationApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicRegistrationApi.class);

    private final TokenService tokenService;
    private final TokenPusher tokenPusher;
    private final AutoApproveConfig autoApproveConfig;
    private final AgentDao agentDao;
    private final SecureRandom random = new SecureRandom();

    @Inject
    public PublicRegistrationApi(TokenService tokenService,
                                 TokenPusher tokenPusher,
                                 AutoApproveConfig autoApproveConfig,
                                 AgentDao agentDao) {
        this.tokenService = tokenService;
        this.tokenPusher = tokenPusher;
        this.autoApproveConfig = autoApproveConfig;
        this.agentDao = agentDao;
    }

    @POST
    @Path("/registration/start")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startRegistration(AgentRegistrationRequest request) {
        LOGGER.info("Received agent registration request: name={}, callback={}",
            request.name(), request.callbackUrl());

        // Check whitelist match
        boolean approved = autoApproveConfig.isMatch(request.name());
        if (!approved) {
            LOGGER.warn("Agent registration rejected - name '{}' does not match whitelist", request.name());
            return Response.status(Response.Status.FORBIDDEN)
                .entity(ResEntityHelper.badRequest("Agent name not in whitelist, registration rejected"))
                .build();
        }

        String nonce = generateId();
        Instant now = Instant.now();

        // Ensure agent record exists in t_agents
        if (!agentDao.exists(request.name())) {
            Agent agent = Agent.builder()
                .withAgentId(request.name())
                .withName(request.name())
                .withDescription(request.description())
                .withStatus(Agent.Status.ACTIVE)
                .withRegisteredAt(now)
                .withLastHeartbeatAt(now)
                .withCreatedAt(now)
                .withUpdatedAt(now)
                .build();
            agentDao.create(agent);
            LOGGER.info("Created new agent record: agentId={}", request.name());
        } else {
            LOGGER.info("Agent already exists: agentId={}", request.name());
        }

        // Generate token bound to the agent
        var tokenResult = tokenService.generateToken(
            request.name(), request.name(), request.description(), 365, "system");
        String jwt = tokenService.generateJwt(tokenResult);

        // Push token to callback
        TokenPushRequest pushRequest = new TokenPushRequest(
            null,
            jwt,
            tokenResult.tokenId(),
            tokenResult.expiresAt(),
            tokenResult.name()
        );

        boolean pushed = tokenPusher.pushToken(request.callbackUrl(), nonce, pushRequest);

        if (pushed) {
            LOGGER.info("Agent registration auto-approved and token pushed: name={}", request.name());
            AgentRegistrationResponse.Data data = new AgentRegistrationResponse.Data(
                tokenResult.tokenId(),
                "APPROVED",
                "Registration approved, token pushed successfully"
            );
            return Response.ok()
                .entity(ResEntityHelper.ok("Registration approved", new AgentRegistrationResponse(true, data, null)))
                .build();
        } else {
            LOGGER.error("Failed to push token to callback URL: {}", request.callbackUrl());
            return Response.status(Response.Status.BAD_GATEWAY)
                .entity(ResEntityHelper.badRequest("Failed to push token to callback URL"))
                .build();
        }
    }

    private String generateId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/auth/PublicRegistrationApi.java
git commit -m "feat: create Agent record before generating token in PublicRegistrationApi"
```

---

## Task 14: Update DagSchedulerBuilder

**Files:**
- Modify: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/server/DagSchedulerBuilder.java`

- [ ] **Step 1: Register new DAOs in build() method**

Add the following lines after `var agentRegistryDao = new AgentRegistryDaoJdbiImpl(jdbiToUse);` (around line 159):

```java
        var agentDao = new AgentDaoJdbiImpl(jdbiToUse);
        var agentTokenDao = new AgentTokenDaoJdbiImpl(jdbiToUse);
        var agentStatusDao = new AgentStatusDaoJdbiImpl(jdbiToUse);
```

Also add getter methods at the bottom of the class (before the closing brace):

```java
    public AgentDaoJdbiImpl getAgentDao() {
        return new AgentDaoJdbiImpl(jdbi);
    }
```

Wait — `DagSchedulerServer` needs to expose the new DAOs. Let's check if we need to modify `DagSchedulerServer` too.

Actually, looking at `WebServerBootstrap`, it accesses DAOs via `schedulerServer.getAgentRegistryDao()`. We should add getters for the new DAOs to `DagSchedulerServer`.

Modify `DagSchedulerServer` to expose new DAOs. Add fields and getters:

In `DagSchedulerServer.java`, add:
- `private final AgentDao agentDao;`
- `private final AgentTokenDao agentTokenDao;`
- `private final AgentStatusDao agentStatusDao;`

Update constructor to accept them, add getters.

Since I don't have the full `DagSchedulerServer` source in context, add these fields and update the constructor in `DagSchedulerBuilder.build()` to pass them. Then add getters.

For the plan, the key change in `DagSchedulerBuilder` is:

After line 159 (`var agentRegistryDao = new AgentRegistryDaoJdbiImpl(jdbiToUse);`), insert:

```java
        var agentDao = new AgentDaoJdbiImpl(jdbiToUse);
        var agentTokenDao = new AgentTokenDaoJdbiImpl(jdbiToUse);
        var agentStatusDao = new AgentStatusDaoJdbiImpl(jdbiToUse);
```

And update the `new DagSchedulerServer(...)` call to pass `agentDao, agentTokenDao, agentStatusDao`.

- [ ] **Step 2: Commit**

```bash
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/server/DagSchedulerBuilder.java
git commit -m "feat: register new AgentDao, AgentTokenDao, AgentStatusDao in DagSchedulerBuilder"
```

---

## Task 15: Update WebServerBootstrap

**Files:**
- Modify: `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/application/WebServerBootstrap.java`

- [ ] **Step 1: Wire new DAOs into services**

In `createRestHandler`, replace the TokenService and PublicRegistrationApi construction blocks with:

```java
        // Create new DAOs
        var agentDao = new top.ilovemyhome.dagtask.core.dao.AgentDaoJdbiImpl(jdbi);
        var agentTokenDao = new top.ilovemyhome.dagtask.core.dao.AgentTokenDaoJdbiImpl(jdbi);

        // Create authentication components
        JwtConfig jwtConfig = readJwtConfig(config);
        AutoApproveConfig autoApproveConfig = readAutoApproveConfig(config);
        TokenService tokenService = new TokenService(agentTokenDao, agentDao, jwtConfig);
        TokenPusher tokenPusher = new DefaultTokenPusher();
        PublicRegistrationApi publicRegistrationApi = new PublicRegistrationApi(tokenService, tokenPusher, autoApproveConfig, agentDao);
        TokenManagementApi tokenManagementApi = new TokenManagementApi(tokenService);
```

Note: You need access to the Jdbi instance. In `WebServerBootstrap`, `schedulerServer` is available but `jdbi` is not directly exposed. Check `DagSchedulerServer` for a `getJdbi()` method, or use `schedulerServer.getAgentRegistryDao()` and cast if needed.

Actually, looking at the code, `DagSchedulerServer` likely has a `getJdbi()` method. If not, add it. For now, assume it's accessible.

If `DagSchedulerServer` doesn't expose `getJdbi()`, you can get the Jdbi from the `AppContext`'s data source or add a getter to `DagSchedulerServer`.

The simplest approach: add `getJdbi()` to `DagSchedulerServer` if it doesn't exist.

- [ ] **Step 2: Commit**

```bash
git add dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/application/WebServerBootstrap.java
git commit -m "feat: wire new DAOs into TokenService and PublicRegistrationApi"
```

---

## Task 16: Full Build and Verification

**Files:**
- All modified and new files

- [ ] **Step 1: Compile entire project**

Run: `cd dag-task && mvn clean compile`

Expected: Build succeeds with no compilation errors.

- [ ] **Step 2: Run all tests**

Run: `cd dag-task && mvn test`

Expected: All existing tests pass + new TokenServiceTest passes.

If there are failures:
- Check that `GenerateTokenRequest` usages in tests compile (the constructor now requires 4 args)
- Check that `TokenService` constructor calls pass the new `agentId` parameter

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat: decouple agent, token, and status into independent tables and DAOs"
```

---

## Self-Review Checklist

### 1. Spec Coverage

| Spec Requirement | Plan Task |
|------------------|-----------|
| Create `t_agents` table | Task 1 |
| Create `t_agent_tokens` table | Task 1 |
| Create `t_agent_status` table | Task 1 |
| Migrate data from `t_agent_registry` | Task 1 |
| Agent entity + DAO | Tasks 2, 6 |
| AgentToken entity + DAO | Tasks 3, 7 |
| AgentStatus entity + DAO | Tasks 4, 8 |
| `GenerateTokenRequest` with `agentId` | Task 11 |
| `TokenService` validates agent exists | Task 10 |
| `TokenService` uses `AgentTokenDao` | Task 10 |
| `TokenManagementApi` passes `agentId` | Task 12 |
| `PublicRegistrationApi` creates Agent | Task 13 |
| DI wiring in `DagSchedulerBuilder` | Task 14 |
| DI wiring in `WebServerBootstrap` | Task 15 |

### 2. Placeholder Scan

- No TBD/TODO/fill-in-details found.
- All steps show actual code.
- All method signatures are consistent across tasks.

### 3. Type Consistency

- `TokenService.generateToken(String agentId, String name, String description, int expiresInDays, String createdBy)` — used consistently in Tasks 10, 12, 13.
- `Agent.Status` enum values: `PENDING`, `ACTIVE`, `INACTIVE` — consistent in Agent.java and PublicRegistrationApi.
- Table names: `t_agents`, `t_agent_tokens`, `t_agent_status` — consistent across migration, DAO implementations, and TableDescription configs.
