package top.ilovemyhome.springsecsso.client;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.ExtendedModelMap;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientControllerTest {

    @Test
    void exposesIdentityWithoutTokens() throws Exception {
        var user = mock(OidcUser.class);
        when(user.getSubject()).thenReturn("researcher");
        when(user.getIssuer()).thenReturn(URI.create("http://localhost:9100").toURL());

        var identity = new ClientController("sso-app-a").identity(user);

        assertAll("minimal identity",
            () -> assertEquals(Set.of("application", "subject", "issuer"), identity.keySet()),
            () -> assertEquals("researcher", identity.get("subject")),
            () -> assertEquals("sso-app-a", identity.get("application"))
        );
    }

    @Test
    void showsPublicHomeBeforeAuthentication() {
        var model = new ExtendedModelMap();
        var view = new ClientController("sso-app-b").home(null, model);
        assertAll("public login entry",
            () -> assertEquals("index", view),
            () -> assertEquals("sso-app-b", model.get("applicationName")),
            () -> assertNull(model.get("user"))
        );
    }
}
