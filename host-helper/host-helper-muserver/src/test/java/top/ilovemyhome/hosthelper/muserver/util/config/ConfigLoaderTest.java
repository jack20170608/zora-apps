package top.ilovemyhome.hosthelper.muserver.util.config;

import com.typesafe.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void resolvesExternalEnvironmentWithBundledDefaults(@TempDir Path directory) throws IOException {
        Path environmentFile = directory.resolve("application-prod.conf");
        Files.writeString(environmentFile, "server.contextPath = external-hosthelper\n");

        Config config = ConfigLoader.loadConfig("config/application.conf", environmentFile);

        assertEquals("external-hosthelper", config.getString("server.contextPath"));
        assertEquals("hosthelper", config.getString("name"));
        assertTrue(config.isResolved());
    }

    @Test
    void refusesMissingExternalEnvironment(@TempDir Path directory) {
        assertThrows(IllegalStateException.class,
            () -> ConfigLoader.loadConfig("config/application.conf", directory.resolve("missing.conf")));
    }

    @Test
    void refusesMissingClasspathEnvironment() {
        assertThrows(IllegalStateException.class,
            () -> ConfigLoader.loadConfig("config/application.conf", "config/application-no-such-env.conf"));
    }
}

