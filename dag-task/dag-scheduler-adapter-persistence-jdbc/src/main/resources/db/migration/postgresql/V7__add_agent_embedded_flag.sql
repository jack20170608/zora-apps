-- ============================================================================
-- Flyway Migration: V7
-- Description: Add embedded flag to t_agents for all-in-one mode identification
-- ============================================================================

ALTER TABLE t_agents ADD COLUMN IF NOT EXISTS embedded BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_t_agents_embedded ON t_agents(embedded);
