package top.ilovemyhome.dagtask.si.auth;

import java.time.Instant;

public record GenerateTokenResponse(
    boolean success,
    Data data,
    String message
) {
    public record Data(
        String token,
        String tokenId,
        Instant expiresAt,
        String name
    ) {}
}
