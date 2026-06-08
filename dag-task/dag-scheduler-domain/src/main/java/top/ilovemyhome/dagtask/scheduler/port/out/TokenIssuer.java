package top.ilovemyhome.dagtask.scheduler.port.out;

import top.ilovemyhome.dagtask.si.auth.TokenInfo;

/**
 * Outbound port: issues agent registration tokens.
 * <p>
 * Token issuance involves JWT creation, secret key management, and persistence of
 * token metadata — all infrastructure concerns that belong in an adapter.
 * The implementation delegates to the legacy {@code TokenService}
 * ({@code LegacyTokenIssuer} in dag-allinone-muserver / dag-scheduler-app) until
 * step 3 owns the full issuance flow.
 * </p>
 */
public interface TokenIssuer {

    /**
     * Issue a new agent token.
     *
     * @param agentId     the agent identifier (may be null for admin-created tokens)
     * @param name        human-readable token name
     * @param description optional description
     * @param validDays   number of days the token is valid for
     * @param issuer      who created the token
     * @return the issued token information including the JWT string
     */
    TokenInfo issueAgentToken(String agentId, String name, String description,
                              int validDays, String issuer);
}