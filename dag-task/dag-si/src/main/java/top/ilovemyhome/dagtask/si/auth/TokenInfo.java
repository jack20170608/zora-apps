package top.ilovemyhome.dagtask.si.auth;

import java.time.Instant;

public record TokenInfo(
    String tokenId,
    String name,
    String description,
    String createdBy,
    Instant createdAt,
    Instant expiresAt,
    boolean revoked
) {}
