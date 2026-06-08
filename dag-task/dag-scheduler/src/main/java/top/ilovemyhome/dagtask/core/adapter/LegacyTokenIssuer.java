package top.ilovemyhome.dagtask.core.adapter;

import top.ilovemyhome.dagtask.scheduler.port.out.TokenIssuer;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;

import java.util.Objects;

/**
 * Temporary adapter: domain {@link TokenIssuer} that delegates to the legacy
 * {@link TokenService} until step 3 owns the full issuance flow.
 */
public class LegacyTokenIssuer implements TokenIssuer {

    private final TokenService tokenService;

    public LegacyTokenIssuer(TokenService tokenService) {
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService must not be null");
    }

    @Override
    public TokenInfo issueAgentToken(String agentId, String name, String description,
                                      int validDays, String issuer) {
        return tokenService.generateToken(agentId, name, description, validDays, issuer);
    }
}
