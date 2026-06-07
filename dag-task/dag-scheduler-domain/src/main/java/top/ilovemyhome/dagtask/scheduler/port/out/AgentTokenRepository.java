package top.ilovemyhome.dagtask.scheduler.port.out;

import top.ilovemyhome.dagtask.si.auth.AgentToken;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for {@link AgentToken} persistence. Implementations live in
 * dag-scheduler-adapter-persistence-jdbc (or any other persistence adapter).
 */
public interface AgentTokenRepository {

    Optional<AgentToken> findByTokenId(String tokenId);

    List<AgentToken> findByAgentId(String agentId);

    List<AgentToken> findActiveByAgentId(String agentId);

    void revokeToken(String tokenId, String revokedBy);
}
