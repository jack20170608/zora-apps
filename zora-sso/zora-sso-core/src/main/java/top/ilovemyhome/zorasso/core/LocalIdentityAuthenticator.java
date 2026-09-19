package top.ilovemyhome.zorasso.core;

import top.ilovemyhome.zorasso.si.AuthenticatedIdentity;
import top.ilovemyhome.zorasso.si.IdentityAuthenticator;

import java.time.Clock;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class LocalIdentityAuthenticator implements IdentityAuthenticator {

    public LocalIdentityAuthenticator(Iterable<LocalUser> users, PasswordHasher passwordHasher, Clock clock) {
        this.users = java.util.stream.StreamSupport.stream(users.spliterator(), false)
            .collect(Collectors.toUnmodifiableMap(LocalUser::username, Function.identity()));
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public AuthenticatedIdentity authenticate(String username, char[] password) {
        LocalUser user;
        try {
            user = users.get(LocalUser.normalizeUsername(username));
        } catch (IllegalArgumentException exception) {
            user = null;
        }
        String hash = user == null ? DUMMY_HASH : user.passwordHash();
        boolean verified = passwordHasher.verify(password, hash);
        if (user == null || !user.enabled() || !verified) {
            return null;
        }
        return new AuthenticatedIdentity(
            user.subject(),
            user.username(),
            user.displayName(),
            user.roles(),
            clock.instant()
        );
    }

    private static final String DUMMY_HASH =
        "pbkdf2-sha256$100000$AAAAAAAAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    private final Map<String, LocalUser> users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;
}
