package top.ilovemyhome.dagtask.core.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jdbi.v3.core.Jdbi;
import top.ilovemyhome.dagtask.si.enums.DispatchStatus;
import top.ilovemyhome.zora.jdbi.SqlGenerator;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;
import top.ilovemyhome.dagtask.si.TaskDispatchRecord;
import top.ilovemyhome.dagtask.si.persistence.TaskDispatchDao;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static top.ilovemyhome.zora.common.date.LocalDateUtils.toLocalDateTime;
import static top.ilovemyhome.zora.common.lang.StringConvertUtils.toEnum;

/**
 * JDBI-based implementation of {@link TaskDispatchDao}.
 * Stores task dispatch tracking records in the {@code t_task_dispatch} table.
 *
 * @see TaskDispatchDao
 * @see top.ilovemyhome.dagtask.si.TaskDispatchRecord
 */
public class TaskDispatchDaoJdbiImpl extends BaseDaoJdbiImpl<TaskDispatchRecord> implements TaskDispatchDao {

    /**
     * Database table name for task dispatch tracking.
     */
    public static final String TABLE_NAME = "t_task_dispatch";

    /**
     * Creates a new TaskDispatchDaoJdbiImpl with the given Jdbi instance.
     *
     * @param jdbi the Jdbi instance connected to the database
     */
    public TaskDispatchDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName(TABLE_NAME)
            .withFieldColumnMap(TaskDispatchRecord.FIELD_COLUMN_MAP)
            .withIdField(TaskDispatchRecord.ID_FIELD)
            .withIdAutoGenerate(true)
            .build(), jdbi);
    }

    @Override
    public void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(TaskDispatchRecord.class, (rs, ctx) -> buildFromResultSet(rs));
    }

    /**
     * Builds a TaskDispatchRecord from the current result set row.
     *
     * @param rs the result set
     * @return the constructed TaskDispatchRecord
     * @throws SQLException if a database access error occurs
     */
    private TaskDispatchRecord buildFromResultSet(ResultSet rs) throws SQLException {
        String paramsJson = rs.getString(TaskDispatchRecord.Field.parameters.getDbColumn());
        Map<String, String> parameters = null;
        if (paramsJson != null && !paramsJson.isBlank()) {
            parameters = JacksonUtil.fromJson(paramsJson, new TypeReference<Map<String, String>>() {});
        }

        return TaskDispatchRecord.builder()
            .withId(rs.getLong(TaskDispatchRecord.Field.id.getDbColumn()))
            .withTaskId(rs.getLong(TaskDispatchRecord.Field.taskId.getDbColumn()))
            .withAgentId(rs.getString(TaskDispatchRecord.Field.agentId.getDbColumn()))
            .withAgentUrl(rs.getString(TaskDispatchRecord.Field.agentUrl.getDbColumn()))
            .withDispatchTime(toLocalDateTime(rs.getTimestamp(TaskDispatchRecord.Field.dispatchTime.getDbColumn())))
            .withLastUpdateTime(toLocalDateTime(rs.getTimestamp(TaskDispatchRecord.Field.lastUpdateTime.getDbColumn())))
            .withStatus(toEnum(DispatchStatus.class, rs.getString(TaskDispatchRecord.Field.status.getDbColumn())))
            .withParameters(parameters)
            .build();
    }

    @Override
    public List<TaskDispatchRecord> findByTaskId(Long taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll)
            + " where TASK_ID = :taskId ";
        return find(sql, Map.of("taskId", taskId), null);
    }

    @Override
    public List<TaskDispatchRecord> findByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll)
            + " where AGENT_ID = :agentId order by DISPATCH_TIME desc ";
        return find(sql, Map.of("agentId", agentId), null);
    }

    @Override
    public List<TaskDispatchRecord> findByStatus(DispatchStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll)
            + " where STATUS = :status ";
        return find(sql, Map.of("status", status), null);
    }

    @Override
    public int updateStatus(Long taskId, DispatchStatus newStatus) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        LocalDateTime now = LocalDateTime.now();
        String sql = String.format(
            "update %s set STATUS = :status, LAST_UPDATE_TIME = :lastUpdateTime where TASK_ID = :taskId",
            table.getName()
        );
        return update(sql, Map.of(
            "taskId", taskId,
            "status", newStatus,
            "lastUpdateTime", now
        ), null);
    }

    @Override
    public int countByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.countAll)
            + " where AGENT_ID = :agentId ";
        return count(sql, Map.of("agentId", agentId), null);
    }

    @Override
    public int countByStatus(DispatchStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.countAll)
            + " where STATUS = :status ";
        return count(sql, Map.of("status", status), null);
    }

    @Override
    public int deleteByAgentId(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        String sql = String.format(
            "delete from %s where AGENT_ID = :agentId ",
            table.getName()
        );
        return update(sql, Map.of("agentId", agentId), null);
    }
}
