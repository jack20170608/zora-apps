package top.ilovemyhome.dagtask.core.task;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import top.ilovemyhome.dagtask.si.TaskDispatchRecord;
import top.ilovemyhome.zora.jdbi.SearchCriteria;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(builder = TaskDispatchRecordSearchCriteria.Builder.class)
public class TaskDispatchRecordSearchCriteria implements SearchCriteria {

    private final List<Long> listOfId;
    private final List<Long> listOfTaskId;
    private final String agentId;
    private final String agentUrlPrefix;
    private final List<TaskDispatchRecord.DispatchStatus> statusList;
    private final LocalDateTime dispatchTimeAfter;
    private final LocalDateTime dispatchTimeBefore;

    public TaskDispatchRecordSearchCriteria(List<Long> listOfId, List<Long> listOfTaskId, String agentId, String agentUrlPrefix,
                                             List<TaskDispatchRecord.DispatchStatus> statusList,
                                             LocalDateTime dispatchTimeAfter, LocalDateTime dispatchTimeBefore) {
        this.listOfId = listOfId;
        this.listOfTaskId = listOfTaskId;
        this.agentId = agentId;
        this.agentUrlPrefix = agentUrlPrefix;
        this.statusList = statusList;
        this.dispatchTimeAfter = dispatchTimeAfter;
        this.dispatchTimeBefore = dispatchTimeBefore;

        prepareQuery();
    }

    public List<Long> getListOfId() {
        return listOfId;
    }

    public List<Long> getListOfTaskId() {
        return listOfTaskId;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentUrlPrefix() {
        return agentUrlPrefix;
    }

    public List<TaskDispatchRecord.DispatchStatus> getStatusList() {
        return statusList;
    }

    public LocalDateTime getDispatchTimeAfter() {
        return dispatchTimeAfter;
    }

    public LocalDateTime getDispatchTimeBefore() {
        return dispatchTimeBefore;
    }

    @Override
    public String toString() {
        return "TaskDispatchRecordSearchCriteria{" +
            "listOfId=" + listOfId +
            ", listOfTaskId=" + listOfTaskId +
            ", agentId='" + agentId + '\'' +
            ", agentUrlPrefix='" + agentUrlPrefix + '\'' +
            ", statusList=" + statusList +
            ", dispatchTimeAfter=" + dispatchTimeAfter +
            ", dispatchTimeBefore=" + dispatchTimeBefore +
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
        if (listOfTaskId != null && !listOfTaskId.isEmpty()) {
            ensureListParamMap();
            listParam.put("listOfTaskId", listOfTaskId);
            sqlBuilder.append(" AND task_id in (<listOfTaskId>)");
        }
        if (StringUtils.isNotBlank(agentId)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND agent_id = :agentId");
            normalParams.put("agentId", agentId);
        }
        if (StringUtils.isNotBlank(agentUrlPrefix)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND agent_url like '%'||:agentUrlPrefix||'%'");
            normalParams.put("agentUrlPrefix", agentUrlPrefix);
        }
        if (statusList != null && !statusList.isEmpty()) {
            ensureListParamMap();
            listParam.put("statusList", statusList);
            sqlBuilder.append(" AND status in (<statusList>)");
        }
        if (Objects.nonNull(dispatchTimeAfter)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND dispatch_time >= :dispatchTimeAfter");
            normalParams.put("dispatchTimeAfter", dispatchTimeAfter);
        }
        if (Objects.nonNull(dispatchTimeBefore)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND dispatch_time <= :dispatchTimeBefore");
            normalParams.put("dispatchTimeBefore", dispatchTimeBefore);
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
        private List<Long> listOfTaskId;
        private String agentId;
        private String agentUrlPrefix;
        private List<TaskDispatchRecord.DispatchStatus> statusList;
        private LocalDateTime dispatchTimeAfter;
        private LocalDateTime dispatchTimeBefore;

        private Builder() {
        }

        public Builder withListOfId(List<Long> listOfId) {
            this.listOfId = listOfId;
            return this;
        }

        public Builder withListOfTaskId(List<Long> listOfTaskId) {
            this.listOfTaskId = listOfTaskId;
            return this;
        }

        public Builder withAgentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder withAgentUrlPrefix(String agentUrlPrefix) {
            this.agentUrlPrefix = agentUrlPrefix;
            return this;
        }

        public Builder withStatusList(List<TaskDispatchRecord.DispatchStatus> statusList) {
            this.statusList = statusList;
            return this;
        }

        public Builder withDispatchTimeAfter(LocalDateTime dispatchTimeAfter) {
            this.dispatchTimeAfter = dispatchTimeAfter;
            return this;
        }

        public Builder withDispatchTimeBefore(LocalDateTime dispatchTimeBefore) {
            this.dispatchTimeBefore = dispatchTimeBefore;
            return this;
        }

        public TaskDispatchRecordSearchCriteria build() {
            return new TaskDispatchRecordSearchCriteria(listOfId, listOfTaskId, agentId, agentUrlPrefix,
                statusList, dispatchTimeAfter, dispatchTimeBefore);
        }
    }
}
