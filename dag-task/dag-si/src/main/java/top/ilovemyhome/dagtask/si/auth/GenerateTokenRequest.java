package top.ilovemyhome.dagtask.si.auth;

public record GenerateTokenRequest(
    String name,
    String description,
    int expiresInDays
) {}
