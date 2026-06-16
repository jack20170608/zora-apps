package top.ilovemyhome.dagtask.scheduler.app;

import java.util.Objects;
import top.ilovemyhome.dagtask.scheduler.port.out.TokenIssuer;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;

/**
 * TokenIssuer adapter that delegates to the existing TokenService implementation.
 */
public class LegacyTokenIssuer implements TokenIssuer {

    private final TokenService tokenService;

    public LegacyTokenIssuer(TokenService tokenService) {
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService must not be null");
    }

    @Override
    public TokenInfo issueAgentToken(String agentId, String name, String description, int validDays, String issuer) {
        return tokenService.generateToken(agentId, name, description, validDays, issuer);
    }
}
