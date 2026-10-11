package top.ilovemyhome.zorasso.muserver.application;

import com.typesafe.config.Config;
import top.ilovemyhome.zorasso.core.InMemoryLocalSsoService;
import top.ilovemyhome.zorasso.core.LocalIdentityAuthenticator;
import top.ilovemyhome.zorasso.core.Pbkdf2PasswordHasher;
import top.ilovemyhome.zorasso.muserver.config.SsoConfiguration;
import top.ilovemyhome.zorasso.si.SsoService;

import java.time.Clock;

public final class AppContext {

    public AppContext(String environment, Config config) {
        this.environment = environment;
        this.config = config;
    }

    public synchronized void init() {
        if (configuration != null) {
            return;
        }
        configuration = SsoConfiguration.from(config);
        Pbkdf2PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        LocalIdentityAuthenticator authenticator = new LocalIdentityAuthenticator(
            configuration.users(),
            passwordHasher,
            Clock.systemUTC()
        );
        ssoService = new InMemoryLocalSsoService(
            authenticator,
            passwordHasher,
            configuration.clients()
        );
    }

    public String environment() {
        return environment;
    }

    public Config config() {
        return config;
    }

    public SsoConfiguration configuration() {
        if (configuration == null) {
            throw new IllegalStateException("Application context has not been initialized");
        }
        return configuration;
    }

    public SsoService ssoService() {
        if (ssoService == null) {
            throw new IllegalStateException("Application context has not been initialized");
        }
        return ssoService;
    }

    private final String environment;
    private final Config config;
    private SsoConfiguration configuration;
    private SsoService ssoService;
}
