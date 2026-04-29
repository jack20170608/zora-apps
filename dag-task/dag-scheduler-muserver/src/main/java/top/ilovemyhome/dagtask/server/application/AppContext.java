package top.ilovemyhome.dagtask.server.application;

import com.typesafe.config.Config;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.DagSchedulerServer;
import top.ilovemyhome.dagtask.core.server.DagSchedulerBuilder;
import top.ilovemyhome.dagtask.scheduler.config.JwtConfig;
import top.ilovemyhome.zora.muserver.security.AppSecurityContext;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;
import top.ilovemyhome.zora.muserver.security.core.CookieValueType;
import top.ilovemyhome.zora.muserver.security.core.User;
import top.ilovemyhome.zora.rdb.config.RdbConfig;
import top.ilovemyhome.zora.rdb.flyway.FlywayMigrationRunner;
import top.ilovemyhome.zora.rdb.pool.DataSourcePoolBuilder;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
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
        //Init JWT
        JwtConfig jwtConfig = readJwtConfig(config);
        registerBean(JwtConfig.class, "jwtConfig", jwtConfig);

        startDagServer(jwtConfig);

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
        List<String> whiteList = config.hasPath("security.whiteList")
                ? config.getStringList("security.whiteList")
                : new ArrayList<>();
        var appSecurityContext = AppSecurityContext.builder()
                .inMemoryUser(users)
                .jwtIssuer(applicationName)
                .jwtSubject("access")
                .jwtTtl(config.getDuration("jwt.ttl", java.util.concurrent.TimeUnit.MILLISECONDS))
                .jwtPublicKeyPath(config.getString("jwt.publicKeyLocation"))
                .jwtPrivateKeyPath(config.getString("jwt.privateKeyLocation"))
                .cookieName(config.getString("cookie.name"))
                .cookieValueType(config.getEnum(CookieValueType.class, "cookie.valueType"))
                .whiteList(whiteList)
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

    private void startDagServer(JwtConfig jwtConfig){
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
            .jwtConfig(jwtConfig)
            .build();
        registerBean(DagSchedulerServer.class, "dagSchedulerServer", dagServer);
        dagServer.start();
    }

    private static JwtConfig readJwtConfig(Config config) {
        Config jwt = config.getConfig("jwt");
        String issuer = jwt.getString("issuer");
        String publicKeyPath = jwt.getString("publicKeyLocation");
        String privateKeyPath = jwt.getString("privateKeyLocation");
        try {
            PublicKey publicKey = readPublicKey(publicKeyPath);
            PrivateKey privateKey = readPrivateKey(privateKeyPath);
            return new JwtConfig(issuer, publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JWT keys", e);
        }
    }

    private static PublicKey readPublicKey(String path) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String content = readKeyContent(path);
        byte[] decoded = Base64.getDecoder().decode(content);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    private static PrivateKey readPrivateKey(String path) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String content = readKeyContent(path);
        byte[] decoded = Base64.getDecoder().decode(content);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private static String readKeyContent(String path) {
        try {
            if (path.startsWith("classpath:")) {
                String resourcePath = path.substring("classpath:".length());
                try (InputStream is = AppContext.class.getClassLoader().getResourceAsStream(resourcePath)) {
                    if (is == null) {
                        throw new RuntimeException("Resource not found in classpath: " + resourcePath);
                    }
                    return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
                }
            } else {
                java.io.File file = new java.io.File(path);
                return java.nio.file.Files.readString(file.toPath())
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read key: " + path, e);
        }
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
