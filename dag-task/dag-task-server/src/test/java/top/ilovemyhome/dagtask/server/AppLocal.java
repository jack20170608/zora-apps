package top.ilovemyhome.dagtask.server;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

/**
 * Local application starter that starts an embedded PostgreSQL before the actual application starts.
 * Useful for local development and testing.
 */
public class AppLocal {

    private static final String DEFAULT_DB_NAME = "dag_task_test";

    public static void main(String[] args) {
        EmbeddedPostgres embeddedPostgres = null;
        try {
            logger.info("Starting embedded PostgreSQL...");
            // Set LC_ALL to C to avoid Chinese output causing garbled characters
            System.setProperty("LC_ALL", "C");
            System.setProperty("LC_CTYPE", "C");

            embeddedPostgres = EmbeddedPostgres.builder()
                .setPort(33333)
                .setPGStartupWait(Duration.ofMinutes(1))
                .start();
            logger.info("Embedded PostgreSQL started with jdbc url = [{}]."
                , embeddedPostgres.getJdbcUrl("postgres", "postgres"));

            // Set system properties for configuration to override database connection
            System.setProperty("env", "local");
            logger.info("Starting main application...");
            App.main(args);
        } catch (IOException ioe) {
            logger.error("Failed to start embedded PostgreSQL", ioe);
            System.exit(1);
        } finally {
            // Note: The embedded postgres will be automatically stopped when JVM exits
            // This close is just for graceful shutdown if App exits normally
            if (embeddedPostgres != null) {
                try {
                    logger.info("Stopping embedded PostgreSQL...");
                    embeddedPostgres.close();
                } catch (IOException e) {
                    logger.error("Failed to stop embedded PostgreSQL", e);
                }
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(AppLocal.class);
}
