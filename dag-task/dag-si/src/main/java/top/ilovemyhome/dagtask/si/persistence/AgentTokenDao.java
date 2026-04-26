package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.auth.AgentToken;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.util.List;
import java.util.Optional;

public interface AgentTokenDao extends BaseDao<AgentToken> {

    Optional<AgentToken> findByTokenId(String tokenId);

    List<AgentToken> findByAgentId(String agentId);

    List<AgentToken> findActiveByAgentId(String agentId);

    void revokeToken(String tokenId, String revokedBy);
}
