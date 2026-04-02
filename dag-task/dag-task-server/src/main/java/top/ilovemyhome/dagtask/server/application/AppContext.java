package top.ilovemyhome.dagtask.server.application;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.AbstractTaskContext;
import top.ilovemyhome.dagtask.core.AbstractTaskDagServiceImpl;
import top.ilovemyhome.dagtask.core.TaskOrderDaoJdbiImpl;
import top.ilovemyhome.dagtask.core.TaskRecordDaoJdbiImpl;
import top.ilovemyhome.dagtask.server.interfaces.api.TaskOrderHandler;
import top.ilovemyhome.dagtask.server.interfaces.api.TaskRecordHandler;
import top.ilovemyhome.dagtask.si.TaskContext;
import top.ilovemyhome.dagtask.si.TaskDagService;
import top.ilovemyhome.dagtask.si.TaskOrderDao;
import top.ilovemyhome.dagtask.si.TaskRecordDao;
import top.ilovemyhome.zora.muserver.security.AppSecurityContext;
import top.ilovemyhome.zora.muserver.security.core.CookieValueType;
import top.ilovemyhome.zora.muserver.security.core.User;
import top.ilovemyhome.zora.rdb.config.RdbConfig;
import top.ilovemyhome.zora.rdb.config.RdbConfigLoader;
import top.ilovemyhome.zora.rdb.flyway.FlywayMigrationRunner;
import top.ilovemyhome.zora.rdb.pool.DataSourcePoolBuilder;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

        initTaskContext();

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
        this.jdbi.installPlugin(new org.jdbi.v3.sqlobject.SqlObjectPlugin());
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

    private void initTaskContext(){
        int totalProcessorSize = Runtime.getRuntime().availableProcessors();
        int nThreads = Math.max(totalProcessorSize, 2);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("TaskDagService-%d").build();
        var taskContext = new AbstractTaskContext<String, String>(jdbi, new ThreadPoolExecutor(
            nThreads, nThreads, 0L, TimeUnit.MILLISECONDS
            , new LinkedBlockingQueue<>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy())
            , "t_task_order", "t_task") {};
        registerBean(TaskContext.class, "taskContext", taskContext);

        registerBean(TaskOrderDao.class, "taskOrderDao", new TaskOrderDaoJdbiImpl(jdbi, taskContext));
        registerBean(TaskRecordDao.class, "taskRecordDao", new TaskRecordDaoJdbiImpl(jdbi, taskContext));
        registerBean(TaskDagService.class, "taskDagService", new AbstractTaskDagServiceImpl<>(jdbi, taskContext) {
        });

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
