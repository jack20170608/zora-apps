# Agent Token Authentication Design

## Overview

Currently, any agent can register with the DAG scheduling center without authentication. This design adds token-based authentication to secure agent registration:

- Administrators can generate agent tokens via a management API
- Agents must authenticate with a valid token when registering
- Tokens can be revoked at any time
- The existing JWT infrastructure (RSA keys) is reused for signing and verification

## Requirements

1. **Token Generation**: Admin API to generate new agent tokens with configurable expiration
2. **Token Validation**: Server must validate token before accepting agent registration
3. **Token Revocation**: Support revoking existing tokens
4. **Token Listing**: List all tokens with their current status
5. **Client Changes**: Agent client must send token in HTTP requests

## Architecture

### Components

| Component | Location | Responsibility |
|-----------|----------|-----------------|
| `agent_tokens` table | Database | Persist token metadata for revocation checking |
| `TokenService` | `dag-scheduler` core | Business logic for token generation and validation |
| `TokenManagementApi` | `dag-scheduler` core | REST API endpoints for admin token management |
| `AgentTokenAuthFilter` | `dag-scheduler-muserver` | JWT token validation filter for agent endpoints |
| `AgentConfiguration` changes | `dag-agent` | Add agent token configuration field |
| `DefaultAgentSchedulerClient` changes | `dag-agent` | Add Authorization header to requests |

### Database Schema

```sql
CREATE TABLE agent_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE NULL,
    revoked_by VARCHAR(100) NULL
);
```

### JWT Claims

JWT tokens will contain the following standard and custom claims:

- `iss`: Issuer (from config, application name)
- `sub`: Subject = "agent" (identifies this as an agent token)
- `jti`: JWT ID (unique identifier, matches `token_id` in database)
- `iat`: Issued at timestamp
- `exp`: Expiration timestamp
- `name`: Token name (human-readable)

### API Endpoints

All endpoints are under `API_VERSION + "/admin/tokens"` and require admin authentication via the existing UI JWT.

#### Generate Token

`POST /api/v1/admin/tokens/generate`

**Request Body:**
```json
{
  "name": "production-agent-1",
  "description": "Token for production agent pool 1",
  "expiresInDays": 365
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenId": "abc123...",
    "expiresAt": "2027-04-18T...",
    "name": "production-agent-1"
  }
}
```

#### List Tokens

`GET /api/v1/admin/tokens`

**Response:**
```json
{
  "success": true,
  "data": {
    "tokens": [
      {
        "tokenId": "abc123...",
        "name": "production-agent-1",
        "description": "Token for production agent pool 1",
        "createdBy": "admin",
        "createdAt": "2026-04-18T...",
        "expiresAt": "2027-04-18T...",
        "revoked": false
      }
    ]
  }
}
```

#### Revoke Token

`POST /api/v1/admin/tokens/{tokenId}/revoke`

**Response:**
```json
{
  "success": true,
  "message": "Token revoked successfully"
}
```

### Agent Client Changes

1. Add `agentToken` field to `AgentConfiguration`
2. When `DefaultAgentSchedulerClient` sends requests, add header:
   ```
   Authorization: Bearer ${agentToken}
   ```

### Authentication Flow

1. Admin generates token via management API
2. Admin configures the token value in agent's configuration file
3. Agent starts up and auto-registers with server
4. Server's `AgentTokenAuthFilter` intercepts request:
   - Extracts token from `Authorization` header
   - Verifies JWT signature using existing public key
   - Checks expiration
   - Checks database to see if token has been revoked
   - If all checks pass, allow request to proceed
   - If any check fails, return 401 Unauthorized

## Security Considerations

1. **Token Storage**: JWT tokens are signed but not encrypted. Don't put sensitive data in claims.
2. **Revocation**: JWT is stateless, but we check revocation status against the database on every protected request. This allows immediate revocation.
3. **Reuse Existing Keys**: Reuses the RSA key pair already configured for UI JWT, no new key material needed.
4. **Admin Access**: All token management endpoints are protected by the existing UI authentication filter - only logged-in admins can access them.

