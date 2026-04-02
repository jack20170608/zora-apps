-- Flyway migration V1: Initial DAG Task schema
-- Create table for task orders (DAG workflow definitions)

CREATE TABLE IF NOT EXISTS t_task_order (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    create_dt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_update_dt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1
);

COMMENT ON TABLE t_task_order IS 'Task order (DAG workflow definition) table';
COMMENT ON COLUMN t_task_order.id IS 'Primary key ID';
COMMENT ON COLUMN t_task_order.key IS 'Unique business key for the workflow';
COMMENT ON COLUMN t_task_order.name IS 'Workflow name';
COMMENT ON COLUMN t_task_order.description IS 'Workflow description';
COMMENT ON COLUMN t_task_order.create_dt IS 'Creation timestamp';
COMMENT ON COLUMN t_task_order.last_update_dt IS 'Last update timestamp';
COMMENT ON COLUMN t_task_order.version IS 'Version number for optimistic locking';

-- Create table for task records (individual task execution instances)

CREATE TABLE IF NOT EXISTS t_task (
    id BIGSERIAL PRIMARY KEY,
    order_key VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    execution_key VARCHAR(255),
    successor_ids BIGINT[],
    input TEXT,
    output TEXT,
    async BOOLEAN NOT NULL DEFAULT FALSE,
    dummy BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL,
    success BOOLEAN,
    fail_reason TEXT,
    create_dt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_update_dt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    start_dt TIMESTAMP,
    end_dt TIMESTAMP,
    timeout INTEGER,
    timeout_unit VARCHAR(50)
);

COMMENT ON TABLE t_task IS 'Task record (individual task execution instance) table';
COMMENT ON COLUMN t_task.id IS 'Primary key ID';
COMMENT ON COLUMN t_task.order_key IS 'Reference to the task order (workflow)';
COMMENT ON COLUMN t_task.name IS 'Task name';
COMMENT ON COLUMN t_task.description IS 'Task description';
COMMENT ON COLUMN t_task.execution_key IS 'Execution identifier for distributed execution';
COMMENT ON COLUMN t_task.successor_ids IS 'Array of successor task IDs that depend on this task';
COMMENT ON COLUMN t_task.input IS 'JSON serialized input data for the task';
COMMENT ON COLUMN t_task.output IS 'JSON serialized output data from the task';
COMMENT ON COLUMN t_task.async IS 'Whether this task should be executed asynchronously';
COMMENT ON COLUMN t_task.dummy IS 'Whether this is a dummy/placeholder task';
COMMENT ON COLUMN t_task.status IS 'Current execution status: INIT, RUNNING, SUCCESS, ERROR, CANCELLED';
COMMENT ON COLUMN t_task.success IS 'Whether the task completed successfully';
COMMENT ON COLUMN t_task.fail_reason IS 'Failure reason if task failed';
COMMENT ON COLUMN t_task.create_dt IS 'Creation timestamp';
COMMENT ON COLUMN t_task.last_update_dt IS 'Last update timestamp';
COMMENT ON COLUMN t_task.start_dt IS 'Execution start timestamp';
COMMENT ON COLUMN t_task.end_dt IS 'Execution end timestamp';
COMMENT ON COLUMN t_task.timeout IS 'Timeout value';
COMMENT ON COLUMN t_task.timeout_unit IS 'Timeout time unit';

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_t_task_order_key ON t_task_order(key);
CREATE INDEX IF NOT EXISTS idx_t_task_order_fk ON t_task(order_key);
CREATE INDEX IF NOT EXISTS idx_t_task_status ON t_task(status);
