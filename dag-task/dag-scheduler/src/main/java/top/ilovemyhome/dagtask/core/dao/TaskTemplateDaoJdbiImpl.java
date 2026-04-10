package top.ilovemyhome.dagtask.core.dao;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.persistence.TaskTemplateDao;
import top.ilovemyhome.zora.jdbi.SqlGenerator;
import top.ilovemyhome.zora.jdbi.TableDescription;
import top.ilovemyhome.zora.jdbi.dao.BaseDaoJdbiImpl;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBI-based implementation of {@link TaskTemplateDao}.
 * Persists task template information to a PostgreSQL database table.
 */
public class TaskTemplateDaoJdbiImpl extends BaseDaoJdbiImpl<TaskTemplate> implements TaskTemplateDao {

    /**
     * Creates a new TaskTemplateDaoJdbiImpl with the given Jdbi instance.
     *
     * @param jdbi the Jdbi instance connected to the database
     */
    public TaskTemplateDaoJdbiImpl(Jdbi jdbi) {
        super(TableDescription.builder()
            .withName("t_task_template")
            .withIdField("id")
            .withIdAutoGenerate(true)
            .withFieldColumnMap(TaskTemplate.FIELD_COLUMN_MAP)
            .build(), jdbi);
    }

    @Override
    protected void registerRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(TaskTemplate.class, new TaskTemplateRowMapper());
    }


//    @Override
//    public List<TaskTemplate> findByTemplateKey(String templateKey) {
//        Objects.requireNonNull(templateKey, "templateKey must not be null");
//        TaskTemplateSearchCriteria criteria = TaskTemplateSearchCriteria.builder()
//                .withTemplateKey(templateKey)
//                .build();
//        return find(criteria);
//    }
//
//    @Override
//    public Optional<TaskTemplate> findByKeyAndVersion(String templateKey, String version) {
//        Objects.requireNonNull(templateKey, "templateKey must not be null");
//        Objects.requireNonNull(version, "version must not be null");
//        TaskTemplateSearchCriteria criteria = TaskTemplateSearchCriteria.builder()
//                .withTemplateKey(templateKey)
//                .withVersion(version)
//                .build();
//        return find(criteria).stream().findFirst();
//    }
//
//    @Override
//    public Optional<TaskTemplate> findActiveByTemplateKey(String templateKey) {
//        Objects.requireNonNull(templateKey, "templateKey must not be null");
//        TaskTemplateSearchCriteria criteria = TaskTemplateSearchCriteria.builder()
//                .withTemplateKey(templateKey)
//                .withActive(true)
//                .build();
//        return find(criteria).stream().findFirst();
//    }
//
//    @Override
//    public List<TaskTemplate> findAllActive() {
//        TaskTemplateSearchCriteria criteria = TaskTemplateSearchCriteria.builder()
//                .withActive(true)
//                .build();
//        return find(criteria);
//    }

//    @Override
//    public List<TaskTemplate> findAll() {
//        TaskTemplateSearchCriteria criteria = TaskTemplateSearchCriteria.builder().build();
//        return search(criteria);
//    }

    @Override
    public int deactivateVersion(String templateKey, String version) {
        Objects.requireNonNull(templateKey, "templateKey must not be null");
        Objects.requireNonNull(version, "version must not be null");
        String sql = String.format(
            "update %s set active = false, last_update_dt = now() " +
                "where template_key = :templateKey and version = :version",
            table.getName()
        );
        return update(sql, Map.of("templateKey", templateKey, "version", version), null);
    }

    @Override
    public int deactivateOtherVersions(String templateKey, String activeVersion) {
        Objects.requireNonNull(templateKey, "templateKey must not be null");
        Objects.requireNonNull(activeVersion, "activeVersion must not be null");
        String sql = String.format(
            "update %s set active = false, last_update_dt = now() " +
                "where template_key = :templateKey and version != :activeVersion",
            table.getName()
        );
        return update(sql, Map.of("templateKey", templateKey, "activeVersion", activeVersion), null);
    }

    @Override
    public int deleteByKeyAndVersion(String templateKey, String version) {
        Objects.requireNonNull(templateKey, "templateKey must not be null");
        Objects.requireNonNull(version, "version must not be null");
        String sql = String.format(
            "delete from %s where template_key = :templateKey and version = :version",
            table.getName()
        );
        return update(sql, Map.of("templateKey", templateKey, "version", version), null);
    }

    @Override
    public boolean exists(String templateKey, String version) {
        Objects.requireNonNull(templateKey, "templateKey must not be null");
        Objects.requireNonNull(version, "version must not be null");
        String sql = String.format(
            "select count(*) from %s where template_key = :templateKey and version = :version",
            table.getName()
        );
        return count(sql, Map.of("templateKey", templateKey, "version", version), null) > 0;
    }

    /**
     * RowMapper for mapping database rows to {@link TaskTemplate} objects.
     */
    private static class TaskTemplateRowMapper implements RowMapper<TaskTemplate> {
        @Override
        public TaskTemplate map(ResultSet rs, org.jdbi.v3.core.statement.StatementContext ctx) throws SQLException {
            return TaskTemplate.builder()
                .withId(rs.getLong(TaskTemplate.Field.id.getDbColumn()))
                .withTemplateKey(rs.getString(TaskTemplate.Field.templateKey.getDbColumn()))
                .withTemplateName(rs.getString(TaskTemplate.Field.templateName.getDbColumn()))
                .withDescription(rs.getString(TaskTemplate.Field.description.getDbColumn()))
                .withVersion(rs.getString(TaskTemplate.Field.version.getDbColumn()))
                .withActive(rs.getBoolean(TaskTemplate.Field.active.getDbColumn()))
                .withDagDefinition(rs.getString(TaskTemplate.Field.dagDefinition.getDbColumn()))
                .withParameterSchema(rs.getString(TaskTemplate.Field.parameterSchema.getDbColumn()))
                .withCreateDt(rs.getTimestamp(TaskTemplate.Field.createDt.getDbColumn()) != null
                    ? rs.getTimestamp(TaskTemplate.Field.createDt.getDbColumn()).toLocalDateTime()
                    : null)
                .withLastUpdateDt(rs.getTimestamp(TaskTemplate.Field.lastUpdateDt.getDbColumn()) != null
                    ? rs.getTimestamp(TaskTemplate.Field.lastUpdateDt.getDbColumn()).toLocalDateTime()
                    : null)
                .withVersionSeq(rs.getInt(TaskTemplate.Field.versionSeq.getDbColumn()))
                .build();
        }
    }
}
