package top.ilovemyhome.dagtask.scheduler.token;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AgentTokenDaoJdbiImpl extends BaseDaoJdbiImpl<AgentToken> implements AgentTokenDao {

    public static final String TABLE_NAME = "agent_tokens";

    public AgentTokenDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName(TABLE_NAME)
            .withIdField("id")
            .withIdAutoGenerate(true)
            .build(), jdbi);
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(AgentToken.class, new AgentTokenRowMapper());
    }

    @Override
    public AgentToken insert(AgentToken token) {
        String sql = """
            INSERT INTO agent_tokens (
                token_id, name, description, created_by, created_at, expires_at, revoked
            ) VALUES (:tokenId, :name, :description, :createdBy, :createdAt, :expiresAt, :revoked)
            """;
        Map<String, Object> params = Map.of(
            "tokenId", token.tokenId(),
            "name", token.name(),
            "description", token.description(),
            "createdBy", token.createdBy(),
            "createdAt", Timestamp.from(token.createdAt()),
            "expiresAt", Timestamp.from(token.expiresAt()),
            "revoked", token.revoked()
        );
        update(sql, params, null);
        return token;
    }

    @Override
    public Optional<AgentToken> findByTokenId(String tokenId) {
        String sql = "SELECT * FROM " + table.getName() + " WHERE token_id = :tokenId";
        List<AgentToken> results = find(sql, Map.of("tokenId", tokenId), null);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<AgentToken> findAll() {
        String sql = "SELECT * FROM " + table.getName() + " ORDER BY created_at DESC";
        return find(sql, Map.of(), null);
    }

    @Override
    public void revoke(String tokenId, String revokedBy) {
        String sql = String.format(
            "UPDATE %s SET revoked = true, revoked_at = NOW(), revoked_by = :revokedBy WHERE token_id = :tokenId",
            table.getName()
        );
        update(sql, Map.of(
            "revokedBy", revokedBy,
            "tokenId", tokenId
        ), null);
    }

    private static class AgentTokenRowMapper implements RowMapper<AgentToken> {
        @Override
        public AgentToken map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return new AgentToken(
                rs.getLong("id"),
                rs.getString("token_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getBoolean("revoked"),
                rs.getTimestamp("revoked_at") != null
                    ? rs.getTimestamp("revoked_at").toInstant()
                    : null,
                rs.getString("revoked_by")
            );
        }
    }
}
