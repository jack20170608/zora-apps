package top.ilovemyhome.zorasso.muserver;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.util.Map;

final class SsoConfigLoader {

    static Config load(Map<String, String> environmentVariables) {
        String environment = requireEnvironment(environmentVariables);
        String environmentResource = "application-" + environment + ".conf";
        if (SsoConfigLoader.class.getClassLoader().getResource(environmentResource) == null) {
            throw new IllegalStateException("Environment config is missing: " + environmentResource);
        }
        return ConfigFactory.parseResources(environmentResource)
            .withFallback(ConfigFactory.parseResources("application.conf"))
            .withValue("zora-sso.environment", ConfigValueFactory.fromAnyRef(environment))
            .resolve();
    }

    static String requireEnvironment(Map<String, String> environmentVariables) {
        String environment = environmentVariables.get("app_env");
        if (environment == null || environment.isBlank()) {
            throw new IllegalStateException("Cannot find app_env environment variable");
        }
        if (!environment.matches("[a-zA-Z0-9][a-zA-Z0-9_-]*")) {
            throw new IllegalStateException("Invalid app_env environment variable");
        }
        return environment;
    }

    private SsoConfigLoader() {
    }
}
