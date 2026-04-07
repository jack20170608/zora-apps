package top.ilovemyhome.hosthelper.muserver.api;

import com.google.common.collect.Maps;
import io.muserver.Cookie;
import io.muserver.MuHandler;
import io.muserver.MuRequest;
import io.muserver.MuResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zora.commons.util.CollectionUtil;
import top.ilovemyhome.zora.muserver.security.AppSecurityContext;
import top.ilovemyhome.zora.muserver.security.core.CookieValueType;
import top.ilovemyhome.zora.muserver.security.core.User;
import top.ilovemyhome.zora.commons.text.AntPathMatcher;
import top.ilovemyhome.hosthelper.muserver.application.AppContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class SecurityHandler implements MuHandler {

    public SecurityHandler(AppContext appContext) {
        this.contextPath = appContext.getConfig().getString("server.contextPath");
        this.cookieName = appContext.getConfig().getString("cookie.name");
        this.cookieValueType = appContext.getConfig().getEnum(CookieValueType.class, "cookie.valueType");
        this.appSecurityContext = appContext.getBean("appSecurityContext", AppSecurityContext.class);
        Function<String, List<String>> FUN_RESTRICTED_STATIC_URIS = (contextPath) -> List.of(
            "/" + contextPath + "/index.html"
        );
        this.restrictedStaticUris = FUN_RESTRICTED_STATIC_URIS.apply(this.contextPath);
    }

    @Override
    public boolean handle(MuRequest request, MuResponse response) {
        Optional<String> cookieName = request.cookies().stream().map(Cookie::name)
            .filter(name -> StringUtils.startsWith(name, this.cookieName))
            .findAny();
        boolean authenticated = false;
        String path = request.uri().getPath();
        boolean isRestricted = restrictCheck(path);
        if (!isRestricted) {
            return false;
        } else {
            if (cookieName.isPresent()) {
                String cookie = request.cookie(cookieName.get()).orElse(null);
                User user = (User) this.appSecurityContext.getCookieAuthSecurityFilter().getAuthenticator().authenticate(cookie);
                authenticated = Objects.nonNull(user);
            }
            logger.info("Authenticated: [{}].", authenticated);
            if (authenticated) {
                return false;
            } else {
                response.redirect(String.format("/%s/login.html", contextPath));
                return true;
            }
        }
    }

    private boolean restrictCheck(String pathUri) {
        restrictedCache.computeIfAbsent(pathUri, (path) -> {
            boolean result = false;
            if (CollectionUtil.isEmpty(restrictedStaticUris)) {
                return true;
            } else {
                for (String whitePath : restrictedStaticUris) {
                    if (antPathMatcher.match(whitePath, pathUri)) {
                        result = true;
                        break;
                    }
                }
            }
            return result;
        });
        return restrictedCache.get(pathUri);
    }

    private final Map<String, Boolean> restrictedCache = Maps.newConcurrentMap();

    private final String cookieName;
    private final CookieValueType cookieValueType;
    private final AppSecurityContext appSecurityContext;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final String contextPath;
    private final List<String> restrictedStaticUris;

    private static final Logger logger = LoggerFactory.getLogger(SecurityHandler.class);
}
