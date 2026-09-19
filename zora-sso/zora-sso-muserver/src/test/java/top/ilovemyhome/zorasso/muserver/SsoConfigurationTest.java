package top.ilovemyhome.zorasso.muserver;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SsoConfigurationTest {

    @Test
    void rejectsLocalAuthenticationInProductionByDefault() {
        String config = """
            zora-sso {
              environment = production
              local-auth { enabled = true, production-allowed = false }
              server { host = "127.0.0.1", port = 9080, public-base-url = "https://sso.example" }
              cookie { name = "ZORA_SSO_SESSION", secure = true }
              users = []
              clients = []
            }
            """;

        assertThatThrownBy(() -> SsoConfiguration.from(ConfigFactory.parseString(config)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("disabled in production");
    }

    @Test
    void rejectsInsecureRemoteBaseUrl() {
        String config = """
            zora-sso {
              environment = development
              local-auth { enabled = true, production-allowed = false }
              server { host = "127.0.0.1", port = 9080, public-base-url = "http://sso.example" }
              cookie { name = "ZORA_SSO_SESSION", secure = false }
              users = []
              clients = []
            }
            """;

        assertThatThrownBy(() -> SsoConfiguration.from(ConfigFactory.parseString(config)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must use HTTPS");
    }
}
