package top.ilovemyhome.dagtask.si.auth;

import java.util.Map;

public record AgentRegistrationRequest(
    String name,
    String description,
    Map<String, String> labels,
    String callbackUrl
) {}
