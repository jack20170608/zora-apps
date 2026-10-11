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
}
