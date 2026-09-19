package top.ilovemyhome.zorasso.si;

import java.net.URI;

public interface SsoService {

    AuthorizationStart beginAuthorization(AuthorizationRequest request, String sessionId);

    LoginPage prepareLogin(String transactionId);

    LoginResult login(
        String transactionId,
        String csrfToken,
        String username,
        char[] password,
        String source
    );

    AuthenticatedIdentity exchange(
        String code,
        String clientId,
        char[] clientSecret,
        URI redirectUri
    );

    void logout(String sessionId);

    record AuthorizationRequest(String clientId, URI redirectUri, String state) {
    }

    record AuthorizationStart(boolean loginRequired, String transactionId, URI redirectUri) {
        public static AuthorizationStart loginRequired(String transactionId) {
            return new AuthorizationStart(true, transactionId, null);
        }

        public static AuthorizationStart redirect(URI redirectUri) {
            return new AuthorizationStart(false, null, redirectUri);
        }
    }

    record LoginPage(String transactionId, String csrfToken, String clientId) {
    }

    record LoginResult(String sessionId, URI redirectUri) {
    }
}
