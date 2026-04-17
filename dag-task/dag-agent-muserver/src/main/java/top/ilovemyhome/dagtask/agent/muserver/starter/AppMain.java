package top.ilovemyhome.dagtask.agent.muserver.starter;

import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.muserver.application.AppContext;
import top.ilovemyhome.dagtask.agent.muserver.application.WebServerBootstrap;
import top.ilovemyhome.zora.config.ConfigLoader;

/**
 * Main entry point for the MuServer-hosted DAG Task Agent.
 * Starts the agent core and the MuServer HTTP server.
 */
public class AppMain {

    public static void main(String[] args) {
        LOGGER.info("Starting DAG Task Agent with MuServer...");

        String env = System.getProperty("env");
        if (StringUtils.isBlank(env)) {
            throw new IllegalStateException("System property 'env' is required. Set it with -Denv=local");
        }

        AppContext appContext = createAppContext(env);
        startAgent(appContext);
        startWebServer(appContext);

        LOGGER.info("DAG Task Agent fully started");
    }

    private static AppContext createAppContext(String env) {
        String rootConfig = "config/application.conf";
        String envConfig = "config/agent-muserver-" + env + ".conf";
        Config config = ConfigLoader.loadConfig(rootConfig, envConfig);
        return new AppContext(env, config);
    }

    private static void startAgent(AppContext appContext) {
        appContext.getDagTaskAgent().start();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Received shutdown signal, stopping DAG Task Agent...");
            appContext.getDagTaskAgent().stop();
            LOGGER.info("DAG Task Agent stopped");
        }));
    }

    private static void startWebServer(AppContext appContext) {
        WebServerBootstrap.start(appContext);
    }

    private AppMain() {
        // Private constructor for utility class
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AppMain.class);
}
