package top.ilovemyhome.dagtask.core.task;

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

@JsonDeserialize(builder = TaskSearchCriteria.class)
public class TaskSearchCriteria implements SearchCriteria {

    private final List<Long> listOfId;
    private final String orderKey;
    private final String orderKeyPrefix;
    private final String executionKey;
    private final String name;
    private final Boolean dummy;
    private final List<TaskStatus> statusList;
    private final Boolean success;

    public TaskSearchCriteria(List<Long> listOfId, String orderKey, String orderKeyPrefix, String executionKey, String name
        , Boolean dummy, List<TaskStatus> statusList, Boolean success) {
        this.listOfId = listOfId;
        this.orderKey = orderKey;
        this.orderKeyPrefix = orderKeyPrefix;
        this.executionKey = executionKey;
        this.name = name;
        this.dummy = dummy;
        this.statusList = statusList;
        this.success = success;

        prepareQuery();

    }

    public List<Long> getListOfId() {
        return listOfId;
    }

    public String getOrderKeyPrefix() {
        return orderKeyPrefix;
    }

    public String getExecutionKey() {
        return executionKey;
    }

    public String getName() {
        return name;
    }

    public boolean isDummy() {
        return dummy;
    }

    public List<TaskStatus> getStatusList() {
        return statusList;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "TaskSearchCriteria{" +
            "listOfId=" + listOfId +
            ", orderKey='" + orderKey + '\'' +
            ", orderKeyPrefix='" + orderKeyPrefix + '\'' +
            ", executionKey='" + executionKey + '\'' +
            ", name='" + name + '\'' +
            ", dummy=" + dummy +
            ", statusList=" + statusList +
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
            sqlBuilder.append(" AND ID in (<listOfId>)");
        }
        if (StringUtils.isNotBlank(orderKey)){
            ensureNormalParamMap();
            sqlBuilder.append(" AND ORDER_KEY = :orderKey");
            normalParams.put("orderKey", orderKey);
        }
        if (StringUtils.isNotBlank(orderKeyPrefix)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND ORDER_KEY like '%'||:orderKeyPrefix||'%'");
            normalParams.put("orderKeyPrefix", orderKeyPrefix);
        }
        if (StringUtils.isNotBlank(executionKey)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND EXECUTION_KEY = :executionKey");
            normalParams.put("executionKey", executionKey);
        }
        if (StringUtils.isNotBlank(name)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND NAME = :name");
            normalParams.put("name", name);
        }
        if (Objects.nonNull(dummy)) {
            if (dummy)
                sqlBuilder.append(" AND DUMMY is TRUE ");
            else
                sqlBuilder.append(" AND DUMMY is FALSE ");
        }
        if (statusList != null && !statusList.isEmpty()) {
            ensureListParamMap();
            sqlBuilder.append(" AND STATUS in (<statusList>)");
            listParam.put("statusList", statusList);
        }
        if (success != null) {
            if (success) {
                sqlBuilder.append(" AND SUCCESS is TRUE ");
            } else {
                sqlBuilder.append(" AND SUCCESS is FALSE ");
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
        private String name;
        private Boolean dummy;
        private List<TaskStatus> statusList;
        private Boolean success;
        private String whereClause;
        private Map<String, List> listParam;
        private Map<String, Object> normalParams;

        private Builder() {
        }


        public Builder withListOfId(List<Long> listOfId) {
            this.listOfId = listOfId;
            return this;
        }

        public Builder withOrderKey(String orderKey) {
            this.orderKey= orderKey;
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

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDummy(Boolean dummy) {
            this.dummy = dummy;
            return this;
        }

        public Builder withStatusList(List<TaskStatus> statusList) {
            this.statusList = statusList;
            return this;
        }

        public Builder withIsSuccess(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder withWhereClause(String whereClause) {
            this.whereClause = whereClause;
            return this;
        }

        public Builder withListParam(Map<String, List> listParam) {
            this.listParam = listParam;
            return this;
        }

        public Builder withNormalParams(Map<String, Object> normalParams) {
            this.normalParams = normalParams;
            return this;
        }

        public TaskSearchCriteria build() {
            return new TaskSearchCriteria(listOfId, orderKey, orderKeyPrefix, executionKey, name, dummy
                , statusList, success);
        }
    }
}
