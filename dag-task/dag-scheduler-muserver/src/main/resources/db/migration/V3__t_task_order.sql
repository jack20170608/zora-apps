-- ============================================================================
-- Database: PostgreSQL
-- Flyway Migration: V3
-- Object: Table t_task_order
-- Description: Task order (DAG workflow definition) table stores the definition
--              of a complete DAG workflow consisting of multiple tasks with dependencies.
-- ============================================================================

-- Create task order table (stores DAG workflow definitions)
CREATE TABLE IF NOT EXISTS t_task_order (
    -- Primary key auto-incrementing ID
    id BIGSERIAL PRIMARY KEY,
    -- Unique business key for the workflow (user-provided identifier)
    key VARCHAR(255) NOT NULL UNIQUE,
    -- Human-readable name of the workflow
    name VARCHAR(255) NOT NULL,
    -- Optional description of what this workflow does
    description TEXT,
    -- Timestamp when this workflow was created
    create_dt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Timestamp when this workflow was last updated
    last_update_dt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Version number for optimistic locking
    version INTEGER NOT NULL DEFAULT 1
);

-- Create index on the unique key for faster lookups
CREATE INDEX IF NOT EXISTS idx_t_task_order_key
    ON t_task_order(key);

-- Comment on table
COMMENT ON TABLE t_task_order IS 'Task order (DAG workflow definition) table stores complete DAG workflow definitions';

-- Column comments
COMMENT ON COLUMN t_task_order.id IS 'Primary key auto-incrementing ID';
COMMENT ON COLUMN t_task_order.key IS 'Unique business key for the workflow (user-provided identifier)';
COMMENT ON COLUMN t_task_order.name IS 'Human-readable name of the workflow';
COMMENT ON COLUMN t_task_order.description IS 'Optional description of what this workflow does';
COMMENT ON COLUMN t_task_order.create_dt IS 'Timestamp when this workflow was created';
COMMENT ON COLUMN t_task_order.last_update_dt IS 'Timestamp when this workflow was last updated';
COMMENT ON COLUMN t_task_order.version IS 'Version number for optimistic locking';
