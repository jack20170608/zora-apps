package top.ilovemyhome.dagtask.allinone.muserver.security;

import io.muserver.MuHandler;
import io.muserver.MuRequest;
import io.muserver.MuResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Unified Cookie JWT security handler for all-in-one mode.
 * Whitelist paths are exempt from authentication.
 */
public class AllInOneSecurityHandler implements MuHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllInOneSecurityHandler.class);

    private final String cookieName;
    private final Function<String, String> authenticator;
    private final Set<String> whitelistPaths;

    public AllInOneSecurityHandler(String cookieName, Function<String, String> authenticator, List<String> whitelistPaths) {
        this.cookieName = cookieName;
        this.authenticator = authenticator;
        this.whitelistPaths = Set.copyOf(whitelistPaths);
    }

    @Override
    public boolean handle(MuRequest request, MuResponse response) {
        String path = request.uri().getPath();

        // 1. Check whitelist — exact match or prefix match
        if (isWhitelisted(path)) {
            return false; // Continue to next handler
        }

        // 2. Validate Cookie JWT
        var cookieValue = request.cookie(cookieName);
        if (cookieValue.isEmpty() || authenticator.apply(cookieValue.get()) == null) {
            LOGGER.warn("Unauthorized request to {}", path);
            response.status(401);
            response.contentType("application/json");
            response.write("{\"error\":\"Unauthorized\"}");
            return true;
        }

        // 3. Set user context in request attribute for downstream handlers
        String user = authenticator.apply(cookieValue.get());
        request.attribute("user", user);
        LOGGER.debug("Authenticated request to {} by user {}", path, user);

        return false; // Continue to next handler
    }

    private boolean isWhitelisted(String path) {
        for (String whitelisted : whitelistPaths) {
            if (path.equals(whitelisted) || path.startsWith(whitelisted + "/")) {
                return true;
            }
        }
        return false;
    }
}
