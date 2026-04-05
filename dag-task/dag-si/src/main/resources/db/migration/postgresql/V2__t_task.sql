-- ============================================================================
-- Database: PostgreSQL
-- Flyway Migration: V2
-- Object: Table t_task
-- Description: Task record table stores individual task execution instances within
--              a task order (DAG workflow). Each record represents one task that
--              will be or has been executed.
-- ============================================================================

-- Create task record table (individual task execution instances)
CREATE TABLE IF NOT EXISTS t_task (
    -- Primary key auto-incrementing ID
    id BIGSERIAL PRIMARY KEY,
    -- Reference to the parent task order (workflow) this task belongs to
    order_key VARCHAR(255) NOT NULL,
    -- Human-readable name of the task
    name VARCHAR(255) NOT NULL,
    -- Optional description of what this task does
    description TEXT,
    -- Execution key that identifies which agent type can execute this task
    execution_key VARCHAR(255),
    -- Array of successor task IDs that depend on this task completing
    successor_ids BIGINT[],
    -- JSON serialized input data for the task
    input TEXT,
    -- JSON serialized output data from the task execution
    output TEXT,
    -- Whether this task should be executed asynchronously by the agent
    async BOOLEAN NOT NULL DEFAULT FALSE,
    -- Whether this is a dummy/placeholder task (used for structure only)
    dummy BOOLEAN NOT NULL DEFAULT FALSE,
    -- Current execution status: INIT, RUNNING, SUCCESS, ERROR, CANCELLED
    status VARCHAR(50) NOT NULL,
    -- Whether the task completed successfully (only valid after completion)
    success BOOLEAN,
    -- Failure reason if the task execution failed
    fail_reason TEXT,
    -- Timestamp when this task record was created
    create_dt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Timestamp when this task record was last updated
    last_update_dt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Timestamp when task execution started
    start_dt TIMESTAMP,
    -- Timestamp when task execution ended (completed or failed)
    end_dt TIMESTAMP,
    -- Timeout value for task execution
    timeout INTEGER,
    -- Time unit for the timeout value (e.g., MINUTES, SECONDS)
    timeout_unit VARCHAR(50)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_t_task_order_key
    ON t_task(order_key);
CREATE INDEX IF NOT EXISTS idx_t_task_status
    ON t_task(status);

-- Comment on table
COMMENT ON TABLE t_task IS 'Task record table stores individual task execution instances within a DAG workflow';

-- Column comments
COMMENT ON COLUMN t_task.id IS 'Primary key auto-incrementing ID';
COMMENT ON COLUMN t_task.order_key IS 'Reference to the parent task order (workflow) this task belongs to';
COMMENT ON COLUMN t_task.name IS 'Human-readable name of the task';
COMMENT ON COLUMN t_task.description IS 'Optional description of what this task does';
COMMENT ON COLUMN t_task.execution_key IS 'Execution key identifies which agent type can execute this task';
COMMENT ON COLUMN t_task.successor_ids IS 'Array of successor task IDs that depend on this task completing';
COMMENT ON COLUMN t_task.input IS 'JSON serialized input data for the task';
COMMENT ON COLUMN t_task.output IS 'JSON serialized output data from the task execution';
COMMENT ON COLUMN t_task.async IS 'Whether this task should be executed asynchronously by the agent';
COMMENT ON COLUMN t_task.dummy IS 'Whether this is a dummy/placeholder task (structure only)';
COMMENT ON COLUMN t_task.status IS 'Current execution status: INIT, RUNNING, SUCCESS, ERROR, CANCELLED';
COMMENT ON COLUMN t_task.success IS 'Whether the task completed successfully (valid after completion)';
COMMENT ON COLUMN t_task.fail_reason IS 'Failure reason if the task execution failed';
COMMENT ON COLUMN t_task.create_dt IS 'Timestamp when this task record was created';
COMMENT ON COLUMN t_task.last_update_dt IS 'Timestamp when this task record was last updated';
COMMENT ON COLUMN t_task.start_dt IS 'Timestamp when task execution started';
COMMENT ON COLUMN t_task.end_dt IS 'Timestamp when task execution ended (completed or failed)';
COMMENT ON COLUMN t_task.timeout IS 'Timeout value for task execution';
COMMENT ON COLUMN t_task.timeout_unit IS 'Time unit for the timeout value (e.g., MINUTES, SECONDS)';
