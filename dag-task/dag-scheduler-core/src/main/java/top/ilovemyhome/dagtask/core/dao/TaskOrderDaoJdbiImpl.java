package top.ilovemyhome.dagtask.core.dao;


import com.fasterxml.jackson.core.type.TypeReference;
import org.jdbi.v3.core.Jdbi;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.OrderType;
import top.ilovemyhome.zora.common.date.LocalDateUtils;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;
import top.ilovemyhome.zora.jdbi.SqlGenerator;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;

import static top.ilovemyhome.zora.common.lang.StringConvertUtils.toEnum;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;


public class TaskOrderDaoJdbiImpl extends BaseDaoJdbiImpl<TaskOrder> implements TaskOrderDao {

    public TaskOrderDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName("t_task_order")
            .withIdAutoGenerate(true)
            .withFieldColumnMap(TaskOrder.FIELD_COLUMN_MAP)
            .withIdField(TaskOrder.ID_FIELD)
            .build(), jdbi);
    }

    @Override
    public Optional<TaskOrder> findByKey(String key) {
        Objects.requireNonNull(key);
        String sql = getCachedSql(SqlGenerator.SQL_STATEMENT.selectAll)
            + " where key = :key ";
        return find(sql, Map.of("key", key), null).stream().findAny();
    }

    @Override
    public int updateByKey(String taskKey, TaskOrder task) {
        Objects.requireNonNull(task);
        Objects.requireNonNull(taskKey);
        String sql = """
            update t_task_order set
            name = :t.name
            , order_type = :t.order_type
            , attributes = :t.attributes
            , last_update_dt = :t.lastUpdateDt
            where key = :key
            """;
        return update(sql, Map.of("key", taskKey), null, Map.of("t", task));
    }

    @Override
    public int deleteByKey(String key) {
        Objects.requireNonNull(key);
        String sql = """
            delete from t_task_order where key = :key
            """;
        return delete(sql, Map.of("key", key) , null);
    }


    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(TaskOrder.class, (rs, ctx) -> TaskOrder.builder()
            .withId(rs.getLong(TaskOrder.Field.id.getDbColumn()))
            .withName(rs.getString(TaskRecord.Field.name.getDbColumn()))
            .withKey(rs.getString(TaskOrder.Field.key.getDbColumn()))
            .withOrderType(toEnum(OrderType.class, rs.getString(TaskOrder.Field.orderType.getDbColumn())))
            .withAttributes(JacksonUtil.fromJson(rs.getString(TaskOrder.Field.attributes.getDbColumn()), new TypeReference<>() {
            }))
            .withCreateDt(LocalDateUtils.toLocalDateTime(rs.getTimestamp(TaskRecord.Field.createDt.getDbColumn())))
            .withLastUpdateDt(LocalDateUtils.toLocalDateTime(rs.getTimestamp(TaskRecord.Field.lastUpdateDt.getDbColumn())))
            .build());
    }
}
