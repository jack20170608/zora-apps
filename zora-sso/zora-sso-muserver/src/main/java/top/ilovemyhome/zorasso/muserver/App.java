package top.ilovemyhome.zorasso.muserver;

import com.typesafe.config.ConfigFactory;
import io.muserver.MuServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zorasso.core.InMemoryLocalSsoService;
import top.ilovemyhome.zorasso.core.LocalIdentityAuthenticator;
import top.ilovemyhome.zorasso.core.Pbkdf2PasswordHasher;

import java.time.Clock;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        SsoConfiguration configuration = SsoConfiguration.from(ConfigFactory.load());
        Pbkdf2PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        LocalIdentityAuthenticator authenticator = new LocalIdentityAuthenticator(
            configuration.users(),
            passwordHasher,
            Clock.systemUTC()
        );
        InMemoryLocalSsoService ssoService = new InMemoryLocalSsoService(
            authenticator,
            passwordHasher,
            configuration.clients()
        );
        MuServer server = new SsoWebServer(configuration, ssoService).start();
        LOGGER.info("Zora SSO local POC started at {}", server.uri());
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
}
