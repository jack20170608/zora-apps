package top.ilovemyhome.micronautsecsso;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.csrf.CsrfConfiguration;
import io.micronaut.security.csrf.generator.CsrfTokenGenerator;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.session.Session;
import io.micronaut.views.View;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class HomeController {

    public HomeController(@Value("${micronaut.application.name}") String name,
                          CsrfTokenGenerator<HttpRequest<?>> csrfGenerator,
                          CsrfConfiguration csrfConfiguration) {
        this.name = name;
        this.csrfGenerator = csrfGenerator;
        this.csrfConfiguration = csrfConfiguration;
    }

    @Get("/")
    @View("index")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public Map<String, Object> home(HttpRequest<?> request, Session session, @Nullable Authentication authentication) {
        String csrfToken = csrfGenerator.generateCsrfToken(request);
        session.put(csrfConfiguration.getHttpSessionName(), csrfToken);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("applicationName", name);
        model.put("loggedIn", authentication != null);
        model.put("username", authentication == null ? "" : authentication.getName());
        model.put("csrfToken", csrfToken);
        return model;
    }

    @Get("/api/me")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public Map<String, String> identity(Authentication authentication) {
        return Map.of("application", name, "username", authentication.getName());
    }

    @Get("/health")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    private final String name;
    private final CsrfTokenGenerator<HttpRequest<?>> csrfGenerator;
    private final CsrfConfiguration csrfConfiguration;
}
