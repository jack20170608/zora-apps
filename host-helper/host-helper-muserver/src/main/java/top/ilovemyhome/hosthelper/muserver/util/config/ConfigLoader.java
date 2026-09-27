package top.ilovemyhome.hosthelper.muserver.util.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {

    public static Config loadConfig(String conf){
        return ConfigFactory.parseResources(conf).resolve();
    }

    public static Config loadConfig(String rootConf, String fallbackConf){
        if (ConfigLoader.class.getClassLoader().getResource(fallbackConf) == null) {
            throw new IllegalStateException("Environment config is missing: " + fallbackConf);
        }
        Config defaultConfig = ConfigFactory.parseResources(rootConf);
        Config specConfig = ConfigFactory.parseResources(fallbackConf);
        return specConfig.withFallback(defaultConfig).resolve();
    }

    public static Config loadConfig(String rootConf, Path environmentFile) {
        if (!Files.isRegularFile(environmentFile) || !Files.isReadable(environmentFile)) {
            throw new IllegalStateException("Environment config is missing or unreadable: " + environmentFile);
        }
        return ConfigFactory.parseFile(environmentFile.toFile())
            .withFallback(ConfigFactory.parseResources(rootConf))
            .resolve();
    }
}
