package top.ilovemyhome.hosthelper.muserver.util.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public final class ConfigLoader {

    public static Config loadConfig(String conf){
        return ConfigFactory.parseResources(conf);
    }

    public static Config loadConfig(String rootConf, String fallbackConf){
        Config defaultConfig = ConfigFactory.parseResources(rootConf);
        Config specConfig = ConfigFactory.parseResources(fallbackConf);
        return specConfig.withFallback(defaultConfig);
    }
}
