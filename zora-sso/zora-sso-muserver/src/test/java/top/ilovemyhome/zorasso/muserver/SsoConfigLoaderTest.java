package top.ilovemyhome.zorasso.muserver;

import com.typesafe.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsoConfigLoaderTest {

    @Test
    void loadsClasspathEnvironmentConfiguration() {
        Config config = SsoConfigLoader.loadConfig("application.conf", "application-local.conf");

        assertTrue(config.getBoolean("zora-sso.local-auth.enabled"));
        assertFalse(config.getConfigList("zora-sso.users").isEmpty());
        assertFalse(config.getConfigList("zora-sso.clients").isEmpty());
    }

    @Test
    void prefersAppEnvAndFallsBackToLegacyEnv() {
        assertEquals(
            "sit",
            SsoConfigLoader.requireEnvironment(Map.of("APP_ENV", "sit", "env", "local"))
        );
        assertEquals("local", SsoConfigLoader.requireEnvironment(Map.of("env", "local")));
    }

    @Test
    void loadsExternalEnvironmentConfiguration(@TempDir Path directory) throws IOException {
        Path environmentFile = directory.resolve("application-sit.conf");
        Files.writeString(environmentFile, """
            zora-sso {
              local-auth.enabled = true
              server.port = 9180
            }
            """);

        Config config = SsoConfigLoader.loadConfig("application.conf", environmentFile);

        assertEquals(9180, config.getInt("zora-sso.server.port"));
        assertTrue(config.getBoolean("zora-sso.local-auth.enabled"));
        assertEquals("ZORA_SSO_SESSION", config.getString("zora-sso.cookie.name"));
    }

    @Test
    void rejectsMissingEnvironmentVariables() {
        IllegalStateException exception = assertThrowsExactly(
            IllegalStateException.class,
            () -> SsoConfigLoader.requireEnvironment(Map.of())
        );

        assertEquals("Cannot find APP_ENV or env environment variable", exception.getMessage());
    }

    @Test
    void rejectsInvalidEnvironmentVariable() {
        IllegalStateException exception = assertThrowsExactly(
            IllegalStateException.class,
            () -> SsoConfigLoader.requireEnvironment(Map.of("APP_ENV", "../production"))
        );

        assertEquals("Invalid environment variable", exception.getMessage());
    }

    @Test
    void rejectsMissingClasspathEnvironmentConfiguration() {
        IllegalStateException exception = assertThrowsExactly(
            IllegalStateException.class,
            () -> SsoConfigLoader.loadConfig("application.conf", "application-missing.conf")
        );

        assertEquals("Environment config is missing: application-missing.conf", exception.getMessage());
    }

    @Test
    void rejectsMissingExternalEnvironmentConfiguration(@TempDir Path directory) {
        Path environmentFile = directory.resolve("application-missing.conf");

        IllegalStateException exception = assertThrowsExactly(
            IllegalStateException.class,
            () -> SsoConfigLoader.loadConfig("application.conf", environmentFile)
        );

        assertEquals(
            "Environment config is missing or unreadable: " + environmentFile,
            exception.getMessage()
        );
    }
}
