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
 * The table schema expected is:
 * <pre>
 * CREATE TABLE t_agent_registry (
 *     agent_id VARCHAR(255) PRIMARY KEY,
 *     agent_url VARCHAR(1024) NOT NULL,
 *     max_concurrent_tasks INTEGER NOT NULL,
 *     max_pending_tasks INTEGER NOT NULL,
 *     supported_execution_keys TEXT NOT NULL,  -- JSON array
 *     registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
 *     last_heartbeat_at TIMESTAMP WITH TIME ZONE NOT NULL,
 *     running BOOLEAN NOT NULL DEFAULT TRUE,
 *     pending_tasks INTEGER NOT NULL DEFAULT 0,
 *     running_tasks INTEGER NOT NULL DEFAULT 0,
 *     finished_tasks INTEGER NOT NULL DEFAULT 0
 * );
 * </pre>
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
            .withIdField("agent_id")
            .withIdAutoGenerate(false)
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

    /**
     * RowMapper for mapping database rows to {@link AgentInfo} objects.
     */
    private static class AgentInfoRowMapper implements RowMapper<AgentInfo> {
        @Override
        public AgentInfo map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return new AgentInfo(
                rs.getString("agent_id"),
                rs.getString("agent_url"),
                rs.getInt("max_concurrent_tasks"),
                rs.getInt("max_pending_tasks"),
                parseSupportedExecutionKeys(rs.getString("supported_execution_keys")),
                rs.getTimestamp("registered_at").toInstant(),
                rs.getTimestamp("last_heartbeat_at").toInstant(),
                rs.getBoolean("running"),
                rs.getInt("pending_tasks"),
                rs.getInt("running_tasks"),
                rs.getInt("finished_tasks")
            );
        }

        /**
         * Parses the JSON array string from database into a List<String>.
         * Simple comma-split for now since it's just keys.
         * Could be replaced with JSON parsing if needed.
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
