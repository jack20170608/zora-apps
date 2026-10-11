package top.ilovemyhome.zorasso.muserver;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

final class SsoConfigLoader {

    static Config loadConfig(String rootConfig, String environmentConfig) {
        if (SsoConfigLoader.class.getClassLoader().getResource(environmentConfig) == null) {
            throw new IllegalStateException("Environment config is missing: " + environmentConfig);
        }
        return ConfigFactory.parseResources(environmentConfig)
            .withFallback(ConfigFactory.parseResources(rootConfig))
            .resolve();
    }

    static Config loadConfig(String rootConfig, Path environmentFile) {
        if (!Files.isRegularFile(environmentFile) || !Files.isReadable(environmentFile)) {
            throw new IllegalStateException("Environment config is missing or unreadable: " + environmentFile);
        }
        return ConfigFactory.parseFile(environmentFile.toFile())
            .withFallback(ConfigFactory.parseResources(rootConfig))
            .resolve();
    }

    static String requireEnvironment(Map<String, String> environmentVariables) {
        String environment = environmentVariables.get("APP_ENV");
        if (isBlank(environment)) {
            environment = environmentVariables.get("env");
        }
        if (environment == null || environment.isBlank()) {
            throw new IllegalStateException("Cannot find APP_ENV or env environment variable");
        }
        if (!environment.matches("[a-zA-Z0-9][a-zA-Z0-9_-]*")) {
            throw new IllegalStateException("Invalid environment variable");
        }
        return environment;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private SsoConfigLoader() {
    }
}
