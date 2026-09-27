package top.ilovemyhome.hosthelper.muserver.util.config;

import com.typesafe.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

    @Test
    void resolvesSingleConfiguration() {
        Config config = ConfigLoader.loadConfig("config/application.conf");

        assertTrue(config.isResolved());
    }

    @Test
    void resolvesMergedConfiguration() {
        Config config = ConfigLoader.loadConfig(
            "config/application.conf",
            "config/application-local.conf"
        );

        assertTrue(config.isResolved());
    }
}

