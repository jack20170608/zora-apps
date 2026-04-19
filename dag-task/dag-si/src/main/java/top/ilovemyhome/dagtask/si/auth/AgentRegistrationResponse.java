package top.ilovemyhome.dagtask.si.auth;

public record AgentRegistrationResponse(
    boolean success,
    Data data,
    String message
) {
    public record Data(
        String registrationId,
        String status,
        String message
    ) {}
}
