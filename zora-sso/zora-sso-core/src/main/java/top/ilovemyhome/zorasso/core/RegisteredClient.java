package top.ilovemyhome.zorasso.core;

import java.net.URI;
import java.util.Set;

public record RegisteredClient(
    String clientId,
    String clientSecretHash,
    Set<URI> redirectUris,
    boolean enabled
) {
    public RegisteredClient {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        if (clientSecretHash == null || clientSecretHash.isBlank()) {
            throw new IllegalArgumentException("clientSecretHash must not be blank");
        }
        redirectUris = Set.copyOf(redirectUris);
        if (redirectUris.isEmpty() || redirectUris.stream().anyMatch(uri -> !uri.isAbsolute() || uri.getFragment() != null)) {
            throw new IllegalArgumentException("redirectUris must contain absolute URIs without fragments");
        }
    }
}
