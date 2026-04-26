package top.ilovemyhome.dagtask.core.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.dagtask.si.persistence.AgentStatusDao;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AgentStatusDaoJdbiImpl extends BaseDaoJdbiImpl<AgentStatus> implements AgentStatusDao {

    public AgentStatusDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName("t_agent_status")
            .withIdField("id")
            .withIdAutoGenerate(true)
            .withFieldColumnMap(AgentStatus.FIELD_COLUMN_MAP)
            .build(), jdbi);
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(AgentStatus.class, new AgentStatusRowMapper());
    }

    @Override
    public Optional<AgentStatus> findByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format("select * from %s where agent_id = :agentId", table.getName());
        return find(sql, Map.of("agentId", agentId), null).stream().findAny();
    }

    @Override
    public List<AgentStatus> findAllActive() {
        String sql = String.format("select * from %s where running = true", table.getName());
        return find(sql, Map.of(), null);
    }

    @Override
    public int updateStatus(String agentId, boolean running, int pendingTasks, int runningTasks, int finishedTasks) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format(
            "update %s set running = :running, pending_tasks = :pendingTasks, " +
            "running_tasks = :runningTasks, finished_tasks = :finishedTasks, last_heartbeat_at = NOW() " +
            "where agent_id = :agentId",
            table.getName()
        );
        return update(sql, Map.of(
            "agentId", agentId,
            "running", running,
            "pendingTasks", pendingTasks,
            "runningTasks", runningTasks,
            "finishedTasks", finishedTasks
        ), null);
    }

    @Override
    public int markUnregistered(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format(
            "update %s set running = false, last_heartbeat_at = NOW() where agent_id = :agentId",
            table.getName()
        );
        return update(sql, Map.of("agentId", agentId), null);
    }

    @Override
    public boolean exists(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format("select count(*) from %s where agent_id = :agentId", table.getName());
        return count(sql, Map.of("agentId", agentId), null) > 0;
    }

    private static class AgentStatusRowMapper implements RowMapper<AgentStatus> {
        @Override
        public AgentStatus map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return AgentStatus.builder()
                .withId(rs.getLong(AgentStatus.Field.id.getDbColumn()))
                .withAgentId(rs.getString(AgentStatus.Field.agentId.getDbColumn()))
                .withAgentUrl(rs.getString(AgentStatus.Field.agentUrl.getDbColumn()))
                .withMaxConcurrentTasks(rs.getInt(AgentStatus.Field.maxConcurrentTasks.getDbColumn()))
                .withMaxPendingTasks(rs.getInt(AgentStatus.Field.maxPendingTasks.getDbColumn()))
                .withSupportedExecutionKeys(rs.getString(AgentStatus.Field.supportedExecutionKeys.getDbColumn()))
                .withRunning(rs.getBoolean(AgentStatus.Field.running.getDbColumn()))
                .withPendingTasks(rs.getInt(AgentStatus.Field.pendingTasks.getDbColumn()))
                .withRunningTasks(rs.getInt(AgentStatus.Field.runningTasks.getDbColumn()))
                .withFinishedTasks(rs.getInt(AgentStatus.Field.finishedTasks.getDbColumn()))
                .withLastHeartbeatAt(rs.getTimestamp(AgentStatus.Field.lastHeartbeatAt.getDbColumn()) != null
                    ? rs.getTimestamp(AgentStatus.Field.lastHeartbeatAt.getDbColumn()).toInstant() : null)
                .build();
        }
    }
}
