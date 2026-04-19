package top.ilovemyhome.dagtask.scheduler.token;

import java.time.Instant;

public record AgentToken(
    Long id,
    String tokenId,
    String name,
    String description,
    String createdBy,
    Instant createdAt,
    Instant expiresAt,
    boolean revoked,
    Instant revokedAt,
    String revokedBy
) {}
