-- ============================================================================
-- Database: PostgreSQL
-- Flyway Migration: V1
-- Object: Table t_agent_registry
-- Description: Registry table for storing agent registration information
--              Persists information about all agents that have registered with
--              the DAG scheduling center, including their current status.
-- ============================================================================

-- Create sequence for auto-generated primary key
CREATE SEQUENCE IF NOT EXISTS seq_t_agent_registry_id;

-- Create agent registry table
CREATE TABLE IF NOT EXISTS t_agent_registry (
    -- Auto-incrementing primary key
    id BIGINT NOT NULL DEFAULT nextval('seq_t_agent_registry_id'),
    -- Unique identifier provided by the agent instance (business key)
    agent_id VARCHAR(255) NOT NULL,
    -- Full URL where the agent can be reached from the scheduling center
    agent_url VARCHAR(1024) NOT NULL,
    -- Maximum number of concurrent tasks this agent can execute
    max_concurrent_tasks INTEGER NOT NULL,
    -- Maximum number of pending tasks that can be queued on this agent
    max_pending_tasks INTEGER NOT NULL,
    -- Comma-separated list of execution keys (task types) supported by this agent
    supported_execution_keys TEXT NOT NULL,
    -- Timestamp when the agent was first registered
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Timestamp of the last heartbeat/status report received from this agent
    last_heartbeat_at TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Whether the agent is currently considered active/running
    running BOOLEAN NOT NULL DEFAULT TRUE,
    -- Current number of tasks waiting in the pending queue
    pending_tasks INTEGER NOT NULL DEFAULT 0,
    -- Current number of tasks actively executing
    running_tasks INTEGER NOT NULL DEFAULT 0,
    -- Total number of tasks finished since agent registration
    finished_tasks INTEGER NOT NULL DEFAULT 0,

    -- Primary key
    CONSTRAINT pk_t_agent_registry PRIMARY KEY (id),
    -- Unique constraint on business key (agent_id provided by agent)
    CONSTRAINT uk_t_agent_registry_agent_id UNIQUE (agent_id)
);

-- Index for faster lookup of active agents
CREATE INDEX IF NOT EXISTS idx_t_agent_registry_running
    ON t_agent_registry(running);

-- Index for faster lookup by business key
CREATE INDEX IF NOT EXISTS idx_t_agent_registry_agent_id
    ON t_agent_registry(agent_id);

-- Comment on table
COMMENT ON TABLE t_agent_registry IS 'Registry of all agent instances that have registered with the DAG scheduling center';

-- Column comments
COMMENT ON COLUMN t_agent_registry.id IS 'Auto-incrementing surrogate primary key';
COMMENT ON COLUMN t_agent_registry.agent_id IS 'Unique business identifier provided by the agent instance';
COMMENT ON COLUMN t_agent_registry.agent_url IS 'Full URL endpoint where the scheduling center can reach this agent';
COMMENT ON COLUMN t_agent_registry.max_concurrent_tasks IS 'Maximum number of concurrent tasks this agent can execute';
COMMENT ON COLUMN t_agent_registry.max_pending_tasks IS 'Maximum number of pending tasks that can be queued';
COMMENT ON COLUMN t_agent_registry.supported_execution_keys IS 'Comma-separated list of execution keys (task types) supported by this agent';
COMMENT ON COLUMN t_agent_registry.registered_at IS 'Timestamp when the agent was first registered';
COMMENT ON COLUMN t_agent_registry.last_heartbeat_at IS 'Timestamp of the last heartbeat received from this agent';
COMMENT ON COLUMN t_agent_registry.running IS 'Whether the agent is currently active and accepting tasks';
COMMENT ON COLUMN t_agent_registry.pending_tasks IS 'Current number of pending tasks in queue';
COMMENT ON COLUMN t_agent_registry.running_tasks IS 'Current number of actively running tasks';
COMMENT ON COLUMN t_agent_registry.finished_tasks IS 'Total number of tasks finished since registration';
