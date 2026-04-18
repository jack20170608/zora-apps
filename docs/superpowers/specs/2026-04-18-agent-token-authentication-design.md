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

## Implementation Steps

1. Add DTOs to `dag-si` for token requests/responses
2. Add database migration for `agent_tokens` table
3. Add DAO interface and implementation for `agent_tokens`
4. Add `TokenService` with generate/validate/revoke/list operations
5. Add `TokenManagementApi` REST endpoints
6. Add `AgentTokenAuthFilter` in `dag-scheduler-muserver` to validate tokens on agent endpoints
7. Update `AgentConfiguration` in `dag-agent` to add `agentToken` field
8. Update `DefaultAgentSchedulerClient` to send Authorization header
9. Update Flyway migration with the new table DDL
