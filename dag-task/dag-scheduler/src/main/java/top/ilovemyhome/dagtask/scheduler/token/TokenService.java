package top.ilovemyhome.dagtask.scheduler.token;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import top.ilovemyhome.dagtask.scheduler.config.JwtConfig;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;
import top.ilovemyhome.dagtask.si.persistence.AgentRegistryDao;
import top.ilovemyhome.dagtask.si.agent.AgentRegistryItem;

import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class TokenService {

    private final AgentRegistryDao agentRegistryDao;
    private final JwtConfig jwtConfig;

    public TokenService(AgentRegistryDao agentRegistryDao, JwtConfig jwtConfig) {
        this.agentRegistryDao = agentRegistryDao;
        this.jwtConfig = jwtConfig;
    }

    public record GenerateTokenResult(
        String tokenId,
        String name,
        String description,
        Instant issuedAt,
        Instant expiresAt,
        String createdBy
    ) {}

    public GenerateTokenResult generateToken(String name, String description, int expiresInDays, String createdBy) {
        String tokenId = generateId();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiresInDays, ChronoUnit.DAYS);

        GenerateTokenResult result = new GenerateTokenResult(tokenId, name, description, issuedAt, expiresAt, createdBy);

        // Persist token metadata to the database
        AgentRegistryItem tokenRecord = AgentRegistryItem.builder()
            .withAgentId("__token__" + tokenId)
            .withAgentUrl("")
            .withMaxConcurrentTasks(0)
            .withMaxPendingTasks(0)
            .withSupportedExecutionKeys(List.of())
            .withRegisteredAt(issuedAt)
            .withLastHeartbeatAt(issuedAt)
            .withRunning(false)
            .withPendingTasks(0)
            .withRunningTasks(0)
            .withFinishedTasks(0)
            .withTokenId(tokenId)
            .withTokenName(name)
            .withDescription(description)
            .withCreatedBy(createdBy)
            .withCreatedAt(issuedAt)
            .withExpiresAt(expiresAt)
            .withRevoked(false)
            .withRevokedAt(null)
            .withRevokedBy(null)
            .build();

        agentRegistryDao.create(tokenRecord);
        return result;
    }

    public String generateJwt(GenerateTokenResult result) {
        return Jwts.builder()
            .setIssuer(jwtConfig.getIssuer())
            .setSubject("agent")
            .setId(result.tokenId())
            .setIssuedAt(Date.from(result.issuedAt()))
            .setExpiration(Date.from(result.expiresAt()))
            .claim("name", result.name())
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
            Optional<AgentRegistryItem> agentOpt = agentRegistryDao.findByTokenId(tokenId);

            // Token must exist and not be revoked
            return agentOpt.isPresent() && !agentOpt.get().isRevoked();
        } catch (Exception e) {
            return false;
        }
    }

    public void revokeToken(String tokenId, String revokedBy) {
        agentRegistryDao.revokeToken(tokenId, revokedBy);
    }

    public List<TokenInfo> listTokens() {
        List<AgentRegistryItem> agents = agentRegistryDao.findAll();
        List<TokenInfo> result = new ArrayList<>();
        for (AgentRegistryItem agent : agents) {
            result.add(new TokenInfo(
                agent.getTokenId(),
                agent.getTokenName(),
                agent.getDescription(),
                agent.getCreatedBy(),
                agent.getCreatedAt(),
                agent.getExpiresAt(),
                agent.isRevoked()
            ));
        }
        return result;
    }

    private String generateId() {
        byte[] bytes = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
