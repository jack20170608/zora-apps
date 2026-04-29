package top.ilovemyhome.dagtask.si.auth;

import java.time.Instant;

/**
 * Token information DTO containing all fields from {@link AgentToken} except revocation info,
 * plus the actual JWT token string for generation responses.
 */
public record TokenInfo(
    Long id,
    String tokenId,
    String agentId,
    String name,
    String description,
    String createdBy,
    Instant createdAt,
    Instant expiresAt,
    String token
) {

    /**
     * Returns a new TokenInfo with the JWT token string set.
     *
     * @param token the JWT token string
     * @return a new TokenInfo instance with the token field populated
     */
    public TokenInfo withToken(String token) {
        return new TokenInfo(this.id, this.tokenId, this.agentId, this.name,
            this.description, this.createdBy, this.createdAt, this.expiresAt, token);
    }
}
