package top.ilovemyhome.dagtask.core.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.dagtask.si.agent.AgentInfo;
import top.ilovemyhome.dagtask.si.persistence.AgentRegistryDao;
import top.ilovemyhome.zora.jdbi.SqlGenerator;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBI-based implementation of {@link AgentRegistryDao}.
 * Persists agent registry information to a PostgreSQL database table.
 *
 */
public class AgentRegistryDaoJdbiImpl extends BaseDaoJdbiImpl<AgentInfo> implements AgentRegistryDao {

    /**
     * Creates a new AgentRegistryDaoJdbiImpl with the given Jdbi instance.
     *
     * @param jdbi the Jdbi instance connected to the database
     */
    public AgentRegistryDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName("t_agent_registry")
            .withIdField("id")
            .withIdAutoGenerate(true)
            .withFieldColumnMap(AgentInfo.FIELD_COLUMN_MAP)
            .build(), jdbi);
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(AgentInfo.class, new AgentInfoRowMapper());
    }

    @Override
    public Optional<AgentInfo> findByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll) + " where agent_id = :agentId ";
        return find(sql, Map.of("agentId", agentId), null).stream().findFirst();
    }

    @Override
    public List<AgentInfo> findAllActive() {
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll) + " where running = true ";
        return find(sql, Map.of(), null);
    }

    @Override
    public List<AgentInfo> findAll() {
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll);
        return find(sql, Map.of(), null);
    }

    @Override
    public int deleteByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.deleteAll) + " where agent_id = :agentId ";
        return update(sql, Map.of("agentId", agentId), null);
    }

    @Override
    public int updateStatus(String agentId, boolean running, int pendingTasks, int runningTasks, int finishedTasks) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format(
            "update %s set running = :running, pending_tasks = :pendingTasks, " +
            "running_tasks = :runningTasks, finished_tasks = :finishedTasks, last_heartbeat_at = now() " +
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
            "update %s set running = false, last_heartbeat_at = now() where agent_id = :agentId",
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

    @Override
    public Long create(AgentInfo entity) {
        // Explicitly handle insert with auto-generated id - exclude id from column list
        String sql = String.format(
            "insert into %s (agent_id, agent_url, max_concurrent_tasks, max_pending_tasks, " +
                "supported_execution_keys, registered_at, last_heartbeat_at, running, " +
                "pending_tasks, running_tasks, finished_tasks) " +
                "values (:agentId, :agentUrl, :maxConcurrentTasks, :maxPendingTasks, " +
                ":supportedExecutionKeys, :registeredAt, :lastHeartbeatAt, :running, " +
                ":pendingTasks, :runningTasks, :finishedTasks) " +
                "returning id",
            table.getName()
        );
        // supported_execution_keys is NOT NULL, even if empty use empty string, not null
        String supportedKeys = "";
        if (entity.getSupportedExecutionKeys() != null && !entity.getSupportedExecutionKeys().isEmpty()) {
            supportedKeys = String.join(",", entity.getSupportedExecutionKeys());
        }
        Map<String, Object> params = Map.of(
            "agentId", entity.getAgentId(),
            "agentUrl", entity.getAgentUrl(),
            "maxConcurrentTasks", entity.getMaxConcurrentTasks(),
            "maxPendingTasks", entity.getMaxPendingTasks(),
            "supportedExecutionKeys", supportedKeys,
            "registeredAt", java.sql.Timestamp.from(entity.getRegisteredAt()),
            "lastHeartbeatAt", java.sql.Timestamp.from(entity.getLastHeartbeatAt()),
            "running", entity.isRunning(),
            "pendingTasks", entity.getPendingTasks(),
            "runningTasks", entity.getRunningTasks(),
            "finishedTasks", entity.getFinishedTasks()
        );
        // Already have returning id in SQL, use createQuery to avoid adding duplicate returning clause
        return jdbi.withHandle(h -> h.createQuery(sql)
            .bindFromMap(params)
            .mapTo(Long.class)
            .one());
    }

    /**
     * RowMapper for mapping database rows to {@link AgentInfo} objects.
     */
    private static class AgentInfoRowMapper implements RowMapper<AgentInfo> {
        @Override
        public AgentInfo map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return AgentInfo.builder()
                .withId(rs.getLong(AgentInfo.Field.id.getDbColumn()))
                .withAgentId(rs.getString(AgentInfo.Field.agentId.getDbColumn()))
                .withAgentUrl(rs.getString(AgentInfo.Field.agentUrl.getDbColumn()))
                .withMaxConcurrentTasks(rs.getInt(AgentInfo.Field.maxConcurrentTasks.getDbColumn()))
                .withMaxPendingTasks(rs.getInt(AgentInfo.Field.maxPendingTasks.getDbColumn()))
                .withSupportedExecutionKeys(parseSupportedExecutionKeys(rs.getString(AgentInfo.Field.supportedExecutionKeys.getDbColumn())))
                .withRegisteredAt(rs.getTimestamp(AgentInfo.Field.registeredAt.getDbColumn()).toInstant())
                .withLastHeartbeatAt(rs.getTimestamp(AgentInfo.Field.lastHeartbeatAt.getDbColumn()).toInstant())
                .withRunning(rs.getBoolean(AgentInfo.Field.running.getDbColumn()))
                .withPendingTasks(rs.getInt(AgentInfo.Field.pendingTasks.getDbColumn()))
                .withRunningTasks(rs.getInt(AgentInfo.Field.runningTasks.getDbColumn()))
                .withFinishedTasks(rs.getInt(AgentInfo.Field.finishedTasks.getDbColumn()))
                .build();
        }

        /**
         * Parses the comma-separated string from database into a List<String>.
         * Currently stored as comma-separated for simplicity.
         * Could be replaced with JSON parsing if needed in the future.
         */
        private List<String> parseSupportedExecutionKeys(String input) {
            if (input == null || input.isBlank()) {
                return List.of();
            }
            // For simplicity, split by comma - this can be changed to JSON parsing if needed
            return List.of(input.split(","));
        }
    }
}
