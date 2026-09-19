package top.ilovemyhome.zorasso.core;

import java.util.Objects;
import java.util.Set;

public record LocalUser(
    String subject,
    String username,
    String displayName,
    String passwordHash,
    Set<String> roles,
    boolean enabled
) {
    public LocalUser {
        subject = requireText(subject, "subject");
        username = normalizeUsername(username);
        displayName = requireText(displayName, "displayName");
        passwordHash = requireText(passwordHash, "passwordHash");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
    }

    public static String normalizeUsername(String username) {
        return requireText(username, "username").trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
