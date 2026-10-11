package top.ilovemyhome.zorasso.muserver;

import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zora.config.ConfigLoader;
import top.ilovemyhome.zorasso.muserver.application.AppContext;
import top.ilovemyhome.zorasso.muserver.application.SsoWebServer;

import java.nio.file.Path;

public final class App {

    private App() {
    }

    static void main(String[] args) {
        LOGGER.info("Starting application.");
        var  environment = System.getenv("env");
        if (StringUtils.isBlank(environment)) {
            throw new IllegalStateException("Cannot find env environment variable");
        }
        if (!environment.matches("[a-zA-Z0-9][a-zA-Z0-9_-]*")) {
            throw new IllegalStateException("Invalid environment variable");
        }

        String rootConfig = "application.conf";
        String environmentConfig = "application-" + environment + ".conf";
        String configDirectory = System.getenv("ZORA_SSO_CONFIG_DIR");
        Config config = StringUtils.isBlank(configDirectory)
            ? ConfigLoader.loadConfig(rootConfig, environmentConfig)
            : ConfigLoader.loadConfig(rootConfig, Path.of(configDirectory, environmentConfig));
        AppContext appContext = new AppContext(environment, config);
        appContext.init();
        var server = SsoWebServer.start(appContext);
        LOGGER.info("Zora SSO local POC started at {}", server.uri());
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
}
