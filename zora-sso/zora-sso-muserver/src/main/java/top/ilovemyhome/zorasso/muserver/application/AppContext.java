package top.ilovemyhome.zorasso.muserver.application;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public final class AppContext {

    public AppContext(String env, Config config) {
        this.env = env;
        this.config = config;
    }



    public synchronized void init() {
        logger.info("Init the application context..........");
        //1. init the security
        initSecurity();

        //2.dao
        initDao();

        //3. service
        initService();

    }

    private void initSecurity(){
        logger.info("Init the security..........");
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
                        e -> String.valueOf(e.getValue())
                    ));
                return new User(id, name, displayName, roles, passwordHashVal, attributes);
            })
            .toList();

        AppSecurityContext appSecurityContext = AppSecurityContext.builder()
            .inMemoryUser(users)
            .jwtIssuer(getApplicationName())
            .jwtAudience("access")
            .jwtTtl(config.getDuration("jwt.ttl", TimeUnit.MILLISECONDS))
            .jwtPublicKeyPath(config.getString("jwt.publicKeyLocation"))
            .jwtPrivateKeyPath(config.getString("jwt.privateKeyLocation"))
            .cookieName(config.getString("cookie.name"))
            .cookieValueType(config.getEnum(CookieValueType.class, "cookie.valueType"))
            .build();
        BEAN_NAME_FACTORY.put("appSecurityContext", appSecurityContext);
        BEAN_FACTORY.put(AppSecurityContext.class, appSecurityContext);
    }



    private void initDao() {
    }

    private void initService() {
    }

    public String getApplicationName() {
        return config.getString("name");
    }

    public String getEnv() {
        return env;
    }


    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName, Class<T> beanClass) {
        return (T) BEAN_FACTORY.getOrDefault(beanClass, (T) BEAN_NAME_FACTORY.get(beanName));
    }

    private final String env;
    private final Config config;
    private final Map<Class<?>, Object> BEAN_FACTORY = new HashMap<>();
    private final Map<String, Object> BEAN_NAME_FACTORY = new HashMap<>();

    public Config getConfig() {
        return config;
    }

    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);
}
