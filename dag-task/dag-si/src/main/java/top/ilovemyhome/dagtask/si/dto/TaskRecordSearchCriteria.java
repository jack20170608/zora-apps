package top.ilovemyhome.dagtask.si.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import top.ilovemyhome.zora.jdbi.SearchCriteria;
import top.ilovemyhome.zora.jdbi.page.Pageable;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(builder = TaskRecordSearchCriteria.Builder.class)
public class TaskRecordSearchCriteria implements SearchCriteria {

    private final List<Long> listOfId;
    private final String orderKey;
    private final String orderKeyPrefix;
    private final String executionKey;
    private final TaskStatus status;
    private final Boolean success;

    public TaskRecordSearchCriteria(List<Long> listOfId, String orderKey, String orderKeyPrefix,
                                    String executionKey, TaskStatus status, Boolean success) {
        this.listOfId = listOfId;
        this.orderKey = orderKey;
        this.orderKeyPrefix = orderKeyPrefix;
        this.executionKey = executionKey;
        this.status = status;
        this.success = success;

        prepareQuery();
    }

    public List<Long> getListOfId() {
        return listOfId;
    }

    public String getOrderKey() {
        return orderKey;
    }

    public String getOrderKeyPrefix() {
        return orderKeyPrefix;
    }

    public String getExecutionKey() {
        return executionKey;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Boolean getSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "TaskRecordSearchCriteria{" +
            "listOfId=" + listOfId +
            ", orderKey='" + orderKey + '\'' +
            ", orderKeyPrefix='" + orderKeyPrefix + '\'' +
            ", executionKey='" + executionKey + '\'' +
            ", status=" + status +
            ", success=" + success +
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
        if (StringUtils.isNotBlank(orderKey)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND order_key = :orderKey");
            normalParams.put("orderKey", orderKey);
        }
        if (StringUtils.isNotBlank(orderKeyPrefix)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND order_key like '%'||:orderKeyPrefix||'%'");
            normalParams.put("orderKeyPrefix", orderKeyPrefix);
        }
        if (StringUtils.isNotBlank(executionKey)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND execution_key = :executionKey");
            normalParams.put("executionKey", executionKey);
        }
        if (Objects.nonNull(status)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND status = :status");
            normalParams.put("status", status.name());
        }
        if (Objects.nonNull(success)) {
            if (success) {
                sqlBuilder.append(" AND success is TRUE ");
            } else {
                sqlBuilder.append(" AND success is FALSE ");
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
        private String orderKey;
        private String orderKeyPrefix;
        private String executionKey;
        private TaskStatus status;
        private Boolean success;

        private Builder() {
        }

        public Builder withListOfId(List<Long> listOfId) {
            this.listOfId = listOfId;
            return this;
        }

        public Builder withOrderKey(String orderKey) {
            this.orderKey = orderKey;
            return this;
        }

        public Builder withOrderKeyPrefix(String orderKeyPrefix) {
            this.orderKeyPrefix = orderKeyPrefix;
            return this;
        }

        public Builder withExecutionKey(String executionKey) {
            this.executionKey = executionKey;
            return this;
        }

        public Builder withStatus(TaskStatus status) {
            this.status = status;
            return this;
        }

        public Builder withSuccess(Boolean success) {
            this.success = success;
            return this;
        }

        public TaskRecordSearchCriteria build() {
            return new TaskRecordSearchCriteria(listOfId, orderKey, orderKeyPrefix, executionKey, status, success);
        }
    }
}
