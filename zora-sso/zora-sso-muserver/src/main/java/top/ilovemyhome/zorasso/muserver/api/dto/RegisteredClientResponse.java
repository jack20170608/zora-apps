package top.ilovemyhome.zorasso.muserver.api.dto;

import top.ilovemyhome.zorasso.core.RegisteredClient;

import java.util.List;

public record RegisteredClientResponse(
    String clientId,
    List<String> redirectUris,
    boolean enabled
) {
    public static RegisteredClientResponse from(RegisteredClient client) {
        return new RegisteredClientResponse(
            client.clientId(),
            client.redirectUris().stream()
                .map(Object::toString)
                .sorted()
                .toList(),
            client.enabled()
        );
    }

    public RegisteredClientResponse {
        redirectUris = List.copyOf(redirectUris);
    }
}
