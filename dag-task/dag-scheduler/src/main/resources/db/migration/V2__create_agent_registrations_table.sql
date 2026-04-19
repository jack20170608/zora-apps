CREATE TABLE agent_registrations (
    id BIGSERIAL PRIMARY KEY,
    registration_id VARCHAR(64) NOT NULL UNIQUE,
    agent_name VARCHAR(255) NOT NULL,
    description TEXT,
    labels JSONB,
    callback_url TEXT NOT NULL,
    nonce VARCHAR(64) NOT NULL,
    client_address VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    processed_by VARCHAR(100),
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_agent_registrations_status ON agent_registrations(status);
CREATE INDEX idx_agent_registrations_expires_at ON agent_registrations(expires_at);
