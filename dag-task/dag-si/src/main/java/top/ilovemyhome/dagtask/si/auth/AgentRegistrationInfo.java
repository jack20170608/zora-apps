package top.ilovemyhome.dagtask.si.auth;

import java.time.Instant;

public record AgentRegistrationInfo(
    String registrationId,
    String agentName,
    String description,
    String status,
    String clientAddress,
    Instant createdAt,
    Instant expiresAt,
    String processedBy,
    Instant processedAt,
    String notes
) {}
