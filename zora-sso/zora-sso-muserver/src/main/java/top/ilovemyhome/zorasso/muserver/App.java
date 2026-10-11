package top.ilovemyhome.zorasso.muserver;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import io.muserver.MuServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zorasso.core.InMemoryLocalSsoService;
import top.ilovemyhome.zorasso.core.LocalIdentityAuthenticator;
import top.ilovemyhome.zorasso.core.Pbkdf2PasswordHasher;
import top.ilovemyhome.zorasso.muserver.application.SsoWebServer;
import top.ilovemyhome.zorasso.muserver.config.SsoConfiguration;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;

public final class App {

    private App() {
    }

    static void main(String[] args) {
        Map<String, String> environmentVariables = System.getenv();
        String environment = SsoConfigLoader.requireEnvironment(environmentVariables);
        String rootConfig = "application.conf";
        String environmentConfig = "application-" + environment + ".conf";
        String configDirectory = environmentVariables.get("ZORA_SSO_CONFIG_DIR");
        Config config = configDirectory == null || configDirectory.isBlank()
            ? SsoConfigLoader.loadConfig(rootConfig, environmentConfig)
            : SsoConfigLoader.loadConfig(rootConfig, Path.of(configDirectory, environmentConfig));
        config = config.withValue("zora-sso.environment", ConfigValueFactory.fromAnyRef(environment));

        SsoConfiguration configuration = SsoConfiguration.from(config);
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
