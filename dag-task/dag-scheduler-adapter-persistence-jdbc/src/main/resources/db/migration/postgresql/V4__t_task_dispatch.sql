-- Task dispatch tracking table
-- This table tracks where each task was dispatched (which agent),
-- when it was dispatched, and the current dispatch status.
-- Used for:
--   1. Management operations: forceOk, kill - need to know which agent to contact
--   2. Auditing: track which agent executed which tasks
--   3. Statistics: analyze load distribution across agents
--   4. Debugging: troubleshoot dispatch issues

-- Create sequence for auto-generated primary key
CREATE SEQUENCE IF NOT EXISTS seq_t_task_dispatch_id;

CREATE TABLE t_task_dispatch (
    id BIGINT NOT NULL DEFAULT nextval('seq_t_task_dispatch_id'),
    task_id BIGINT NOT NULL,
    agent_id VARCHAR(255) NOT NULL,
    agent_url VARCHAR(1024) NOT NULL,
    dispatch_time TIMESTAMP NOT NULL,
    last_update_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    parameters VARCHAR
);

-- Index for finding dispatch record by task ID (primary lookup for management operations)
CREATE INDEX idx_t_task_dispatch_task_id ON t_task_dispatch(task_id);

-- Index for finding all dispatches by agent ID (for statistics and cleanup after unregister)
CREATE INDEX idx_t_task_dispatch_agent_id ON t_task_dispatch(agent_id);

-- Index for filtering by dispatch status (for monitoring pending/completed tasks)
CREATE INDEX idx_t_task_dispatch_status ON t_task_dispatch(status);

-- Index for sorting by dispatch time (for time-based queries)
CREATE INDEX idx_t_task_dispatch_dispatch_time ON t_task_dispatch(dispatch_time);
