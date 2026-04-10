package top.ilovemyhome.dagtask.si.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import top.ilovemyhome.dagtask.si.enums.OrderType;
import top.ilovemyhome.zora.jdbi.SearchCriteria;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(builder = TaskOrderSearchCriteria.Builder.class)
public class TaskOrderSearchCriteria implements SearchCriteria {

    private final List<Long> listOfId;
    private final String name;
    private final String namePrefix;
    private final String key;
    private final String keyPrefix;
    private final OrderType orderType;

    public TaskOrderSearchCriteria(List<Long> listOfId, String name, String namePrefix, String key, String keyPrefix, OrderType orderType) {
        this.listOfId = listOfId;
        this.name = name;
        this.namePrefix = namePrefix;
        this.key = key;
        this.keyPrefix = keyPrefix;
        this.orderType = orderType;

        prepareQuery();
    }

    public List<Long> getListOfId() {
        return listOfId;
    }

    public String getName() {
        return name;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public String getKey() {
        return key;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    @Override
    public String toString() {
        return "TaskOrderSearchCriteria{" +
            "listOfId=" + listOfId +
            ", name='" + name + '\'' +
            ", namePrefix='" + namePrefix + '\'' +
            ", key='" + key + '\'' +
            ", keyPrefix='" + keyPrefix + '\'' +
            ", orderType=" + orderType +
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
        if (StringUtils.isNotBlank(name)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND name = :name");
            normalParams.put("name", name);
        }
        if (StringUtils.isNotBlank(namePrefix)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND name like :namePrefix");
            normalParams.put("namePrefix", namePrefix + "%");
        }
        if (StringUtils.isNotBlank(key)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND key = :key");
            normalParams.put("key", key);
        }
        if (StringUtils.isNotBlank(keyPrefix)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND key like :keyPrefix");
            normalParams.put("keyPrefix", keyPrefix + "%");
        }
        if (Objects.nonNull(orderType)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND order_type = :orderType");
            normalParams.put("orderType", orderType);
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
        private String name;
        private String namePrefix;
        private String key;
        private String keyPrefix;
        private OrderType orderType;

        private Builder() {
        }

        public Builder withListOfId(List<Long> listOfId) {
            this.listOfId = listOfId;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withNamePrefix(String namePrefix) {
            this.namePrefix = namePrefix;
            return this;
        }

        public Builder withKey(String key) {
            this.key = key;
            return this;
        }

        public Builder withKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return this;
        }

        public Builder withOrderType(OrderType orderType) {
            this.orderType = orderType;
            return this;
        }

        public TaskOrderSearchCriteria build() {
            return new TaskOrderSearchCriteria(listOfId, name, namePrefix, key, keyPrefix, orderType);
        }
    }
}
