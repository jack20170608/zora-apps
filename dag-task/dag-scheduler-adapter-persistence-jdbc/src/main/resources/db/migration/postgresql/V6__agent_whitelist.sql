-- ============================================================================
-- Flyway Migration: V6
-- Description: Agent whitelist table for IP and agentId based access control
-- ============================================================================

-- Agent whitelist table
CREATE TABLE IF NOT EXISTS t_agent_whitelist (
    id BIGSERIAL PRIMARY KEY,
    ip_segment VARCHAR(64),
    agent_id VARCHAR(255),
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_t_agent_whitelist_ip ON t_agent_whitelist(ip_segment);
CREATE INDEX idx_t_agent_whitelist_agent_id ON t_agent_whitelist(agent_id);
CREATE INDEX idx_t_agent_whitelist_enabled ON t_agent_whitelist(enabled);
