package top.ilovemyhome.dagtask.server;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Local application starter that starts an embedded PostgreSQL before the actual application starts.
 * Useful for local development and testing.
 */
public class AppLocal {

    static EmbeddedPostgres embeddedPostgres = null;

    static void main(String[] args) {
//        try {
//            logger.info("Starting embedded PostgreSQL...");
//            embeddedPostgres = EmbeddedPostgres.builder()
//                .setPort(33333)
//                .setPGStartupWait(Duration.ofMinutes(1))
//                .start();
//            logger.info("Embedded PostgreSQL started with jdbc url = [{}].", embeddedPostgres.getJdbcUrl("postgres", "postgres"));
//
//            try (var connection = embeddedPostgres.getPostgresDatabase().getConnection()) {
//                try (var statement = connection.createStatement()) {
//                    // Check if database exists, create if not
//                    var rs = statement.executeQuery("select version() as version ;");
//                    while (rs.next()) {
//                        logger.info(rs.getString("version"));
//                    }
//                }
//            } catch (SQLException e) {
//                logger.error("Failed to connect database.", e);
//            }
//            logger.info("Embedded PostgreSQL started");
        // Set system properties for configuration to override database connection
        System.setProperty("env", "local");

        // Add shutdown hook to close embedded PostgreSQL when JVM exits
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            logger.info("Shutdown hook triggered, stopping embedded PostgreSQL...");
//            try {
//                if (embeddedPostgres != null) {
//                    embeddedPostgres.close();
//                    logger.info("Embedded PostgreSQL stopped successfully");
//                }
//            } catch (IOException e) {
//                logger.error("Failed to stop embedded PostgreSQL in shutdown hook", e);
//            }
//        }, "embedded-postgres-shutdown-hook"));
//
//        logger.info("Starting main application...");
        App.main(args);
//        } catch (IOException ioe) {
//            logger.error("Failed to start embedded PostgreSQL", ioe);
//            System.exit(1);
//        }
    }

    private static final Logger logger = LoggerFactory.getLogger(AppLocal.class);
}
