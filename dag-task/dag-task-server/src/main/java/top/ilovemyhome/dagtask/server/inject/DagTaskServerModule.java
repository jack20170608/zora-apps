package top.ilovemyhome.dagtask.server.inject;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.jdbi.v3.core.Jdbi;
import top.ilovemyhome.dagtask.core.AbstractTaskContext;
import top.ilovemyhome.dagtask.core.AbstractTaskDagServiceImpl;
import top.ilovemyhome.dagtask.core.TaskOrderDaoJdbiImpl;
import top.ilovemyhome.dagtask.core.TaskRecordDaoJdbiImpl;
import top.ilovemyhome.dagtask.si.TaskContext;
import top.ilovemyhome.dagtask.si.TaskDagService;
import top.ilovemyhome.dagtask.si.TaskOrderDao;
import top.ilovemyhome.dagtask.si.TaskRecordDao;
import top.ilovemyhome.zora.muserver.security.AppSecurityContext;
import top.ilovemyhome.zora.muserver.security.core.CookieValueType;
import top.ilovemyhome.zora.muserver.security.core.User;
import top.ilovemyhome.zora.rdb.config.RdbConfig;
import top.ilovemyhome.zora.rdb.config.RdbConfigLoader;
import top.ilovemyhome.zora.rdb.pool.DataSourcePoolBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DagTaskServerModule extends AbstractModule {

    private final Config config;

    public DagTaskServerModule(Config config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        // Bind interfaces to implementations
        bind(Config.class).toInstance(config);
    }

    @Provides
    @Singleton
    public Jdbi provideJdbi() {
        RdbConfig rdbConfig = RdbConfigLoader.load(config, "database");
        DataSourcePoolBuilder dbPool = DataSourcePoolBuilder.create(rdbConfig);
        Jdbi jdbi = Jdbi.create(dbPool.build());
        jdbi.installPlugin(new org.jdbi.v3.sqlobject.SqlObjectPlugin());
        return jdbi;
    }

    @Provides
    @Singleton
    public TaskContext<String, String> provideTaskContext(Jdbi jdbi) {
        int totalProcessorSize = Runtime.getRuntime().availableProcessors();
        int nThreads = Math.max(totalProcessorSize, 2);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("TaskDagService-%d").build();
        return new AbstractTaskContext<>(jdbi, new ThreadPoolExecutor(
                nThreads, nThreads, 0L, TimeUnit.MILLISECONDS
                , new LinkedBlockingQueue<>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy())
                , "t_task_order", "t_task") {
        };
    }

    @Provides
    @Singleton
    public TaskOrderDao provideTaskOrderDao(Jdbi jdbi, TaskContext<String, String> taskContext) {
        return new TaskOrderDaoJdbiImpl(jdbi, taskContext);
    }

    @Provides
    @Singleton
    public TaskRecordDao provideTaskRecordDao(Jdbi jdbi, TaskContext<String, String> taskContext) {
        return new TaskRecordDaoJdbiImpl(jdbi, taskContext);
    }

    @Provides
    @Singleton
    public TaskDagService<String, String> provideTaskDagService(Jdbi jdbi, TaskContext<String, String> taskContext) {
        return new AbstractTaskDagServiceImpl<>(jdbi, taskContext) {
        };
    }

    @Provides
    @Singleton
    public AppSecurityContext provideAppSecurityContext() {
        List<User> users = config.getConfigList("users")
                .stream()
                .map(item -> {
                    String id = item.getString("id");
                    String name = item.getString("name");
                    String displayName = item.getString("displayName");
                    List<String> roles = item.getStringList("roles");
                    String passwordHashVal = item.getString("passwordHashVal");
                    Map<String, Object> attributes = item.getConfig("attributes").root().unwrapped()
                            .entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> (Object) String.valueOf(e.getValue())
                            ));
                    return new User(id, name, displayName, roles, passwordHashVal, attributes);
                })
                .toList();

        String applicationName = config.getString("name");
        return AppSecurityContext.builder()
                .inMemoryUser(users)
                .jwtIssuer(applicationName)
                .jwtSubject("access")
                .jwtTtl(config.getDuration("jwt.ttl", java.util.concurrent.TimeUnit.MILLISECONDS))
                .jwtPublicKeyPath(config.getString("jwt.publicKeyLocation"))
                .jwtPrivateKeyPath(config.getString("jwt.privateKeyLocation"))
                .cookieName(config.getString("cookie.name"))
                .cookieValueType(config.getEnum(CookieValueType.class, "cookie.valueType"))
                .build();
    }
}
