package top.ilovemyhome.zorasso.muserver;

import com.typesafe.config.Config;
import top.ilovemyhome.zorasso.core.LocalUser;
import top.ilovemyhome.zorasso.core.RegisteredClient;

import java.net.URI;
import java.util.List;
import java.util.Set;

public record SsoConfiguration(
    String environment,
    String host,
    int port,
    URI publicBaseUrl,
    String cookieName,
    boolean secureCookie,
    boolean localAuthEnabled,
    List<LocalUser> users,
    List<RegisteredClient> clients
) {
    public static SsoConfiguration from(Config root) {
        Config config = root.getConfig("zora-sso");
        String environment = config.getString("environment");
        boolean enabled = config.getBoolean("local-auth.enabled");
        boolean productionAllowed = config.getBoolean("local-auth.production-allowed");
        URI publicBaseUrl = URI.create(config.getString("server.public-base-url"));
        boolean secureCookie = config.getBoolean("cookie.secure");

        if (!enabled) {
            throw new IllegalStateException("Local authentication must be explicitly enabled");
        }
        if ("production".equalsIgnoreCase(environment) && !productionAllowed) {
            throw new IllegalStateException("Local authentication is disabled in production");
        }
        boolean localhost = "localhost".equalsIgnoreCase(publicBaseUrl.getHost())
            || "127.0.0.1".equals(publicBaseUrl.getHost())
            || "::1".equals(publicBaseUrl.getHost());
        if (!"https".equalsIgnoreCase(publicBaseUrl.getScheme()) && !localhost) {
            throw new IllegalStateException("Non-local public base URL must use HTTPS");
        }
        if ("production".equalsIgnoreCase(environment) && !secureCookie) {
            throw new IllegalStateException("Production mode requires secure cookies");
        }

        List<LocalUser> users = config.getConfigList("users").stream()
            .map(item -> new LocalUser(
                item.getString("subject"),
                item.getString("username"),
                item.getString("display-name"),
                item.getString("password-hash"),
                Set.copyOf(item.getStringList("roles")),
                item.getBoolean("enabled")
            ))
            .toList();
        List<RegisteredClient> clients = config.getConfigList("clients").stream()
            .map(item -> new RegisteredClient(
                item.getString("client-id"),
                item.getString("client-secret-hash"),
                item.getStringList("redirect-uris").stream().map(URI::create).collect(java.util.stream.Collectors.toSet()),
                item.getBoolean("enabled")
            ))
            .toList();

        return new SsoConfiguration(
            environment,
            config.getString("server.host"),
            config.getInt("server.port"),
            publicBaseUrl,
            config.getString("cookie.name"),
            secureCookie,
            enabled,
            List.copyOf(users),
            List.copyOf(clients)
        );
    }
}
