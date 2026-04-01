package top.ilovemyhome.dagtask.server.application;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.server.inject.DagTaskServerModule;
import top.ilovemyhome.zora.rdb.flyway.FlywayMigrationRunner;

import javax.sql.DataSource;

public final class AppContext {

    private final Injector injector;
    private final String env;
    private final Config config;
    private final DataSource dataSource;

    public AppContext(String env, Config config) {
        this.env = env;
        this.config = config;
        this.dataSource =
        this.injector = Guice.createInjector(new DagTaskServerModule(config));
        FlywayMigrationRunner flywayMigrationRunner = FlywayMigrationRunner.builder(

        ).build()
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


    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);
}
