package top.ilovemyhome.zorasso.muserver;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import io.muserver.MuServer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zora.config.ConfigLoader;
import top.ilovemyhome.zorasso.core.InMemoryLocalSsoService;
import top.ilovemyhome.zorasso.core.LocalIdentityAuthenticator;
import top.ilovemyhome.zorasso.core.Pbkdf2PasswordHasher;
import top.ilovemyhome.zorasso.muserver.application.AppContext;
import top.ilovemyhome.zorasso.muserver.application.SsoWebServer;
import top.ilovemyhome.zorasso.muserver.config.SsoConfiguration;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;

public final class App {

    private App() {
    }

    public AppContext getAppContext() {
        return appContext;
    }

    static void main(String[] args) {
        LOGGER.info("Starting application.");
        String env = System.getenv("APP_ENV");
        if (StringUtils.isBlank(env)){
            throw new IllegalStateException("Cannot find the env variable.");
        }
        if (!env.matches("[a-zA-Z0-9][a-zA-Z0-9_-]*")) {
            throw new IllegalStateException("Invalid env property.");
        }
        App app = new App();
        app.initAppContext(env);
        app.initWebServer(app.getAppContext());
    }


    static void main2(String[] args) {
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

    private void initWebServer(AppContext appContext){
        WebServerBootstrap.start(appContext);
    }

    private void initAppContext(String env){
        String rootConfig = "config/application.conf";
        String envConfig = "config/application-" + env + ".conf";
        String configDir = System.getenv("ZORA_SSO_CONFIG_DIR");
        Config config = StringUtils.isBlank(configDir)
            ? ConfigLoader.loadConfig(rootConfig, envConfig)
            : ConfigLoader.loadConfig(rootConfig, Path.of(configDir, "application-" + env + ".conf"));
        this.appContext = new AppContext(env, config);
        this.appContext.init();
    }

    private AppContext appContext;

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
}
