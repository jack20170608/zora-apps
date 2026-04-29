package top.ilovemyhome.dagtask.scheduler.token;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import top.ilovemyhome.dagtask.scheduler.config.JwtConfig;
import top.ilovemyhome.dagtask.si.auth.AgentToken;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;
import top.ilovemyhome.dagtask.si.persistence.AgentTokenDao;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class TokenService {

    private final AgentTokenDao agentTokenDao;
    private final JwtConfig jwtConfig;

    public TokenService(AgentTokenDao agentTokenDao, JwtConfig jwtConfig) {
        this.agentTokenDao = agentTokenDao;
        this.jwtConfig = jwtConfig;
    }

    public TokenInfo generateToken(String name, String description, int expiresInDays, String createdBy) {
        return generateToken(null, name, description, expiresInDays, createdBy);
    }

    public TokenInfo generateToken(String agentId, String name, String description, int expiresInDays, String createdBy) {
        String tokenId = generateId();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiresInDays, ChronoUnit.DAYS);

        // Persist token metadata to the database
        AgentToken tokenRecord = AgentToken.builder()
            .withTokenId(tokenId)
            .withAgentId(agentId)
            .withName(name)
            .withDescription(description)
            .withCreatedBy(createdBy)
            .withCreatedAt(issuedAt)
            .withExpiresAt(expiresAt)
            .withRevoked(false)
            .withRevokedAt(null)
            .withRevokedBy(null)
            .build();

        agentTokenDao.create(tokenRecord);

        return new TokenInfo(
            null,
            tokenId,
            agentId,
            name,
            description,
            createdBy,
            issuedAt,
            expiresAt,
            null
        );
    }

    public String generateJwt(TokenInfo tokenInfo) {
        return Jwts.builder()
            .issuer(jwtConfig.getIssuer())
            .subject("agent")
            .id(tokenInfo.tokenId())
            .issuedAt(Date.from(tokenInfo.createdAt()))
            .expiration(Date.from(tokenInfo.expiresAt()))
            .claim("name", tokenInfo.name())
            .signWith(jwtConfig.getPrivateKey(), SignatureAlgorithm.RS256)
            .compact();
    }

    public boolean validateToken(String jwt) {
        try {
            // Parse and verify signature
            var claims = Jwts.parser()
                .setSigningKey(jwtConfig.getPublicKey())
                .build()
                .parseClaimsJws(jwt);

            String tokenId = claims.getBody().getId();
            Optional<AgentToken> tokenOpt = agentTokenDao.findByTokenId(tokenId);

            // Token must exist and not be revoked
            return tokenOpt.isPresent() && !tokenOpt.get().isRevoked();
        } catch (Exception e) {
            return false;
        }
    }

    public void revokeToken(String tokenId, String revokedBy) {
        agentTokenDao.revokeToken(tokenId, revokedBy);
    }

    public List<TokenInfo> listTokens() {
        List<AgentToken> tokens = agentTokenDao.findAll();
        return tokens.stream()
            .map(token -> new TokenInfo(
                token.getId(),
                token.getTokenId(),
                token.getAgentId(),
                token.getName(),
                token.getDescription(),
                token.getCreatedBy(),
                token.getCreatedAt(),
                token.getExpiresAt(),
                null
            ))
            .toList();
    }

    private String generateId() {
        byte[] bytes = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
