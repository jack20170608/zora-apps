package top.ilovemyhome.dagtask.core.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.dagtask.si.agent.Agent;
import top.ilovemyhome.dagtask.si.persistence.AgentDao;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AgentDaoJdbiImpl extends BaseDaoJdbiImpl<Agent> implements AgentDao {

    public AgentDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName("t_agents")
            .withIdField("id")
            .withIdAutoGenerate(true)
            .withFieldColumnMap(Agent.FIELD_COLUMN_MAP)
            .build(), jdbi);
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(Agent.class, new AgentRowMapper());
    }

    @Override
    public Optional<Agent> findByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format("select * from %s where agent_id = :agentId", table.getName());
        return find(sql, Map.of("agentId", agentId), null).stream().findAny();
    }

    @Override
    public List<Agent> findByStatus(Agent.Status status) {
        Objects.requireNonNull(status, "status must not be null");
        String sql = String.format("select * from %s where status = :status", table.getName());
        return find(sql, Map.of("status", status.name()), null);
    }

    @Override
    public void updateStatus(String agentId, Agent.Status status) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        String sql = String.format(
            "update %s set status = :status, updated_at = NOW() where agent_id = :agentId",
            table.getName()
        );
        update(sql, Map.of("agentId", agentId, "status", status.name()), null);
    }

    @Override
    public void updateHeartbeat(String agentId, Instant heartbeatAt) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format(
            "update %s set last_heartbeat_at = :heartbeatAt, updated_at = NOW() where agent_id = :agentId",
            table.getName()
        );
        update(sql, Map.of("agentId", agentId, "heartbeatAt", java.sql.Timestamp.from(heartbeatAt)), null);
    }

    @Override
    public boolean exists(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format("select count(*) from %s where agent_id = :agentId", table.getName());
        return count(sql, Map.of("agentId", agentId), null) > 0;
    }

    private static class AgentRowMapper implements RowMapper<Agent> {
        @Override
        public Agent map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return Agent.builder()
                .withId(rs.getLong(Agent.Field.id.getDbColumn()))
                .withAgentId(rs.getString(Agent.Field.agentId.getDbColumn()))
                .withName(rs.getString(Agent.Field.name.getDbColumn()))
                .withDescription(rs.getString(Agent.Field.description.getDbColumn()))
                .withLabelsJson(rs.getString(Agent.Field.labelsJson.getDbColumn()))
                .withStatus(Agent.Status.valueOf(rs.getString(Agent.Field.status.getDbColumn())))
                .withRegisteredAt(rs.getTimestamp(Agent.Field.registeredAt.getDbColumn()) != null
                    ? rs.getTimestamp(Agent.Field.registeredAt.getDbColumn()).toInstant() : null)
                .withLastHeartbeatAt(rs.getTimestamp(Agent.Field.lastHeartbeatAt.getDbColumn()) != null
                    ? rs.getTimestamp(Agent.Field.lastHeartbeatAt.getDbColumn()).toInstant() : null)
                .withCreatedAt(rs.getTimestamp(Agent.Field.createdAt.getDbColumn()) != null
                    ? rs.getTimestamp(Agent.Field.createdAt.getDbColumn()).toInstant() : null)
                .withUpdatedAt(rs.getTimestamp(Agent.Field.updatedAt.getDbColumn()) != null
                    ? rs.getTimestamp(Agent.Field.updatedAt.getDbColumn()).toInstant() : null)
                .build();
        }
    }
}
