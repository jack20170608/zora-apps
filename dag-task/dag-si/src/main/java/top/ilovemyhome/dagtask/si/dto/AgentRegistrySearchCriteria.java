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

@JsonDeserialize(builder = AgentRegistrySearchCriteria.Builder.class)
public class AgentRegistrySearchCriteria implements SearchCriteria {

    private final List<Long> listOfId;
    private final String agentId;
    private final String agentIdPrefix;
    private final String agentUrlPrefix;
    private final Boolean running;
    private final String supportedExecutionKey;

    public AgentRegistrySearchCriteria(List<Long> listOfId, String agentId, String agentIdPrefix, String agentUrlPrefix,
                                   Boolean running, String supportedExecutionKey) {
        this.listOfId = listOfId;
        this.agentId = agentId;
        this.agentIdPrefix = agentIdPrefix;
        this.agentUrlPrefix = agentUrlPrefix;
        this.running = running;
        this.supportedExecutionKey = supportedExecutionKey;

        prepareQuery();
    }

    public List<Long> getListOfId() {
        return listOfId;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentIdPrefix() {
        return agentIdPrefix;
    }

    public String getAgentUrlPrefix() {
        return agentUrlPrefix;
    }

    public Boolean isRunning() {
        return running;
    }

    public String getSupportedExecutionKey() {
        return supportedExecutionKey;
    }

    @Override
    public String toString() {
        return "AgentRegistrySearchCriteria{" +
            "listOfId=" + listOfId +
            ", agentId='" + agentId + '\'' +
            ", agentIdPrefix='" + agentIdPrefix + '\'' +
            ", agentUrlPrefix='" + agentUrlPrefix + '\'' +
            ", running=" + running +
            ", supportedExecutionKey='" + supportedExecutionKey + '\'' +
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
        if (StringUtils.isNotBlank(agentId)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND agent_id = :agentId");
            normalParams.put("agentId", agentId);
        }
        if (StringUtils.isNotBlank(agentIdPrefix)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND agent_id like '%'||:agentIdPrefix||'%'");
            normalParams.put("agentIdPrefix", agentIdPrefix);
        }
        if (StringUtils.isNotBlank(agentUrlPrefix)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND agent_url like '%'||:agentUrlPrefix||'%'");
            normalParams.put("agentUrlPrefix", agentUrlPrefix);
        }
        if (Objects.nonNull(running)) {
            if (running) {
                sqlBuilder.append(" AND running is TRUE ");
            } else {
                sqlBuilder.append(" AND running is FALSE ");
            }
        }
        if (StringUtils.isNotBlank(supportedExecutionKey)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND supported_execution_keys like '%'||:supportedExecutionKey||'%'");
            normalParams.put("supportedExecutionKey", supportedExecutionKey);
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
        private String agentId;
        private String agentIdPrefix;
        private String agentUrlPrefix;
        private Boolean running;
        private String supportedExecutionKey;

        private Builder() {
        }

        public Builder withListOfId(List<Long> listOfId) {
            this.listOfId = listOfId;
            return this;
        }

        public Builder withAgentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder withAgentIdPrefix(String agentIdPrefix) {
            this.agentIdPrefix = agentIdPrefix;
            return this;
        }

        public Builder withAgentUrlPrefix(String agentUrlPrefix) {
            this.agentUrlPrefix = agentUrlPrefix;
            return this;
        }

        public Builder withRunning(Boolean running) {
            this.running = running;
            return this;
        }

        public Builder withSupportedExecutionKey(String supportedExecutionKey) {
            this.supportedExecutionKey = supportedExecutionKey;
            return this;
        }

        public AgentRegistrySearchCriteria build() {
            return new AgentRegistrySearchCriteria(listOfId, agentId, agentIdPrefix, agentUrlPrefix,
                running, supportedExecutionKey);
        }
    }
}
