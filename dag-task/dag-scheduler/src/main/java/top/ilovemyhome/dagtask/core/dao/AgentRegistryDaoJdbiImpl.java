package top.ilovemyhome.dagtask.core.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.dagtask.si.dto.AgentRegistrySearchCriteria;
import top.ilovemyhome.dagtask.si.agent.AgentRegistryItem;
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
public class AgentRegistryDaoJdbiImpl extends BaseDaoJdbiImpl<AgentRegistryItem> implements AgentRegistryDao {

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
            .withFieldColumnMap(AgentRegistryItem.FIELD_COLUMN_MAP)
            .build(), jdbi);
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(AgentRegistryItem.class, new AgentInfoRowMapper());
    }

    @Override
    public Optional<AgentRegistryItem> findByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll) + " where agent_id = :agentId ";
        return find(sql, Map.of("agentId", agentId), null).stream().findFirst();
    }

    @Override
    public List<AgentRegistryItem> findAllActive() {
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll) + " where running = true ";
        return find(sql, Map.of(), null);
    }

    @Override
    public List<AgentRegistryItem> findAll() {
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll);
        return find(sql, Map.of(), null);
    }

    /**
     * Search agents using dynamic search criteria.
     *
     * @param criteria the search criteria to use
     * @return list of agents matching the search criteria
     */
    @Override
    public List<AgentRegistryItem> search(AgentRegistrySearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll) + criteria.whereClause();
        return find(sql, criteria.normalParams(), criteria.listParam());
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

    /**
     * RowMapper for mapping database rows to {@link AgentRegistryItem} objects.
     */
    private static class AgentInfoRowMapper implements RowMapper<AgentRegistryItem> {
        @Override
        public AgentRegistryItem map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return AgentRegistryItem.builder()
                .withId(rs.getLong(AgentRegistryItem.Field.id.getDbColumn()))
                .withAgentId(rs.getString(AgentRegistryItem.Field.agentId.getDbColumn()))
                .withAgentUrl(rs.getString(AgentRegistryItem.Field.agentUrl.getDbColumn()))
                .withMaxConcurrentTasks(rs.getInt(AgentRegistryItem.Field.maxConcurrentTasks.getDbColumn()))
                .withMaxPendingTasks(rs.getInt(AgentRegistryItem.Field.maxPendingTasks.getDbColumn()))
                .withSupportedExecutionKeys(parseSupportedExecutionKeys(rs.getString(AgentRegistryItem.Field.supportedExecutionKeys.getDbColumn())))
                .withRegisteredAt(rs.getTimestamp(AgentRegistryItem.Field.registeredAt.getDbColumn()).toInstant())
                .withLastHeartbeatAt(rs.getTimestamp(AgentRegistryItem.Field.lastHeartbeatAt.getDbColumn()).toInstant())
                .withRunning(rs.getBoolean(AgentRegistryItem.Field.running.getDbColumn()))
                .withPendingTasks(rs.getInt(AgentRegistryItem.Field.pendingTasks.getDbColumn()))
                .withRunningTasks(rs.getInt(AgentRegistryItem.Field.runningTasks.getDbColumn()))
                .withFinishedTasks(rs.getInt(AgentRegistryItem.Field.finishedTasks.getDbColumn()))
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
