package top.ilovemyhome.hosthelper.muserver.sso;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class SsoIdentityTest {

    @Test
    void rejectsIncompleteIdentity() {
        Instant now = Instant.parse("2026-10-11T00:00:00Z");

        assertThrowsExactly(IllegalArgumentException.class,
            () -> new SsoIdentity(null, "alice", "Alice", Set.of("USER"), now));
        assertThrowsExactly(IllegalArgumentException.class,
            () -> new SsoIdentity("user-1", " ", "Alice", Set.of("USER"), now));
        assertThrowsExactly(IllegalArgumentException.class,
            () -> new SsoIdentity("user-1", "alice", null, Set.of("USER"), now));
        assertThrowsExactly(IllegalArgumentException.class,
            () -> new SsoIdentity("user-1", "alice", "Alice", null, now));
        assertThrowsExactly(IllegalArgumentException.class,
            () -> new SsoIdentity("user-1", "alice", "Alice", Set.of(" "), now));
        assertThrowsExactly(IllegalArgumentException.class,
            () -> new SsoIdentity("user-1", "alice", "Alice", Set.of("USER"), null));
    }

    @Test
    void copiesRolesToImmutableSet() {
        Set<String> roles = new HashSet<>(Set.of("USER"));
        SsoIdentity identity = new SsoIdentity(
            "user-1", "alice", "Alice", roles, Instant.parse("2026-10-11T00:00:00Z")
        );

        roles.add("ADMIN");

        assertEquals(Set.of("USER"), identity.roles());
        assertThrowsExactly(UnsupportedOperationException.class, () -> identity.roles().add("ADMIN"));
    }
}
