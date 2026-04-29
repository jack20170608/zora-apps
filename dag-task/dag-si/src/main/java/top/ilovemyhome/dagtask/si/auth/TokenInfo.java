package top.ilovemyhome.dagtask.si.auth;

import java.time.Instant;

/**
 * Token information DTO containing all fields from {@link AgentToken} except revocation info,
 * plus the actual JWT token string for generation responses.
 */
public record TokenInfo(
    Long id,
    String tokenId,
    String agentId,
    String name,
    String description,
    String createdBy,
    Instant createdAt,
    Instant expiresAt,
    String token
) {
}
