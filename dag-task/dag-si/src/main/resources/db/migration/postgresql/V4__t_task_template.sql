-- ============================================================================
-- Database: PostgreSQL
-- Flyway Migration: V4
-- Object: Table t_task_template
-- Description: DAG task template table stores reusable DAG workflow definitions.
--              Templates allow versioning and parameterization for creating
--              multiple similar workflow instances from a single definition.
-- ============================================================================

-- Create sequence for auto-generated primary key
CREATE SEQUENCE IF NOT EXISTS seq_t_task_template_id;

-- Create task template table (stores versioned DAG workflow templates)
CREATE TABLE IF NOT EXISTS t_task_template (
    -- Auto-incrementing surrogate primary key
    id BIGINT NOT NULL DEFAULT nextval('seq_t_task_template_id'),
    -- Unique business key for the template (identifies the template across versions)
    template_key VARCHAR(255) NOT NULL,
    -- Human-readable name of the template
    template_name VARCHAR(255) NOT NULL,
    -- Optional description of what this template does
    description TEXT,
    -- Semantic version of this template (e.g., 1.0.0, 1.1.0)
    version VARCHAR(50) NOT NULL,
    -- Whether this is the currently active/recommended version
    active BOOLEAN NOT NULL DEFAULT TRUE,
    -- JSON serialized complete DAG definition including all tasks and dependencies
    -- Structure contains: tasks array with each task's name, execution_key, config,
    -- and dependencies list that defines the DAG structure
    dag_definition TEXT NOT NULL,
    -- JSON Schema describing the template parameters that can be injected
    -- when instantiating this template. Defines parameter names, types,
    -- default values, and validation constraints.
    parameter_schema TEXT,
    -- Timestamp when this template version was created
    create_dt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Timestamp when this template version was last updated
    last_update_dt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Version sequence number for optimistic locking
    version_seq INTEGER NOT NULL DEFAULT 1,

    -- Primary key
    CONSTRAINT pk_t_task_template PRIMARY KEY (id),
    -- Unique constraint: same template key + version must be unique
    CONSTRAINT uk_t_task_template_key_version UNIQUE (template_key, version)
);

-- Create indexes for faster lookups
CREATE INDEX IF NOT EXISTS idx_t_task_template_template_key
    ON t_task_template(template_key);
CREATE INDEX IF NOT EXISTS idx_t_task_template_active
    ON t_task_template(active);

-- Comment on table
COMMENT ON TABLE t_task_template IS 'DAG task template table stores versioned reusable workflow templates';

-- Column comments
COMMENT ON COLUMN t_task_template.id IS 'Auto-incrementing surrogate primary key from sequence';
COMMENT ON COLUMN t_task_template.template_key IS 'Unique business key identifying the template across versions';
COMMENT ON COLUMN t_task_template.template_name IS 'Human-readable name of the template';
COMMENT ON COLUMN t_task_template.description IS 'Optional description of what this template does';
COMMENT ON COLUMN t_task_template.version IS 'Semantic version of this template version';
COMMENT ON COLUMN t_task_template.active IS 'Whether this is the currently active/recommended version';
COMMENT ON COLUMN t_task_template.dag_definition IS 'JSON serialized complete DAG definition with tasks and dependencies';
COMMENT ON COLUMN t_task_template.parameter_schema IS 'JSON Schema describing template parameters for instantiation';
COMMENT ON COLUMN t_task_template.create_dt IS 'Timestamp when this template version was created';
COMMENT ON COLUMN t_task_template.last_update_dt IS 'Timestamp when this template version was last updated';
COMMENT ON COLUMN t_task_template.version_seq IS 'Version sequence number for optimistic locking';
