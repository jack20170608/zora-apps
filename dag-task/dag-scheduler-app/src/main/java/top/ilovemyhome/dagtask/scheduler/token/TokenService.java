package top.ilovemyhome.dagtask.scheduler.token;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.scheduler.config.JwtConfig;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentTokenRepository;
import top.ilovemyhome.dagtask.si.auth.AgentToken;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;

public class TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenService.class);

    private final AgentTokenRepository agentTokenRepository;
    private final JwtConfig jwtConfig;

    public TokenService(AgentTokenRepository agentTokenRepository, JwtConfig jwtConfig) {
        this.agentTokenRepository = Objects.requireNonNull(agentTokenRepository, "agentTokenRepository must not be null");
        this.jwtConfig = Objects.requireNonNull(jwtConfig, "jwtConfig must not be null");
    }

    public TokenInfo generateToken(String name, String description, int expiresInDays, String createdBy) {
        return generateToken(null, name, description, expiresInDays, createdBy);
    }

    public TokenInfo generateToken(String agentId, String name, String description, int expiresInDays, String createdBy) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        if (expiresInDays <= 0) {
            throw new IllegalArgumentException("expiresInDays must be greater than 0");
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw new IllegalArgumentException("createdBy must not be null or blank");
        }

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

        agentTokenRepository.create(tokenRecord);

        TokenInfo tokenInfo = new TokenInfo(
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
        String jwt = generateJwt(tokenInfo);
        return tokenInfo.withToken(jwt);
    }

    private String generateJwt(TokenInfo tokenInfo) {
        return Jwts.builder()
            .issuer(jwtConfig.issuer())
            .subject(tokenInfo.agentId())
            .id(tokenInfo.tokenId())
            .issuedAt(Date.from(tokenInfo.createdAt()))
            .expiration(Date.from(tokenInfo.expiresAt()))
            .claim("name", tokenInfo.name())
            .claim("id", tokenInfo.agentId())
            .claim("roles", "agent")
            .signWith(jwtConfig.privateKey(), SignatureAlgorithm.RS256)
            .compact();
    }

    public boolean validateToken(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return false;
        }

        try {
            // Parse and verify signature
            var claims = Jwts.parser()
                .setSigningKey(jwtConfig.publicKey())
                .build()
                .parseClaimsJws(jwt);

            String tokenId = claims.getBody().getId();
            Optional<AgentToken> tokenOpt = agentTokenRepository.findByTokenId(tokenId);

            // Token must exist and not be revoked
            return tokenOpt.isPresent() && !tokenOpt.get().isRevoked();
        } catch (Exception e) {
            LOGGER.debug("Token validation failed", e);
            return false;
        }
    }

    public void revokeToken(String tokenId, String revokedBy) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("tokenId must not be null or blank");
        }
        if (revokedBy == null || revokedBy.isBlank()) {
            throw new IllegalArgumentException("revokedBy must not be null or blank");
        }
        agentTokenRepository.revokeToken(tokenId, revokedBy);
    }

    public List<TokenInfo> listTokens() {
        List<AgentToken> tokens = agentTokenRepository.findAll();
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
