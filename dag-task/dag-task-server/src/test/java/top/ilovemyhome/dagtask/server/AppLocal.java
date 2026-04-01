package top.ilovemyhome.dagtask.server;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;

/**
 * Local application starter that starts an embedded PostgreSQL before the actual application starts.
 * Useful for local development and testing.
 */
public class AppLocal {

    private static final String DEFAULT_DB_NAME = "dag_task_test";
    private static final String DEFAULT_PG_VERSION = "16.8";

    public static void main(String[] args) {
        EmbeddedPostgres embeddedPostgres = null;
        try {
            logger.info("Starting embedded PostgreSQL...");
            // Set LC_ALL to C to avoid Chinese output causing garbled characters
            System.setProperty("LC_ALL", "C");
            System.setProperty("LC_CTYPE", "C");

            String pgVersion = DEFAULT_PG_VERSION;
            String dbName = DEFAULT_DB_NAME;

            if (args.length >= 2) {
                pgVersion = args[0];
                dbName = args[1];
            } else if (args.length == 1) {
                dbName = args[0];
            }

            embeddedPostgres = EmbeddedPostgres.builder()
                .setPgVersion(pgVersion)
                .setPort(33333)
                .setPGStartupWait(Duration.ofMinutes(1))
                .start();
            logger.info("Embedded PostgreSQL started with jdbc url = [{}]."
                , embeddedPostgres.getJdbcUrl("postgres", "postgres"));

            // Create the database if it doesn't exist
            try (var connection = embeddedPostgres.getPostgresDatabase().getConnection()) {
                try (var statement = connection.createStatement()) {
                    // Check if database exists, create if not
                    statement.execute(String.format(
                        "SELECT 'CREATE DATABASE %s' " +
                        "WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '%s')",
                        dbName, dbName));
                }
            } catch (SQLException e) {
                logger.warn("Failed to create database '{}', it may already exist: {}",
                    dbName, e.getMessage());
            }

            String jdbcUrl = String.format(
                "jdbc:postgresql://localhost:%d/%s?user=postgres&password=postgres",
                embeddedPostgres.getPort(), dbName);

            logger.info("Embedded PostgreSQL started");
            logger.info("PostgreSQL version: {}", pgVersion);
            logger.info("Database name: {}", dbName);
            logger.info("JDBC URL: {}", jdbcUrl);

            // Set system properties for configuration to override database connection
            System.setProperty("env", "local");
            System.setProperty("database.jdbcUrl", jdbcUrl);
            System.setProperty("database.username", "postgres");
            System.setProperty("database.password", "postgres");

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
