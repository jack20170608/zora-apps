package top.ilovemyhome.dagtask.allinone.muserver.database;

import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Initializes shared database resources for the all-in-one server.
 * Creates a single DataSource, Jdbi instance, and runs Flyway migrations.
 */
public class DatabaseBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBootstrap.class);

    private final Config config;
    private HikariDataSource dataSource;
    private Jdbi jdbi;

    public DatabaseBootstrap(Config config) {
        this.config = config;
    }

    /**
     * Initializes DataSource, Jdbi, and runs Flyway migrations.
     *
     * @return the initialized Jdbi instance
     */
    public Jdbi start() {
        LOGGER.info("Initializing database connection...");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getString("database.url"));
        hikariConfig.setUsername(config.getString("database.username"));
        hikariConfig.setPassword(config.getString("database.password"));
        hikariConfig.setMaximumPoolSize(config.getInt("database.pool.maxSize"));
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setPoolName("AllInOneHikariPool");

        this.dataSource = new HikariDataSource(hikariConfig);
        LOGGER.info("DataSource created with max pool size: {}", hikariConfig.getMaximumPoolSize());

        this.jdbi = Jdbi.create(dataSource);
        this.jdbi.installPlugin(new SqlObjectPlugin());
        LOGGER.info("Jdbi initialized with SqlObjectPlugin");

        // Run Flyway migrations
        String migrationLocation = config.hasPath("flyway.location")
            ? config.getString("flyway.location")
            : "db/migration/postgresql";

        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(migrationLocation)
            .load();

        int migrations = flyway.migrate().migrationsExecuted;
        LOGGER.info("Flyway migration completed: {} migrations executed", migrations);

        return jdbi;
    }

    /**
     * Gracefully shuts down the database resources.
     */
    public void stop() {
        LOGGER.info("Shutting down database connection...");
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("DataSource closed");
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Jdbi getJdbi() {
        return jdbi;
    }
}
