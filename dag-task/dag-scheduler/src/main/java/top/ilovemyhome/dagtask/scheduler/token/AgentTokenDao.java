package top.ilovemyhome.dagtask.scheduler.token;

import java.util.List;
import java.util.Optional;

public interface AgentTokenDao {

    AgentToken insert(AgentToken token);

    Optional<AgentToken> findByTokenId(String tokenId);

    List<AgentToken> findAll();

    void revoke(String tokenId, String revokedBy);
}
