package top.ilovemyhome.zorasso.si;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record AuthenticatedIdentity(
    String subject,
    String username,
    String displayName,
    Set<String> roles,
    Instant authenticatedAt
) {
    public AuthenticatedIdentity {
        subject = requireText(subject, "subject");
        username = requireText(username, "username");
        displayName = requireText(displayName, "displayName");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        authenticatedAt = Objects.requireNonNull(authenticatedAt, "authenticatedAt");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
