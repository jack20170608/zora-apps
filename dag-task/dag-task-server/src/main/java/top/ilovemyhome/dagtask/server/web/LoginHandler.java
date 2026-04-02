package top.ilovemyhome.dagtask.server.web;

import io.muserver.*;
import io.muserver.rest.UserPassAuthenticator;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.server.application.AppContext;
import top.ilovemyhome.zora.common.lang.CollectionUtil;
import top.ilovemyhome.zora.muserver.security.AppSecurityContext;
import top.ilovemyhome.zora.muserver.security.authenticator.JwtAuthenticator;
import top.ilovemyhome.zora.muserver.security.core.User;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LoginHandler implements RouteHandler {

    public LoginHandler(AppContext appContext) {
        this.env = appContext.getEnv();
        Objects.requireNonNull(env);
        AppSecurityContext appSecurityContext = appContext.getAppSecurityContext();
        this.userPassAuthenticators = appSecurityContext.getUserPassAuthenticators();
        this.jwtAuthenticator = appSecurityContext.getJwtAuthenticator();
        this.domains = appContext.getConfig().getStringList("cookie.domain");
        this.cookieName = appContext.getConfig().getString("cookie.name");
        Objects.requireNonNull(cookieName);
    }


    @Override
    public void handle(MuRequest request, MuResponse response, Map<String, String> pathParams) throws Exception {
        try {
            String body = request.readBodyAsString();
            JSONObject bodyJson = new JSONObject(body);
            String username = bodyJson.getString("username");
            String password = bodyJson.getString("password");
            String jwtResponseType = bodyJson.optString("jwtResponseType", "cookie");
            User user = null;
            //Anyone of the authenticators can authenticate the user
            for(UserPassAuthenticator authenticator : userPassAuthenticators){
                if(Objects.nonNull(authenticator)){
                    user = (User) authenticator.authenticate(username, password);
                    if(Objects.nonNull(user)){
                        break;
                    }
                }
            }
            if (Objects.isNull(user)) {
                response.status(Response.Status.UNAUTHORIZED.getStatusCode());
                response.contentType(MediaType.APPLICATION_JSON);
                response.write(failure("Username or Password Error!").toString(1));
            } else if (CollectionUtil.isEmpty(user.roles())) {
                response.status(Response.Status.FORBIDDEN.getStatusCode());
                response.contentType(MediaType.APPLICATION_JSON);
                response.write(failure("Not allowed to access!").toString(1));
            } else {
                if (jwtResponseType.equalsIgnoreCase("cookie")) {
                    createCookies(user).forEach(response::addCookie);
                    response.contentType(MediaType.APPLICATION_JSON);
                    response.write(success(user.displayName(), "Welcome!!!").toString(1));
                } else {
                    if (Objects.isNull(this.jwtAuthenticator)) {
                        response.write(failure("Not allowed to access!").toString(1));
                    } else {
                        var jwtToken = jwtAuthenticator.generateJwt(user);
                        response.contentType(MediaType.APPLICATION_JSON);
                        response.write(successWithToken(user.displayName(), jwtToken).toString(1));
                    }
                }
            }
        }catch (Throwable t){
            logger.error("Login failed!", t);
            response.status(Response.Status.UNAUTHORIZED.getStatusCode());
            response.contentType(MediaType.APPLICATION_JSON);
            response.write(failure("Username or Password Error!" + t.getMessage()).toString(1));
        }
    }

    private JSONObject success(String userName, String msg){
        JSONObject res = new JSONObject();
        res.put("success", true);
        res.put("message", msg);
        res.put("userName", userName);
        return res;
    }

    private JSONObject successWithToken(String userName, String jwt){
        JSONObject res = new JSONObject();
        res.put("token", jwt);
        res.put("userName", userName);
        return res;
    }

    public JSONObject failure(String msg){
        JSONObject res = new JSONObject();
        res.put("success", false);
        res.put("message", msg);
        return res;
    }

    private List<Cookie> createCookies(User user) {
        String jwtToken = jwtAuthenticator.generateJwt(user);
        List<Cookie> cookies = new ArrayList<>();
        //If not specify the domain, add the localhost
        Set<String> nonEmptyDomains = domains.stream()
            .filter(Objects::nonNull)
            .filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        nonEmptyDomains.add("localhost");
        AtomicInteger index = new AtomicInteger(1);
        nonEmptyDomains.forEach(domain -> {
            cookies.add(CookieBuilder.newCookie()
                .withName(String.format("%s%d", cookieName , index.getAndIncrement()))
                .withValue(jwtToken)
                .withMaxAgeInSeconds(2 * 3600)
                .withSameSite("Lax")
                .withPath("/")
                .withDomain(domain)
                .secure(false)
                .httpOnly(true)
                .build()
            );
        });
        return cookies;
    }

    private final List<UserPassAuthenticator> userPassAuthenticators;
    private final JwtAuthenticator jwtAuthenticator;
    private final String env;
    private final List<String> domains;
    private final String cookieName;

    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);
}
