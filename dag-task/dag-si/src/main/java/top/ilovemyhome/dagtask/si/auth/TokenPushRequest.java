package top.ilovemyhome.dagtask.si.auth;

import java.time.Instant;

public record TokenPushRequest(
    String registrationId,
    String token,
    String tokenId,
    Instant expiresAt,
    String name
) {}
