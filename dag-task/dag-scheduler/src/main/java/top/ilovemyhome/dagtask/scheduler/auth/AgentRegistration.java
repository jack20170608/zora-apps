package top.ilovemyhome.dagtask.scheduler.auth;

import java.time.Instant;

public record AgentRegistration(
    Long id,
    String registrationId,
    String agentName,
    String description,
    String labelsJson,
    String callbackUrl,
    String nonce,
    String clientAddress,
    Status status,
    String notes,
    String processedBy,
    Instant processedAt,
    Instant createdAt,
    Instant expiresAt
) {
    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }
}
