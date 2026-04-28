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

@JsonDeserialize(builder = AgentWhitelistSearchCriteria.Builder.class)
public class AgentWhitelistSearchCriteria implements SearchCriteria {

    private final List<Long> listOfId;
    private final String ipSegment;
    private final String ipSegmentPrefix;
    private final String agentId;
    private final String agentIdPrefix;
    private final Boolean enabled;

    public AgentWhitelistSearchCriteria(List<Long> listOfId, String ipSegment, String ipSegmentPrefix,
                                        String agentId, String agentIdPrefix, Boolean enabled) {
        this.listOfId = listOfId;
        this.ipSegment = ipSegment;
        this.ipSegmentPrefix = ipSegmentPrefix;
        this.agentId = agentId;
        this.agentIdPrefix = agentIdPrefix;
        this.enabled = enabled;

        prepareQuery();
    }

    public List<Long> getListOfId() {
        return listOfId;
    }

    public String getIpSegment() {
        return ipSegment;
    }

    public String getIpSegmentPrefix() {
        return ipSegmentPrefix;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentIdPrefix() {
        return agentIdPrefix;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "AgentWhitelistSearchCriteria{" +
            "listOfId=" + listOfId +
            ", ipSegment='" + ipSegment + '\'' +
            ", ipSegmentPrefix='" + ipSegmentPrefix + '\'' +
            ", agentId='" + agentId + '\'' +
            ", agentIdPrefix='" + agentIdPrefix + '\'' +
            ", enabled=" + enabled +
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
        if (StringUtils.isNotBlank(ipSegment)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND ip_segment = :ipSegment");
            normalParams.put("ipSegment", ipSegment);
        }
        if (StringUtils.isNotBlank(ipSegmentPrefix)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND ip_segment like '%'||:ipSegmentPrefix||'%'");
            normalParams.put("ipSegmentPrefix", ipSegmentPrefix);
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
        if (Objects.nonNull(enabled)) {
            if (enabled) {
                sqlBuilder.append(" AND enabled is TRUE ");
            } else {
                sqlBuilder.append(" AND enabled is FALSE ");
            }
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
        private String ipSegment;
        private String ipSegmentPrefix;
        private String agentId;
        private String agentIdPrefix;
        private Boolean enabled;

        private Builder() {
        }

        public Builder withListOfId(List<Long> listOfId) {
            this.listOfId = listOfId;
            return this;
        }

        public Builder withIpSegment(String ipSegment) {
            this.ipSegment = ipSegment;
            return this;
        }

        public Builder withIpSegmentPrefix(String ipSegmentPrefix) {
            this.ipSegmentPrefix = ipSegmentPrefix;
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

        public Builder withEnabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public AgentWhitelistSearchCriteria build() {
            return new AgentWhitelistSearchCriteria(listOfId, ipSegment, ipSegmentPrefix,
                agentId, agentIdPrefix, enabled);
        }
    }
}