## Automatic Token Distribution (Self-Registration)

This design adds automated token distribution where agents can self-register on first startup, with admin approval workflow and optional whitelist auto-approval.

### Requirements for Automation

1. **Self-Registration**: Agent automatically registers on startup if no token is configured locally
2. **Dual Approval Modes**: Support whitelist auto-approval and manual approval for non-whitelisted agents
3. **Pattern Matching**: Whitelist supports glob pattern matching for agent names
4. **Token Push**: After approval, server pushes token to agent via callback
5. **Nonce Replay Protection**: Prevent replay attacks on callback without shared secret
6. **Local Persistence**: Agent saves received token to local config for reuse on restart
7. **Auto Cleanup**: Pending registrations expire after 24 hours and are automatically cleaned up

### Additional Components

| Component | Location | Responsibility |
|-----------|----------|----------------|
| `agent_registrations` table | Database | Store pending/approved/rejected registration requests |
| `RegistrationService` | `dag-scheduler` core | Handle registration logic, whitelist matching, approval workflow |
| `PublicRegistrationApi` | `dag-scheduler-muserver` | Public registration endpoint (no authentication required) |
| `RegistrationAdminApi` | `dag-scheduler-muserver` | Admin endpoints for listing, approving, rejecting |
| `AgentAutoRegistration` | `dag-agent` | Startup detection and auto-registration flow |
| `TokenCallbackHandler` | `dag-agent-muserver` | Endpoint to receive pushed token from server |

### Database Schema - agent_registrations

```sql
CREATE TABLE agent_registrations (
    id BIGSERIAL PRIMARY KEY,
    registration_id VARCHAR(64) NOT NULL UNIQUE,
    agent_name VARCHAR(255) NOT NULL,
    description TEXT,
    labels JSONB,
    callback_url TEXT NOT NULL,
    nonce VARCHAR(64) NOT NULL,
    client_address VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, APPROVED, REJECTED
    notes TEXT,
    processed_by VARCHAR(100) NULL,
    processed_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

Status meanings:
- `PENDING`: Waiting for admin approval
- `APPROVED`: Approved, token has been pushed
- `REJECTED`: Rejected by admin

### Configuration

**Server configuration (application.yml):**
```yaml
dag-task:
  auth:
    auto-approve:
      enabled: true
      patterns: # glob patterns for auto-approval
        - "prod-*"
        - "staging-*"
