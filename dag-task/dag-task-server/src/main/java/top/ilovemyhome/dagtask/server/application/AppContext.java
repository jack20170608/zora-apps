package top.ilovemyhome.dagtask.server.application;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.server.inject.DagTaskServerModule;
import top.ilovemyhome.zora.rdb.config.RdbConfig;
import top.ilovemyhome.zora.rdb.flyway.FlywayMigrationRunner;
import top.ilovemyhome.zora.rdb.pool.DataSourcePoolBuilder;

import javax.sql.DataSource;

public final class AppContext {

    private final Injector injector;
    private final String env;
    private final Config config;
    private DataSource dataSource;

    public AppContext(String env, Config config) {
        this.env = env;
        this.config = config;
        this.injector = Guice.createInjector(new DagTaskServerModule(config));
        initRdb(config);
        runFlywayMigration(config);
    }


    private void initRdb(Config config) {
        Config dbConfig = config.getConfig("database");
        RdbConfig rdbConfig = RdbConfig.builder()
                .jdbcUrl(dbConfig.getString("jdbcUrl"))
                .username(dbConfig.getString("username"))
                .password(dbConfig.getString("password"))
                .driverClassName(dbConfig.getString("driverClassName"))
                .maximumPoolSize(dbConfig.getInt("maximumPoolSize"))
                .minimumIdle(dbConfig.getInt("minimumIdle"))
                .autoCommit(dbConfig.getBoolean("autoCommit"))
                .readOnly(dbConfig.getBoolean("readOnly"))
                .build();
        this.dataSource = DataSourcePoolBuilder.create(rdbConfig).build();
        logger.info("DataSource initialized successfully");
    }

    private void runFlywayMigration(Config config) {
        Config flywayConfig = config.getConfig("flyway");
        FlywayMigrationRunner flywayMigrationRunner = FlywayMigrationRunner.builder(dataSource)
                .locations(flywayConfig.getStringList("locations").toArray(String[]::new))
                .baselineOnMigrate(flywayConfig.getBoolean("baselineOnMigrate"))
                .baselineVersion(flywayConfig.getString("baselineVersion"))
                .baselineDescription(flywayConfig.getString("baselineDescription"))
                .table(flywayConfig.getString("table"))
                .defaultSchema(flywayConfig.getString("defaultSchema"))
                .build();
        flywayMigrationRunner.migrate();
        logger.info("Flyway migration completed successfully");
    }

    public Injector getInjector() {
        return injector;
    }

    public String getEnv() {
        return env;
    }

    public Config getConfig() {
        return config;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);
}
