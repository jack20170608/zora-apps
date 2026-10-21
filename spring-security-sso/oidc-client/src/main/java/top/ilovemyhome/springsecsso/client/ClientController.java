package top.ilovemyhome.springsecsso.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class ClientController {

    public ClientController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("user", user);
        return "index";
    }

    @GetMapping("/api/me")
    @ResponseBody
    public Map<String, String> identity(@AuthenticationPrincipal OidcUser user) {
        return Map.of("application", applicationName, "subject", user.getSubject(),
            "issuer", user.getIssuer().toString());
    }

    @GetMapping("/health")
    @ResponseBody
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    private final String applicationName;
}
