package top.ilovemyhome.dagtask.allinone.muserver;

import com.typesafe.config.Config;
import io.muserver.MuServer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.allinone.muserver.application.AllInOneAppContext;
import top.ilovemyhome.dagtask.allinone.muserver.application.AllInOneWebServerBootstrap;
import top.ilovemyhome.zora.config.ConfigLoader;

/**
 * Main entry point for the DAG Task AllInOne server.
 * Combines scheduler, admin, and agent into a single JVM process
 * with a unified HTTP server.
 */
public class AllInOneApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllInOneApp.class);

    /**
     * Starts the all-in-one server.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        LOGGER.info("Starting DAG Task AllInOne server...");

        String env = System.getProperty("env", "dev");
        if (StringUtils.isBlank(env)) {
            throw new IllegalStateException("System property 'env' is required. Set it with -Denv=dev");
        }

        Config config = ConfigLoader.loadConfig("config/application.conf", "config/application-" + env + ".conf");

        AllInOneAppContext appContext = new AllInOneAppContext(env, config);
        appContext.start();

        MuServer muServer = AllInOneWebServerBootstrap.start(appContext);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown hook triggered, stopping AllInOne server...");
            muServer.stop();
            appContext.stop();
            LOGGER.info("AllInOne server stopped");
        }));

        LOGGER.info("DAG Task AllInOne server fully started at {}", muServer.uri());

        keepAlive();
    }

    /**
     * Keeps the main thread alive so the JVM does not exit.
     * Interrupted when the shutdown hook runs.
     */
    private static void keepAlive() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
