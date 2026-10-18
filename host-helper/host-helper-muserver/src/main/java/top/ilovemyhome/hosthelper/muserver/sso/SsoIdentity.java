package top.ilovemyhome.hosthelper.muserver.sso;

import java.time.Instant;
import java.util.Set;

public record SsoIdentity(
    String subject,
    String username,
    String displayName,
    Set<String> roles,
    Instant authenticatedAt
) {
    public SsoIdentity {
        subject = requireText(subject, "subject");
        username = requireText(username, "username");
        displayName = requireText(displayName, "displayName");
        if (roles == null || roles.stream().anyMatch(role -> role == null || role.isBlank())) {
            throw new IllegalArgumentException("roles must contain non-blank values");
        }
        roles = Set.copyOf(roles);
        if (authenticatedAt == null) {
            throw new IllegalArgumentException("authenticatedAt must not be null");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
