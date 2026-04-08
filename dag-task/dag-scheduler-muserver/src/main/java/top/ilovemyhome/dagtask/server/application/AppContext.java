package top.ilovemyhome.dagtask.server.application;

import com.typesafe.config.Config;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.DagSchedulerServer;
import top.ilovemyhome.dagtask.core.server.DagSchedulerBuilder;
import top.ilovemyhome.zora.muserver.security.AppSecurityContext;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;
import top.ilovemyhome.zora.muserver.security.core.CookieValueType;
import top.ilovemyhome.zora.muserver.security.core.User;
import top.ilovemyhome.zora.rdb.config.RdbConfig;
import top.ilovemyhome.zora.rdb.flyway.FlywayMigrationRunner;
import top.ilovemyhome.zora.rdb.pool.DataSourcePoolBuilder;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AppContext {

    private final String env;
    private final Config config;
    private DataSource dataSource;
    private Jdbi jdbi;

    private final Map<Class<?>, Object> BEAN_FACTORY = new HashMap<>();
    private final Map<String, Object> BEAN_NAME_FACTORY = new HashMap<>();

    public AppContext(String env, Config config) {
        this.env = env;
        this.config = config;

        //Init security
        initSecurity();
        //Init Db
        initRdb(config);
        runFlywayMigration(config);

        startDagServer();

    }

    private void initSecurity() {
        List<User> users = config.getConfigList("users")
                .stream()
                .map(item -> {
                    String id = item.getString("id");
                    String name = item.getString("name");
                    String displayName = item.getString("displayName");
                    List<String> roles = item.getStringList("roles");
                    String passwordHashVal = item.getString("passwordHashVal");
                    Map<String, Object> unwrapped = (Map<String, Object>) item.getConfig("attributes").root().unwrapped();
                    Map<String, Object> attributes = unwrapped.entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> (Object) String.valueOf(entry.getValue())
                            ));
                    return new User(id, name, displayName, roles, passwordHashVal, attributes);
                })
                .toList();

        String applicationName = config.getString("name");
        var appSecurityContext = AppSecurityContext.builder()
                .inMemoryUser(users)
                .jwtIssuer(applicationName)
                .jwtSubject("access")
                .jwtTtl(config.getDuration("jwt.ttl", java.util.concurrent.TimeUnit.MILLISECONDS))
                .jwtPublicKeyPath(config.getString("jwt.publicKeyLocation"))
                .jwtPrivateKeyPath(config.getString("jwt.privateKeyLocation"))
                .cookieName(config.getString("cookie.name"))
                .cookieValueType(config.getEnum(CookieValueType.class, "cookie.valueType"))
                .build();

        BEAN_NAME_FACTORY.put("appSecurityContext", appSecurityContext);
        BEAN_FACTORY.put(AppSecurityContext.class, appSecurityContext);
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
        this.jdbi = Jdbi.create(this.dataSource);
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

    private void startDagServer(){
        DagSchedulerServer dagServer = DagSchedulerBuilder.builder()
            .dataSource(this.dataSource)
            .jdbi(this.jdbi)
            .objectMapper(JacksonUtil.MAPPER)
            .scanIntervalSeconds(30)
            .maxSystemConcurrentTasks(100)
            .databaseType("postgresql")
            .heartbeatTimeoutSeconds(5)
            .heartbeatIntervalSeconds(30)
            .maxHeartbeatFailedTimes(3)
            .build();
        registerBean(DagSchedulerServer.class, "dagSchedulerServer", dagServer);
        dagServer.start();
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName, Class<T> beanClass) {
        return (T) BEAN_FACTORY.getOrDefault(beanClass, (T) BEAN_NAME_FACTORY.get(beanName));
    }

    private <T> void registerBean(Class<T> beanClass, String beanName, T bean) {
        BEAN_FACTORY.put(beanClass, bean);
        BEAN_NAME_FACTORY.put(beanName, bean);
    }


    // Getters
    public String getEnv() {
        return env;
    }

    public Config getConfig() {
        return config;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Jdbi getJdbi() {
        return jdbi;
    }

    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);
}
