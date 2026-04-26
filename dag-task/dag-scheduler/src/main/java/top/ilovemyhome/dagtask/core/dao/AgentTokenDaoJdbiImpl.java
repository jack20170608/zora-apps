package top.ilovemyhome.dagtask.core.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.dagtask.si.auth.AgentToken;
import top.ilovemyhome.dagtask.si.persistence.AgentTokenDao;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AgentTokenDaoJdbiImpl extends BaseDaoJdbiImpl<AgentToken> implements AgentTokenDao {

    public AgentTokenDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName("t_agent_tokens")
            .withIdField("id")
            .withIdAutoGenerate(true)
            .withFieldColumnMap(AgentToken.FIELD_COLUMN_MAP)
            .build(), jdbi);
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(AgentToken.class, new AgentTokenRowMapper());
    }

    @Override
    public Optional<AgentToken> findByTokenId(String tokenId) {
        Objects.requireNonNull(tokenId, "tokenId must not be null");
        String sql = String.format("select * from %s where token_id = :tokenId", table.getName());
        return find(sql, Map.of("tokenId", tokenId), null).stream().findAny();
    }

    @Override
    public List<AgentToken> findByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format("select * from %s where agent_id = :agentId", table.getName());
        return find(sql, Map.of("agentId", agentId), null);
    }

    @Override
    public List<AgentToken> findActiveByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format(
            "select * from %s where agent_id = :agentId and revoked = false and expires_at > NOW()",
            table.getName()
        );
        return find(sql, Map.of("agentId", agentId), null);
    }

    @Override
    public void revokeToken(String tokenId, String revokedBy) {
        Objects.requireNonNull(tokenId, "tokenId must not be null");
        String sql = String.format(
            "update %s set revoked = true, revoked_at = NOW(), revoked_by = :revokedBy where token_id = :tokenId",
            table.getName()
        );
        update(sql, Map.of("tokenId", tokenId, "revokedBy", revokedBy), null);
    }

    private static class AgentTokenRowMapper implements RowMapper<AgentToken> {
        @Override
        public AgentToken map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return AgentToken.builder()
                .withId(rs.getLong(AgentToken.Field.id.getDbColumn()))
                .withTokenId(rs.getString(AgentToken.Field.tokenId.getDbColumn()))
                .withAgentId(rs.getString(AgentToken.Field.agentId.getDbColumn()))
                .withName(rs.getString(AgentToken.Field.name.getDbColumn()))
                .withDescription(rs.getString(AgentToken.Field.description.getDbColumn()))
                .withCreatedBy(rs.getString(AgentToken.Field.createdBy.getDbColumn()))
                .withCreatedAt(rs.getTimestamp(AgentToken.Field.createdAt.getDbColumn()) != null
                    ? rs.getTimestamp(AgentToken.Field.createdAt.getDbColumn()).toInstant() : null)
                .withExpiresAt(rs.getTimestamp(AgentToken.Field.expiresAt.getDbColumn()) != null
                    ? rs.getTimestamp(AgentToken.Field.expiresAt.getDbColumn()).toInstant() : null)
                .withRevoked(rs.getBoolean(AgentToken.Field.revoked.getDbColumn()))
                .withRevokedAt(rs.getTimestamp(AgentToken.Field.revokedAt.getDbColumn()) != null
                    ? rs.getTimestamp(AgentToken.Field.revokedAt.getDbColumn()).toInstant() : null)
                .withRevokedBy(rs.getString(AgentToken.Field.revokedBy.getDbColumn()))
                .build();
        }
    }
}