```

**Agent configuration:**
- If `agentToken` is already configured: skip auto-registration, use configured token
- If `agentToken` is empty/null: start auto-registration flow
- Agent saves received token to the local config file after successful registration

### Automated Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Agent Startup: Detect no local token                          │
│    Generate random 32-byte nonce                                 │
└────────────────────────────┘                                   
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Agent sends registration request to server:                   │
│    POST /api/v1/agent/register                                   │
│                                                                   │
│    {                                                              │
│      "name": "prod-worker-01",                                   │
│      "description": "Production worker node 01",                 │
│      "labels": {"env": "prod", "region": "us-east"},             │
│      "callbackUrl": "http://agent-01:8080/callback/token",       │
│      "nonce": "random-32-byte-nonce"                             │
│    }                                                              │
└────────────────────────────┘                                   
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Server creates registration with status = PENDING             │
│    Check whitelist patterns against agent name                   │
└────────────────────────────┘                                   
                             │
         ┌───────────────────┴───────────────────┐
         │                                       │
         ▼                                       ▼
┌───────────────────────┐           ┌───────────────────────────┐
│ Match Whitelist       │           │ No Whitelist Match        │
│ Auto-Approve → Step 4 │           │ Wait for Manual Approval  │
└───────────────────────┘           └───────────────────────────┘
                                                             │
                                                             ▼
                                                 ┌───────────────────────────┐
                                                 │  Admin reviews pending    │
                                                 │  → Approve / Reject       │
                                                 └───────────────────────────┘
                                                             │
                                                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. After Approval: Server generates agent token (as per original design) │
│    Update registration status = APPROVED                         │
│    Push token to agent callback URL:                             │
│    POST {callbackUrl}                                            │
│    Headers: X-Registration-Nonce: {nonce}                        │
│    Body: { "token": "eyJhbGci...", "tokenId": "...", ... }      │
└────────────────────────────┘                                   
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Agent receives callback:                                     │
│    Verify Nonce matches the one sent in registration             │
│    If match: save token to local config file → start normal operation │
│    If not match: reject request                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Nonce Replay Protection

- Agent generates a fresh random nonce for each registration attempt
- Nonce is stored with the registration record on server
- Server must include the exact nonce in the `X-Registration-Nonce` header when pushing token
- Agent only accepts the callback if nonce matches the pending registration
- This prevents replay attacks without requiring shared secrets

### API Endpoints

#### Public Registration Endpoints (No Authentication)

**Register Agent**
`POST /api/v1/agent/register`

Request body:
```json
{
  "name": "prod-worker-01",
  "description": "Production worker node 01",
  "labels": {
    "environment": "production",
    "region": "us-east-1"
  },
  "callbackUrl": "http://192.168.1.100:8080/callback/token"
}
```

Response:
```json
{
  "success": true,
  "data": {
    "registrationId": "a1b2c3...",
    "status": "PENDING",
    "message": "Registration submitted, waiting for approval"
  }
}
```

When auto-approved:
```json
{
  "success": true,
  "data": {
    "registrationId": "a1b2c3...",
    "status": "APPROVED",
    "message": "Registration auto-approved via whitelist, token will be pushed to your callback"
  }
}
```

#### Admin Registration Endpoints (Requires Admin Authentication)

**List Registrations**
`GET /api/v1/admin/registrations?status=PENDING&page=0&size=20`

**Approve Registration**
`POST /api/v1/admin/registrations/{registrationId}/approve`

Request body (optional):
```json
{
  "notes": "Approved for production use",
  "expiresInDays": 365
}
```

**Reject Registration**
`POST /api/v1/admin/registrations/{registrationId}/reject`

Request body:
```json
{
  "notes": "Unauthorized agent, rejected"
}
```

#### Agent Callback Endpoint (Receives Token)

**Receive Token**
`POST /callback/token`

This endpoint is started by the agent when it begins auto-registration.

Headers:
```
X-Registration-Nonce: {nonce}
```

Body:
```json
{
  "registrationId": "a1b2c3...",
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenId": "abc123...",
  "expiresAt": "2027-04-18T10:00:00Z",
  "name": "prod-worker-01"
}
```

Response:
```json
{
  "success": true,
  "message": "Token saved successfully"
}
```

### Expired Pending Registration Cleanup

- All pending registrations have an `expires_at` set to `created_at + 24 hours`
- A scheduled job on the server runs periodically (daily) to delete expired pending registrations
- This prevents the table from accumulating stale pending requests

## Implementation Steps

1. Add DTOs to `dag-si` for registration requests/responses
2. Add database migration for `agent_registrations` table
3. Add DAO interface and implementation for `agent_registrations`
4. Add whitelist pattern matching configuration in `dag-scheduler`
5. Add `RegistrationService` with:
   - create registration
   - whitelist checking
   - approve/reject
   - token push via callback
   - cleanup expired registrations
6. Add `PublicRegistrationApi` public REST endpoint
7. Add `RegistrationAdminApi` admin REST endpoints
8. Add `AgentAutoRegistration` component in `dag-agent` for startup auto-registration flow
9. Add `TokenCallbackHandler` endpoint in `dag-agent-muserver`
10. Add token persistence to local config file in `dag-agent`
11. Add scheduled cleanup job for expired pending registrations
12. Update Flyway migration with both `agent_tokens` and `agent_registrations` tables
