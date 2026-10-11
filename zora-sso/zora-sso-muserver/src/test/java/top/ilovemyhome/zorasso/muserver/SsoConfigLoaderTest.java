package top.ilovemyhome.zorasso.muserver;

import com.typesafe.config.Config;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsoConfigLoaderTest {

    @Test
    void loadsEnvironmentSpecificConfiguration() {
        Config config = SsoConfigLoader.load(Map.of("app_env", "local"));

        assertEquals("local", config.getString("zora-sso.environment"));
        assertTrue(config.getBoolean("zora-sso.local-auth.enabled"));
        assertEquals(3, config.getConfigList("zora-sso.users").size());
        assertEquals(3, config.getConfigList("zora-sso.clients").size());
    }

    @Test
    void rejectsMissingEnvironmentVariable() {
        IllegalStateException exception = assertThrowsExactly(
            IllegalStateException.class,
            () -> SsoConfigLoader.load(Map.of())
        );

        assertEquals("Cannot find app_env environment variable", exception.getMessage());
    }

    @Test
    void rejectsInvalidEnvironmentVariable() {
        IllegalStateException exception = assertThrowsExactly(
            IllegalStateException.class,
            () -> SsoConfigLoader.load(Map.of("app_env", "../production"))
        );

        assertEquals("Invalid app_env environment variable", exception.getMessage());
    }

    @Test
    void rejectsMissingEnvironmentConfiguration() {
        IllegalStateException exception = assertThrowsExactly(
            IllegalStateException.class,
            () -> SsoConfigLoader.load(Map.of("app_env", "missing"))
        );

        assertEquals("Environment config is missing: application-missing.conf", exception.getMessage());
    }
}
