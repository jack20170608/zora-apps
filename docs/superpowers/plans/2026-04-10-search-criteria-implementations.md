# SearchCriteria Implementations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `SearchCriteria` classes for `TaskTemplate`, `TaskDispatchRecord`, `TaskOrder`, and `AgentInfo` entities to provide consistent dynamic querying capability across all core DAG entities.

**Architecture:** Follow the exact pattern established by the existing `TaskSearchCriteria`. Each entity gets its own SearchCriteria class in the `top.ilovemyhome.dagtask.core.task` package. Each class implements the `SearchCriteria` interface with dynamic SQL WHERE clause construction, builder pattern, and Jackson JSON serialization support.

**Tech Stack:**
- Java 25
- JDBI with zora `SearchCriteria` interface
- Jackson for JSON serialization (`@JsonDeserialize`, `@JsonPOJOBuilder`)
- Guava `ImmutableMap`
- Apache Commons Lang 3 `StringUtils`
- Existing entity classes from `dag-task-si` module

---

### Task 1: Create TaskTemplateSearchCriteria

**Files:**
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/task/TaskTemplateSearchCriteria.java`

- [ ] **Step 1: Create the file with the complete implementation**

```java
package top.ilovemyhome.dagtask.core.task;

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

@JsonDeserialize(builder = TaskTemplateSearchCriteria.class)
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
```

- [ ] **Step 2: Compile to verify correctness**

Run from project root: `mvn compile -pl dag-task/dag-scheduler -am`
Expected: Compilation succeeds with no errors

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/task/TaskTemplateSearchCriteria.java
git commit -m "feat: add TaskTemplateSearchCriteria implements SearchCriteria"
```

---

### Task 2: Create TaskDispatchRecordSearchCriteria

**Files:**
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/task/TaskDispatchRecordSearchCriteria.java`

- [ ] **Step 1: Create the file with the complete implementation**

```java
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

@JsonDeserialize(builder = TaskDispatchRecordSearchCriteria.class)
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
```

- [ ] **Step 2: Compile to verify correctness**

Run from project root: `mvn compile -pl dag-task/dag-scheduler -am`
Expected: Compilation succeeds with no errors

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/task/TaskDispatchRecordSearchCriteria.java
git commit -m "feat: add TaskDispatchRecordSearchCriteria implements SearchCriteria"
```

---

### Task 3: Create TaskOrderSearchCriteria

**Files:**
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/task/TaskOrderSearchCriteria.java`

- [ ] **Step 1: Create the file with the complete implementation**

```java
package top.ilovemyhome.dagtask.core.task;

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

@JsonDeserialize(builder = TaskOrderSearchCriteria.class)
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
            sqlBuilder.append(" AND name like '%'||:namePrefix||'%'");
            normalParams.put("namePrefix", namePrefix);
        }
        if (StringUtils.isNotBlank(key)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND key = :key");
            normalParams.put("key", key);
        }
        if (StringUtils.isNotBlank(keyPrefix)) {
            ensureNormalParamMap();
            sqlBuilder.append(" AND key like '%'||:keyPrefix||'%'");
            normalParams.put("keyPrefix", keyPrefix);
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
```

- [ ] **Step 2: Compile to verify correctness**

Run from project root: `mvn compile -pl dag-task/dag-scheduler -am`
Expected: Compilation succeeds with no errors

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/task/TaskOrderSearchCriteria.java
git commit -m "feat: add TaskOrderSearchCriteria implements SearchCriteria"
```

---

### Task 4: Create AgentInfoSearchCriteria

**Files:**
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/task/AgentInfoSearchCriteria.java`

- [ ] **Step 1: Create the file with the complete implementation**

```java
package top.ilovemyhome.dagtask.core.task;

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

@JsonDeserialize(builder = AgentInfoSearchCriteria.class)
public class AgentInfoSearchCriteria implements SearchCriteria {

    private final List<Long> listOfId;
    private final String agentId;
    private final String agentIdPrefix;
    private final String agentUrlPrefix;
    private final Boolean running;
    private final String supportedExecutionKey;

    public AgentInfoSearchCriteria(List<Long> listOfId, String agentId, String agentIdPrefix, String agentUrlPrefix,
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
        return "AgentInfoSearchCriteria{" +
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

        public AgentInfoSearchCriteria build() {
            return new AgentInfoSearchCriteria(listOfId, agentId, agentIdPrefix, agentUrlPrefix,
                running, supportedExecutionKey);
        }
    }
}
```

- [ ] **Step 2: Compile to verify correctness**

Run from project root: `mvn compile -pl dag-task/dag-scheduler -am`
Expected: Compilation succeeds with no errors

- [ ] **Step 3: Final compile - entire dag-task module**

Run from project root: `mvn clean compile -pl dag-task -am`
Expected: All classes compile successfully

- [ ] **Step 4: Commit**

```bash
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/task/AgentInfoSearchCriteria.java
git commit -m "feat: add AgentInfoSearchCriteria implements SearchCriteria"
```

---

## Summary of Files Created

| File | Entity | Description |
|------|--------|-------------|
| `TaskTemplateSearchCriteria.java` | TaskTemplate | Search criteria for workflow templates |
| `TaskDispatchRecordSearchCriteria.java` | TaskDispatchRecord | Search criteria for task dispatch records |
| `TaskOrderSearchCriteria.java` | TaskOrder | Search criteria for top-level task orders |
| `AgentInfoSearchCriteria.java` | AgentInfo | Search criteria for registered agents |

All follow the same pattern as the existing `TaskSearchCriteria`.
