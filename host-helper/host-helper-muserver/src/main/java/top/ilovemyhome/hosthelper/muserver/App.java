package top.ilovemyhome.hosthelper.muserver;

import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.hosthelper.muserver.application.AppContext;
import top.ilovemyhome.hosthelper.muserver.application.WebServerBootstrap;
import top.ilovemyhome.hosthelper.muserver.util.config.ConfigLoader;

import java.nio.file.Path;

public class App {

    public static void main(String[] args) {
        LOGGER.info("Starting application.");
        String env = System.getenv("APP_ENV");
        if (StringUtils.isBlank(env)) {
            env = System.getenv("env");
        }
        if (StringUtils.isBlank(env)){
            throw new IllegalStateException("Cannot find env property.");
        }
        if (!env.matches("[a-zA-Z0-9][a-zA-Z0-9_-]*")) {
            throw new IllegalStateException("Invalid env property.");
        }
        App app = new App();
        app.initAppContext(env);
        app.initWebServer(app.getAppContext());
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public static App getInstance() {
        return APP;
    }

    private App() {
    }

    private void initAppContext(String env){
        String rootConfig = "config/application.conf";
        String envConfig = "config/application-" + env + ".conf";
        String configDir = System.getenv("HOST_HELPER_CONFIG_DIR");
        Config config = StringUtils.isBlank(configDir)
            ? ConfigLoader.loadConfig(rootConfig, envConfig)
            : ConfigLoader.loadConfig(rootConfig, Path.of(configDir, "application-" + env + ".conf"));
        this.appContext = new AppContext(env, config);
        this.appContext.init();
    }

    private void initWebServer(AppContext appContext){
        WebServerBootstrap.start(appContext);
    }

    private AppContext appContext;
    private static App APP;
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

}
