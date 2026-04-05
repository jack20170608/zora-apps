package top.ilovemyhome.dagtask.si;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a reusable DAG workflow template with versioning support.
 * <p>
 * A template stores a complete DAG definition that can be instantiated multiple times
 * with different parameter values to create concrete {@link TaskOrder} instances.
 * Supports semantic versioning and activation status management.
 * </p>
 */
@JsonDeserialize(builder = TaskTemplate.Builder.class)
public class TaskTemplate {

    public String getTemplateKey() {
        return templateKey;
    }

    private Long id;
    private final String templateKey;
    private final String templateName;
    private final String description;
    private final String version;
    private final boolean active;
    private final String dagDefinition;
    private final String parameterSchema;
    private LocalDateTime createDt;
    private LocalDateTime lastUpdateDt;
    private Integer versionSeq;

    /**
     * Field enumeration for database column mapping.
     */
    public enum Field {
        id("id", true),
        templateKey("template_key"),
        templateName("template_name"),
        description("description"),
        version("version"),
        active("active"),
        dagDefinition("dag_definition"),
        parameterSchema("parameter_schema"),
        createDt("create_dt"),
        lastUpdateDt("last_update_dt"),
        versionSeq("version_seq")
        ;

        private final String dbColumn;
        private final boolean isId;

        Field(String dbColumn) {
            this.dbColumn = dbColumn;
            this.isId = false;
        }

        Field(String dbColumn, boolean isId) {
            this.dbColumn = dbColumn;
            this.isId = isId;
        }

        public String getDbColumn() {
            return dbColumn;
        }

        public boolean isId() {
            return isId;
        }
    }

    /**
     * Field to column mapping for JDBI SQL generation.
     */
    public static final java.util.Map<String, String> FIELD_COLUMN_MAP
        = Collections.unmodifiableMap(Stream.of(Field.values())
        .collect(Collectors.toMap(Field::name, Field::getDbColumn)));

    /**
     * ID field name constant.
     */
    public static final String ID_FIELD = Field.id.name();

    private TaskTemplate(Long id, String templateKey, String templateName, String description,
                        String version, boolean active, String dagDefinition, String parameterSchema,
                        LocalDateTime createDt, LocalDateTime lastUpdateDt, Integer versionSeq) {
        Objects.requireNonNull(templateKey, "templateKey must not be null");
        Objects.requireNonNull(templateName, "templateName must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(dagDefinition, "dagDefinition must not be null");

        this.id = id;
        this.templateKey = templateKey;
        this.templateName = templateName;
        this.description = description;
        this.version = version;
        this.active = active;
        this.dagDefinition = dagDefinition;
        this.parameterSchema = parameterSchema;
        LocalDateTime now = LocalDateTime.now();
        this.createDt = Objects.isNull(createDt) ? now : createDt;
        this.lastUpdateDt = Objects.isNull(lastUpdateDt) ? now : lastUpdateDt;
        this.versionSeq = Objects.isNull(versionSeq) ? 1 : versionSeq;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public boolean isActive() {
        return active;
    }

    public String getDagDefinition() {
        return dagDefinition;
    }

    public String getParameterSchema() {
        return parameterSchema;
    }

    public LocalDateTime getCreateDt() {
        return createDt;
    }

    public LocalDateTime getLastUpdateDt() {
        return lastUpdateDt;
    }

    public void setLastUpdateDt(LocalDateTime lastUpdateDt) {
        this.lastUpdateDt = lastUpdateDt;
    }

    public Integer getVersionSeq() {
        return versionSeq;
    }

    public void incrementVersionSeq() {
        this.versionSeq = this.versionSeq + 1;
        this.lastUpdateDt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskTemplate that = (TaskTemplate) o;
        return Objects.equals(templateKey, that.templateKey) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateKey, version);
    }

    @Override
    public String toString() {
        return "TaskTemplate{" +
            "id=" + id +
            ", templateKey='" + templateKey + '\'' +
            ", templateName='" + templateName + '\'' +
            ", version='" + version + '\'' +
            ", active=" + active +
            ", createDt=" + createDt +
            ", lastUpdateDt=" + lastUpdateDt +
            '}';
    }

    /**
     * Creates a new builder for TaskTemplate.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TaskTemplate.
     */
    public static final class Builder {
        private Long id;
        private String templateKey;
        private String templateName;
        private String description;
        private String version;
        private boolean active = true;
        private String dagDefinition;
        private String parameterSchema;
        private LocalDateTime createDt;
        private LocalDateTime lastUpdateDt;
        private Integer versionSeq;

        private Builder() {
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withTemplateKey(String templateKey) {
            this.templateKey = templateKey;
            return this;
        }

        public Builder withTemplateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder withActive(boolean active) {
            this.active = active;
            return this;
        }

        public Builder withDagDefinition(String dagDefinition) {
            this.dagDefinition = dagDefinition;
            return this;
        }

        public Builder withParameterSchema(String parameterSchema) {
            this.parameterSchema = parameterSchema;
            return this;
        }

        public Builder withCreateDt(LocalDateTime createDt) {
            this.createDt = createDt;
            return this;
        }

        public Builder withLastUpdateDt(LocalDateTime lastUpdateDt) {
            this.lastUpdateDt = lastUpdateDt;
            return this;
        }

        public Builder withVersionSeq(Integer versionSeq) {
            this.versionSeq = versionSeq;
            return this;
        }

        public TaskTemplate build() {
            return new TaskTemplate(id, templateKey, templateName, description,
                version, active, dagDefinition, parameterSchema,
                createDt, lastUpdateDt, versionSeq);
        }
    }
}