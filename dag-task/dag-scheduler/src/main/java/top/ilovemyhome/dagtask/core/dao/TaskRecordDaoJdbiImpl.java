package top.ilovemyhome.dagtask.core.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jdbi.v3.core.Jdbi;
import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;
import top.ilovemyhome.zora.jdbi.SqlGenerator;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static top.ilovemyhome.zora.common.date.LocalDateUtils.toLocalDateTime;
import static top.ilovemyhome.zora.common.lang.StringConvertUtils.toEnum;


public class TaskRecordDaoJdbiImpl extends BaseDaoJdbiImpl<TaskRecord> implements TaskRecordDao {

    public static final String TABLE_NAME = "t_task";

    public TaskRecordDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName(TABLE_NAME)
            .withFieldColumnMap(TaskRecord.FIELD_COLUMN_MAP)
            .withIdField(TaskRecord.ID_FIELD)
            .withIdAutoGenerate(false)
            .build(), jdbi);
    }

    @Override
    public void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(TaskRecord.class, (rs, ctx) -> {
            String successorIdStr = rs.getString(TaskRecord.Field.successorIds.getDbColumn());
            Set<Long> successorIds = JacksonUtil.fromJson(successorIdStr, new TypeReference<>() {
            });
            return TaskRecord.builder()
                .withId(rs.getLong(TaskRecord.Field.id.getDbColumn()))
                .withOrderKey(rs.getString(TaskRecord.Field.orderKey.getDbColumn()))
                .withName(rs.getString(TaskRecord.Field.name.getDbColumn()))
                .withDescription(rs.getString(TaskRecord.Field.description.getDbColumn()))
                .withExecutionKey(rs.getString(TaskRecord.Field.executionKey.getDbColumn()))
                .withSuccessorIds(successorIds)
                .withInput(rs.getString(TaskRecord.Field.input.getDbColumn()))
                .withOutput(rs.getString(TaskRecord.Field.output.getDbColumn()))
                .withAsync(rs.getBoolean(TaskRecord.Field.async.getDbColumn()))
                .withDummy(rs.getBoolean(TaskRecord.Field.dummy.getDbColumn()))
                .withCreateDt(toLocalDateTime(rs.getTimestamp(TaskRecord.Field.createDt.getDbColumn())))
                .withLastUpdateDt(toLocalDateTime(rs.getTimestamp(TaskRecord.Field.lastUpdateDt.getDbColumn())))
                .withStatus(toEnum(TaskStatus.class, rs.getString(TaskRecord.Field.status.getDbColumn())))
                .withStartDt(toLocalDateTime(rs.getTimestamp(TaskRecord.Field.startDt.getDbColumn())))
                .withEndDt(toLocalDateTime(rs.getTimestamp(TaskRecord.Field.endDt.getDbColumn())))
                .withSuccess(rs.getBoolean(TaskRecord.Field.success.getDbColumn()))
                .withFailReason(rs.getString(TaskRecord.Field.failReason.getDbColumn()))
                .withTimeout(rs.getLong(TaskRecord.Field.timeout.getDbColumn()))
                .withTimeoutUnit(toEnum(TimeUnit.class, rs.getString(TaskRecord.Field.timeoutUnit.getDbColumn())))
                .build();
        });
    }

    @Override
    public Long getNextId() {
        return jdbi.withHandle(h -> h.createQuery("select nextval('seq_t_task_id') ")
            .mapTo(Long.class)
            .one());
    }

    @Override
    public List<TaskRecord> findByOrderKey(String orderKey) {
        Objects.requireNonNull(orderKey);
        TaskRecordSearchCriteria criteria = TaskRecordSearchCriteria.builder()
                .withOrderKey(orderKey)
                .build();
        return search(criteria);
    }

    @Override
    public List<TaskRecord> findByStatus(TaskStatus status) {
        Objects.requireNonNull(status);
        TaskRecordSearchCriteria criteria = TaskRecordSearchCriteria.builder()
                .withStatus(status)
                .build();
        return search(criteria);
    }

    @Override
    public int deleteByOrderKey(String orderKey) {
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.deleteAll)
            + " where ORDER_KEY = :orderKey ";
        return update(sql, Map.of("orderKey", orderKey), null);
    }

    @Override
    public List<TaskRecord> loadTaskForOrder(String orderKey) {
        TaskRecordSearchCriteria criteria = TaskRecordSearchCriteria.builder()
                .withOrderKey(orderKey)
                .build();
        return search(criteria);
    }

    @Override
    public int createTasksForOrder(String orderKey, List<TaskRecord> listOfTask) {
        AtomicInteger result = new AtomicInteger();
        jdbi.useTransaction(h -> {
            listOfTask.forEach(t -> {
                create(t);
                result.incrementAndGet();
            });
        });
        return result.get();
    }

    @Override
    public boolean isOrdered(String orderKey) {
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.countAll)
            + " where ORDER_KEY = :orderKey";
        return count(sql, Map.of("orderKey", orderKey), null) > 0;
    }

    @Override
    public boolean isSuccess(String orderKey) {
        boolean ordered = isOrdered(orderKey);
        if (!ordered) {
            return false;
        }
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.countAll)
            + " where ORDER_KEY = :orderKey  and STATUS != :status ";
        int nonSuccessCount = count(sql, Map.of("orderKey", orderKey, "status", TaskStatus.SUCCESS), null);
        return nonSuccessCount == 0;
    }

    @Override
    public int start(Long id, TaskInput input, LocalDateTime startDt) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(startDt);
        String sql = String.format("update %s set STATUS = :status , START_DT = :startDt , INPUT = :input where ID = :id ", table.getName());
        return update(sql
            , Map.of("status", TaskStatus.RUNNING, "startDt", startDt, "id", id, "input", input)
            , null);
    }

    @Override
    public int stop(Long id, TaskStatus newStatus, TaskOutput output, LocalDateTime endDt) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(endDt);
        boolean success = newStatus == TaskStatus.SUCCESS;
        LocalDateTime now = LocalDateTime.now();
        String sql = String.format(
            "update %s set STATUS = :status, LAST_UPDATE_DT = :lastUpdateDt, SUCCESS = :success, END_DT = :endDt "
            , table.getName());
        Map<String, Object> normalParams = new HashMap<>();
        normalParams.put("id", id);
        normalParams.put("status", newStatus);
        normalParams.put("lastUpdateDt", now);
        normalParams.put("endDt", endDt);
        normalParams.put("success", success);

        if (Objects.nonNull(output.output())) {
            sql = sql + " ,OUTPUT = :output ";
            normalParams.put("output", output.output());
        }
        if (!output.isSuccess() && Objects.nonNull(output.message())) {
            sql = sql + " ,FAIL_REASON = :failReason";
            normalParams.put("failReason", output.message());
        }
        sql = sql + " where ID = :id ";
        return update(sql, normalParams, null);
    }

    @Override
    public String getTaskOrderByTaskId(Long taskId) {
        Objects.requireNonNull(taskId);
        return findOne(taskId).map(TaskRecord::getOrderKey).orElse(null);
    }

    @Override
    public boolean isReady(Long taskId) {
        Objects.requireNonNull(taskId);
        // For PostgreSQL: find all predecessor tasks (tasks that have this task in their successor_ids)
        // and count how many are NOT in SUCCESS status
        // successor_ids is stored as JSON array
        // successor_ids is stored as varchar (JSON string), need to cast to jsonb first
        String sql = String.format(
            "select count(*) from %s " +
                "where (successor_ids::jsonb) @> jsonb_build_array(:taskId) " +
                "and status != 'SUCCESS'",
            table.getName()
        );
        int nonSuccessCount = jdbi.withHandle(h ->
            h.createQuery(sql)
                .bind("taskId", taskId)
                .mapTo(Integer.class)
                .one()
        );
        return nonSuccessCount == 0;
    }

    @Override
    public Optional<TaskRecord> loadTaskById(Long taskId) {
        return findOne(taskId);
    }

    @Override
    public List<TaskRecord> findReadyTasksForOrder(String orderKey) {
        Objects.requireNonNull(orderKey);
        // Find all tasks for this order that are in INIT status and ready to execute
        // A task is ready when there are NO predecessors (tasks that contain it in their successor_ids) with status != SUCCESS
        String sql = String.format(
            "select * from %s " +
                "where order_key = :orderKey " +
                "and status = 'INIT' " +
                "and not exists (" +
                "    select 1 from %s t2 " +
                "    where (t2.successor_ids::jsonb) @> jsonb_build_array(%s.id) " +
                "    and t2.status != 'SUCCESS'" +
                ")",
            table.getName(), table.getName(), table.getName()
        );
        return jdbi.withHandle(h ->
            h.createQuery(sql)
                .bind("orderKey", orderKey)
                .mapTo(TaskRecord.class)
                .list()
        );
    }

    @Override
    public List<TaskRecord> findReadySuccessors(Long taskId) {
        Objects.requireNonNull(taskId);
        // Find all direct successors of the given task that are now ready to execute
        // A successor is ready when all its predecessors (including this one) are SUCCESS
        // We get all successors from the current task's successor_ids, then filter them with the ready check
        String sql = String.format(
            "select * from %s " +
                "where id in (" +
                "    select (jsonb_array_elements(successor_ids::jsonb))::bigint from %s where id = :taskId" +
                ") " +
                "and status = 'INIT' " +
                "and not exists (" +
                "    select 1 from %s t2 " +
                "    where (t2.successor_ids::jsonb) @> jsonb_build_array(%s.id) " +
                "    and t2.status != 'SUCCESS'" +
                ")",
            table.getName(), table.getName(), table.getName(), table.getName()
        );
        return jdbi.withHandle(h ->
            h.createQuery(sql)
                .bind("taskId", taskId)
                .mapTo(TaskRecord.class)
                .list()
        );
    }

    @Override
    public int updateStatus(Long taskId, TaskStatus newStatus) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        LocalDateTime now = LocalDateTime.now();
        String sql = String.format("update %s set STATUS = :status, LAST_UPDATE_DT = :lastUpdateDt where ID = :id",
            table.getName());
        return update(sql, Map.of("id", taskId, "status", newStatus, "lastUpdateDt", now), null);
    }

    /**
     * Search tasks using dynamic search criteria.
     *
     * @param criteria the search criteria to use
     * @return list of tasks matching the search criteria
     */
    @Override
    public List<TaskRecord> search(TaskRecordSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll) + criteria.whereClause();
        return find(sql, criteria.normalParams(), criteria.listParam());
    }

    /**
     * Search tasks with pagination using dynamic search criteria.
     *
     * @param criteria the search criteria to use
     * @param pageable pagination information
     * @return page of tasks matching the search criteria
     */
    @Override
    public Page<TaskRecord> search(TaskRecordSearchCriteria criteria, Pageable pageable) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll) + criteria.pageableWhereClause(pageable);
        return findAll(sql, criteria.normalParams(), criteria.listParam(), pageable);
    }
}
