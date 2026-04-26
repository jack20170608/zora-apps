-- ============================================================================
-- Flyway Migration: V6
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
