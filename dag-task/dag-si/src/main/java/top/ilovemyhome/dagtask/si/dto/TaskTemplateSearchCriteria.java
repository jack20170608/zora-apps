package top.ilovemyhome.dagtask.si.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import top.ilovemyhome.zora.jdbi.SearchCriteria;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(builder = TaskTemplateSearchCriteria.Builder.class)
public class TaskTemplateSearchCriteria implements SearchCriteria {

    private final List<Long> listOfId;
    private final String templateKey;
    private final String templateKeyPrefix;
    private final String templateName;
    private final String descriptionContains;
    private final String version;
    private final Boolean active;
    private final Integer versionSeq;

    public TaskTemplateSearchCriteria(List<Long> listOfId, String templateKey, String templateKeyPrefix, String templateName,
                                      String descriptionContains, String version, Boolean active, Integer versionSeq) {
        this.listOfId = listOfId;
        this.templateKey = templateKey;
        this.templateKeyPrefix = templateKeyPrefix;
        this.templateName = templateName;
        this.descriptionContains = descriptionContains;
        this.version = version;
        this.active = active;
        this.versionSeq = versionSeq;

        prepareQuery();
    }

    public List<Long> getListOfId() {
        return listOfId;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public String getTemplateKeyPrefix() {
        return templateKeyPrefix;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getDescriptionContains() {
        return descriptionContains;
    }

    public String getVersion() {
        return version;
    }

    public Boolean isActive() {
        return active;
    }

    public Integer getVersionSeq() {
        return versionSeq;
    }

    @Override
    public String toString() {
        return "TaskTemplateSearchCriteria{" +
            "listOfId=" + listOfId +
            ", templateKey='" + templateKey + '\'' +
            ", templateKeyPrefix='" + templateKeyPrefix + '\'' +
            ", templateName='" + templateName + '\'' +
            ", descriptionContains='" + descriptionContains + '\'' +
            ", version='" + version + '\'' +
            ", active=" + active +
            ", versionSeq=" + versionSeq +
            ", whereClause='" + whereClause + '\'' +
            ", listParam=" + listParam +
            ", normalParams=" + normalParams +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    private void prepareQuery() {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append(" where 1 = 1 ");
        if (listOfId != null && !listOfId.isEmpty()) {
            ensureListParamMap();
            listParam.put("listOfId", listOfId);
            sqlBuilder.append(" AND id in (<listOfId>)");
        }
        if (StringUtils.isNotBlank(templateKey)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND template_key = :templateKey");
            normalParams.put("templateKey", templateKey);
        }
        if (StringUtils.isNotBlank(templateKeyPrefix)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND template_key like '%'||:templateKeyPrefix||'%'");
            normalParams.put("templateKeyPrefix", templateKeyPrefix);
        }
        if (StringUtils.isNotBlank(templateName)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND template_name = :templateName");
            normalParams.put("templateName", templateName);
        }
        if (StringUtils.isNotBlank(descriptionContains)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND description like '%'||:descriptionContains||'%'");
            normalParams.put("descriptionContains", descriptionContains);
        }
        if (StringUtils.isNotBlank(version)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND version = :version");
            normalParams.put("version", version);
        }
        if (Objects.nonNull(active)) {
            if (active) {
                sqlBuilder.append(" AND active is TRUE ");
            } else {
                sqlBuilder.append(" AND active is FALSE ");
            }
        }
        if (Objects.nonNull(versionSeq)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND version_seq = :versionSeq");
            normalParams.put("versionSeq", versionSeq);
        }
        this.whereClause = sqlBuilder.toString();
    }

    @Override
    public String whereClause() {
        return whereClause;
    }

    @Override
    public Map<String, Object> normalParams() {
        return Objects.nonNull(normalParams) ? ImmutableMap.copyOf(normalParams) : null;
    }

    @Override
    public Map<String, List> listParam() {
        return Objects.nonNull(listParam) ? ImmutableMap.copyOf(listParam) : null;
    }

    @Override
    public String pageableWhereClause(Pageable pageable) {
        return SearchCriteria.super.pageableWhereClause(pageable);
    }

    private void ensureListParamMap() {
        if (Objects.isNull(listParam)) {
            listParam = new HashMap<>();
        }
    }

    private void ensureNormalParamMap() {
        if (Objects.isNull(normalParams)) {
            normalParams = new HashMap<>();
        }
    }

    private String whereClause;
    private Map<String, List> listParam;
    private Map<String, Object> normalParams;

    @JsonPOJOBuilder()
    public static final class Builder {
        private List<Long> listOfId;
        private String templateKey;
        private String templateKeyPrefix;
        private String templateName;
        private String descriptionContains;
        private String version;
        private Boolean active;
        private Integer versionSeq;

        private Builder() {
        }

        public Builder withListOfId(List<Long> listOfId) {
            this.listOfId = listOfId;
            return this;
        }

        public Builder withTemplateKey(String templateKey) {
            this.templateKey = templateKey;
            return this;
        }

        public Builder withTemplateKeyPrefix(String templateKeyPrefix) {
            this.templateKeyPrefix = templateKeyPrefix;
            return this;
        }

        public Builder withTemplateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Builder withDescriptionContains(String descriptionContains) {
            this.descriptionContains = descriptionContains;
            return this;
        }

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder withActive(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder withVersionSeq(Integer versionSeq) {
            this.versionSeq = versionSeq;
            return this;
        }

        public TaskTemplateSearchCriteria build() {
            return new TaskTemplateSearchCriteria(listOfId, templateKey, templateKeyPrefix, templateName,
                descriptionContains, version, active, versionSeq);
        }
    }
}
