# Step 2: dag-scheduler 六边形重构 — Domain Migration

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Created**: 2026-06-08（重建，原文件丢失）
**Spec**: `dag-task/docs/superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md`
**Step 1 plan**: `dag-task/docs/superpowers/plans/2026-06-07-dag-scheduler-hexagonal-step1-skeleton.md`
**Architecture doc**: `dag-task/docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`

## 目标

把 `dag-scheduler` 的领域逻辑从旧 `dag-scheduler/core` + `dag-scheduler-muserver` 真正搬到 `dag-scheduler-domain`，让外部调用者（dag-admin、dag-allinone-muserver）改通过 inbound use case 接口访问，让旧 ServiceImpl 退役。本步骤完成后：

- 13 个 application service 类（实现 15 个 inbound use case）位于 `dag-scheduler-domain/scheduler/application` 包，**零基础设施依赖**。
- 8 个旧 `*DaoJdbiImpl` 同时实现新 `port.out` Repository 接口，使 adapter 不必额外写 wrapper（步骤 3 再迁 adapter）。
- `DefaultTaskDispatcher` 额外实现新 `port.out.AgentDispatcher`。
- dag-admin 与 dag-allinone-muserver 改通过 use case 调用。
- 启用 `HexagonalArchitectureTest`（含临时豁免 `zora-jdbi.page.*`）。
- 旧 `*ServiceImpl` 保留 `@Deprecated forRemoval=true`；真正物理删除留到 step 2 末尾的 Task 12.5（与 dag-si 旧目录一起清掉）。

**DoD**（验收标准）：
1. `mvn -pl dag-task -am clean verify -pl '!dag-agent,!dag-agent-muserver,!dag-agent-cli'` 全绿。
2. `HexagonalArchitectureTest` 三条规则全通过（去 `@Disabled`）。
3. `api-test/` 下原有 scheduler / admin HTTP 测试全部通过。
4. `dag-allinone` 启动整测通过。

## Task 完成状态总览

| Task | 描述 | 状态 | Commit |
|---|---|---|---|
| 1 | 清洗 TaskInput.getInputAs + 9 callers | ✅ | d69ce65 |
| 2 | 定义 15 outbound ports（3 exception + 4 cross-cutting + 8 Repository） | ✅ | b131878 |
| 3 | 定义 15 UseCase + 6 domain exception；scheduler-domain pom 加 zora-jdbi TEMPORARY（TD-1） | ✅ | 0d34916 |
| 4 | 搬 7 个 domain helper 到 dag-scheduler-domain；删 TaskHelper duplicate | ✅ | 226720a |
| 5 | **补齐 outbound Repository port 缺失方法 + 新增 2 个 cross-cutting port**（Task 6 阻塞前置） | ⏳ | — |
| 6 | 实现 13 个 application services（最大头） | ⏳ | — |
| 7 | 8 个 DaoJdbiImpl 同时 implement 新接口；DefaultTaskDispatcher 实现 AgentDispatcher；旧 *ServiceImpl 标 @Deprecated | ⏳ | — |
| 8 | dag-admin/WorkflowApi 改调 use case | ⏳ | — |
| 9 | dag-allinone-muserver 3 文件 + 2 测试装配点更新 | ⏳ | — |
| 10 | 清理 dag-si pom 无用依赖（zora-jdbi provided 是否能去掉的评估） | ⏳ | — |
| 11 | 启用 HexagonalArchitectureTest（去 @Disabled + zora-jdbi.page.* 临时豁免） | ⏳ | — |
| 12 | 全 repo verify + 启动整测（exclude dag-agent / dag-agent-muserver / dag-agent-cli） | ⏳ | — |
| 12.5 | 真正删 dag-si 旧 `service.*` 接口 + 删 @Deprecated ServiceImpl + TaskOrder/TaskTemplate @JsonDeserialize 评估 | ⏳ | — |
| 13 | 更新 CLAUDE.md + docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md（标 step 2 完成） | ⏳ | — |
| 14 | 最终 stage（不 commit；人工 commit per CLAUDE.md 约定） | ⏳ | — |

---

## Tasks 5–14 详细分解

### Task 5: 补齐 outbound Repository port 缺失方法 + 新增 cross-cutting port

**为什么先做**：Task 6 的 application services 直接调 `xxxRepository.create/update/find/count`，当前 port 接口只搬了"动作型"方法（updateStatus / deactivateVersion / search 等），但 `createOrder`、`createTemplate`、`updateTemplate`、`findTemplate(criteria)`、`count(criteria)`、`find(criteria, pageable)` 等都缺。同时旧 `DagManageServiceImpl.instantiateFromTemplate` 内部需要 JSON 解析（template 字段是 JSON 字符串），不能进 pure domain；需通过新 outbound port `DagDefinitionParser` 反转。同理 token 签发需 `TokenIssuer` port。

若不先补，Task 6 编译时会大量 "cannot find symbol"。

#### Files
- Modify: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/port/out/TaskOrderRepository.java`
- Modify: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/port/out/TaskRecordRepository.java`
- Modify: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/port/out/TaskTemplateRepository.java`
- Create: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/port/out/DagDefinitionParser.java`
- Create: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/port/out/TokenIssuer.java`

#### Step 5.1: `TaskOrderRepository`
- [ ] 加 `Long create(TaskOrder taskOrder)`（旧 `TaskOrderDao.create` 同签名）

#### Step 5.2: `TaskRecordRepository`
- [ ] 加 `int count(TaskRecordSearchCriteria criteria)`（旧 `DagQueryServiceImpl.findAll` 用来限制 MAX_QUERY_SIZE，旧 `TaskOrderServiceImpl.deleteOrderByKey` 计数用）
- [ ] 加 `List<TaskRecord> find(TaskRecordSearchCriteria criteria)`（无分页；旧 `DagManageServiceImpl.findTasksByOrderKey`、`TaskDagServiceImpl.findByOrderKey/findByStatus`、`DagQueryServiceImpl.findAll` 都用）
- [ ] 加 `Page<TaskRecord> find(TaskRecordSearchCriteria criteria, Pageable pageable)`（旧 `DagQueryServiceImpl.find` 用；类型 `top.ilovemyhome.zora.jdbi.page.Page/Pageable`，TD-1 临时容忍）
- [ ] 加 `Long create(TaskRecord record)`（旧 `DagManageServiceImpl.createTasks/createTasksFromDagDefinition` 调 `taskRecordDao.create(task)` 后取 ID）
- [ ] 加 `int update(Long id, TaskRecord record)`（旧 `createTasksFromDagDefinition` 第二趟更新 successorIds 时用 `taskRecordDao.update(existingTask.getId(), updatedTask)`）

> 现存 `loadTaskById` vs 旧 `findOne`：两者语义等价。Task 5 不改 port 名，application service 统一用 `loadTaskById`；Task 7 让 DaoJdbiImpl 同时满足新旧两套接口（两个名字都返回同一查询）。

#### Step 5.3: `TaskTemplateRepository`
- [ ] 加 `boolean create(TaskTemplate template)`（旧 `TaskTemplateDao.create`；返回 bool 还是 void 跟旧签名一致即可——核对 `TaskTemplateDao.create` 真实返回类型）
- [ ] 加 `int update(Long id, TaskTemplate template)`（旧 `TaskTemplateServiceImpl.updateTemplate` 调 `taskTemplateDao.update(template.getId(), template)`）
- [ ] 加 `List<TaskTemplate> find(TaskTemplateSearchCriteria criteria)`（旧 `findAll`、`findByKey` 两处用）
- [ ] 加 `Page<TaskTemplate> find(TaskTemplateSearchCriteria criteria, Pageable pageable)`（旧 `TaskTemplateServiceImpl.find` 用）

#### Step 5.4: 新增 `DagDefinitionParser` outbound port
- [ ] 创建文件，单方法接口：
  ```java
  package top.ilovemyhome.dagtask.scheduler.port.out;
  import top.ilovemyhome.dagtask.scheduler.port.in.SubmitDagRunUseCase.DagDefinition;
  import java.util.Map;
  /**
   * Outbound port: parses raw JSON strings stored in TaskTemplate into typed
   * domain values. Implementation lives in an adapter (e.g. JacksonDagDefinitionParser
   * in dag-allinone-muserver / dag-scheduler-app), keeping the pure domain free
   * from Jackson and other JSON libraries.
   */
  public interface DagDefinitionParser {
      DagDefinition parseDag(String json);
      Map<String, String> parseParameterDefaults(String parameterSchemaJson);
  }
  ```

#### Step 5.5: 新增 `TokenIssuer` outbound port
- [ ] 创建文件：
  ```java
  package top.ilovemyhome.dagtask.scheduler.port.out;
  /**
   * Outbound port: issues agent registration tokens. Implementation delegates to
   * legacy TokenService (LegacyTokenIssuer) until step 3 owns the issuance flow.
   */
  public interface TokenIssuer {
      TokenInfo issueAgentToken(String agentId, String name, String description, int validDays, String issuer);
      record TokenInfo(String tokenId, String tokenValue, long expiresAtMillis) {}
  }
  ```

#### Step 5.6: 验证
- [ ] Run: `mvn -f dag-task/pom.xml -pl dag-scheduler-domain compile -q`
- [ ] Expected: BUILD SUCCESS。`Page`/`Pageable` 来自 zora-jdbi（TD-1 已加依赖）。
- [ ] ArchUnit 仍 `@Disabled`，本步骤不解禁。

#### 设计要点
- 新方法签名与旧 `*Dao` 接口对齐（`Long create / int update / Page find` 等），以便 Task 7 的 `*DaoJdbiImpl` 同时实现两套接口时**0 改动**。
- 不复制旧 Dao 中 SQL/Jdbi 相关的低阶方法。
- `Page/Pageable` 留 TD-1 注释（与已有 `QueryDagTasksUseCase.find` 注释一致）。
- 2 个新 cross-cutting port 让 domain 不再依赖 Jackson 和 zora `TokenService`，是 ArchUnit 启用的关键前置。

#### Commit message（人工执行）
```
refactor(scheduler-domain): expand outbound ports for application services (step 2/Task 5)

Pre-requisite for Task 6 (13 application services).

- TaskOrderRepository: add create(TaskOrder): Long
- TaskRecordRepository: add count/find(criteria)/find(criteria,pageable)/create/update
- TaskTemplateRepository: add create/update/find(criteria)/find(criteria,pageable)
- New port: DagDefinitionParser (JSON parsing inversion)
- New port: TokenIssuer (token issuance inversion)

Repository signatures mirror legacy *Dao methods so the existing *DaoJdbiImpl
can implement both old and new interfaces with zero changes in Task 7.

Page/Pageable types still leak via zora-jdbi (TD-1, see step 3).
```

---

### Task 6: 实现 13 个 application services

**最大工作量任务（估约 4 小时）**。每个 service 一个 `.java` 文件（部分合并），全部位于 `top.ilovemyhome.dagtask.scheduler.application` 包下（与现有 6 个 domain exception 同包）。

#### 全局规则（适用于所有 service）

1. **构造器注入**：所有依赖通过构造器传入，private final 字段，`Objects.requireNonNull` 校验。
2. **事务**：跨多次 outbound 调用且需要原子性的，用 `UnitOfWork.execute(...)` 包裹；**禁止**直接 `jdbi.inTransaction`。
3. **ID 生成**：通过 `IdGenerator.nextTaskId()` / `nextOrderId()`，**禁止** `taskRecordRepository.getNextId()`（标了 `@Deprecated forRemoval` 的旧路径仅给 DaoJdbiImpl 用）。
4. **时间**：通过 `Clock.now()`，**禁止**直接 `LocalDateTime.now()` / `Instant.now()`。注：当前 `TaskRecordRepository.start/stop` 签名收的是 `LocalDateTime`；过渡期 service 把 `clock.now()` 转 `LocalDateTime.ofInstant(clock.now(), ZoneId.systemDefault())` 后传入，TD-2 待 step 3 把 port 也改成 `Instant`。
5. **agent dispatch**：通过 `AgentDispatcher` outbound port，**禁止**直接 HttpClient。Task 7 让 `DefaultTaskDispatcher` 实现这个 port。
6. **异常翻译**：domain 决策路径抛 `DomainException` 子类（`DagNotFoundException` 等）。捕获 `PersistenceException` 用于重试/降级判断，不上抛到 web adapter。
7. **JSON 解析责任前移**：旧 `DagManageServiceImpl` 里 `objectMapper.readTree` / `JsonNode` 等代码**不进** application service；对外接收：`SubmitDagRunUseCase` 直接接 `DagDefinition` record（web adapter 负责把 raw JSON parse），内部场景（如 `InstantiateDagTemplateUseCase` 从模板字段读 JSON）通过 `DagDefinitionParser` outbound port。
8. **包结构**：每个 service 一个文件；不写 service interface（自身就实现 inbound use case 接口）。
9. **禁止 application 互调 inbound port**（spec §4.4 + ArchUnit 第 2 条规则）。两个 service 需要共享逻辑时，提取到 `domain.*` 包下纯 domain service / policy 类，两边各自注入 ports 共同调用。

#### 13 个 Service 详单

> 经核对 15 个 UseCase 与旧 ServiceImpl 后的最终映射：

| # | Service 类 | 实现 UseCase | 来源 |
|---|---|---|---|
| 1 | `SubmitDagRunService` | `SubmitDagRunUseCase` | `DagManageServiceImpl.createTasks` + `createTasksFromDagDefinition`；`TaskDagServiceImpl.createTasks` |
| 2 | `InstantiateDagTemplateService` | `InstantiateDagTemplateUseCase` | `DagManageServiceImpl.instantiateFromTemplate`（两个重载 + helpers `resolveParameters` / `findTemplateByKey` / `substituteParameters`） |
| 3 | `ScheduleDagRunService` | `ScheduleDagRunUseCase` | `DagScheduleServiceImpl.start/findReadyTasks/triggerReadyTasks/onTaskCompleted` |
| 4 | `ManualTaskOperationService` | `ManualTaskOperationUseCase` | `DagScheduleServiceImpl.runNow/forceOk/kill/hold/resume` |
| 5 | `DispatchTaskService` | `DispatchTasksUseCase` | `DefaultTaskDispatcher.dispatch(TaskRecord)`（agent 选择 + filter + 调 AgentDispatcher） |
| 6 | `QueryDagTasksService` | `QueryDagTasksUseCase` | `DagQueryServiceImpl.getTask/findAll/find` + `TaskDagServiceImpl.findByOrderKey/findTaskByOrderKey/findByStatus` |
| 7 | `QueryDagRunStatusService` | `QueryDagRunStatusUseCase` | `TaskDagServiceImpl.isSuccess` |
| 8 | `AllocateTaskIdsService` | `AllocateTaskIdsUseCase` | `TaskDagServiceImpl.getNextTaskIds` |
| 9 | `TaskOrderApplicationService` | `ManageTaskOrderUseCase` + `QueryTaskOrderUseCase` | `TaskOrderServiceImpl.createOrder/updateOrderByKey/deleteOrderByKey/isOrdered` |
| 10 | `TaskTemplateApplicationService` | `ManageTaskTemplateUseCase` + `QueryTaskTemplateUseCase` | `TaskTemplateServiceImpl.create/update/deactivate/delete/findAll/find` |
| 11 | `RegisterAgentService` | `RegisterAgentUseCase` | `DefaultAgentRegistryService.registerAgent/unregisterAgent` |
| 12 | `AgentHeartbeatService` | `AgentHeartbeatUseCase` | `DefaultAgentRegistryService.reportAgentStatus` |
| 13 | `ReportTaskResultService` | `ReportTaskResultUseCase` | `DefaultAgentRegistryService.reportTaskResult`（两 overload） |

> 9/10 合并是因为同一聚合根的 read/write 通常共用大量私有 helper（参数校验、entity 构造），物理拆开会反复抽取重复 helper。

#### 6.1 `SubmitDagRunService implements SubmitDagRunUseCase`
- **deps**：`TaskOrderRepository`, `TaskRecordRepository`, `UnitOfWork`, `IdGenerator`, `Clock`
- **methods**：
  - `submitTasks(List<TaskRecord>)`：搬旧 `DagManageServiceImpl.createTasks` 全部逻辑，把 `jdbi.inTransaction` 改成 `uow.execute(...)`，把 `taskOrderDao.findByKey` → `taskOrderRepository.findByKey`，`taskRecordDao.create(r)` → `taskRecordRepository.create(r)`。`DagHelper.visitDAG` 用法保持（已经在 domain 包）。
  - `submitFromDefinition(DagDefinition, orderKey, parameters)`：搬旧 `createTasksFromDagDefinition`，**但 JSON 解析删除**——直接遍历 `definition.tasks()`（每个是 `TaskDefinition` record，字段已 typed）。`substituteParameters` 方法整段保留搬过来当 private helper。
- **特别注意**：旧代码两次 `taskRecordDao.create` + `update`（先创建再回填 successorIds）。新 service 用 `uow.execute` 保整体原子。

#### 6.2 `InstantiateDagTemplateService implements InstantiateDagTemplateUseCase`
- **deps**：`TaskOrderRepository`, `TaskTemplateRepository`, `TaskRecordRepository`, `UnitOfWork`, `IdGenerator`, `Clock`, `DagDefinitionParser`
- **methods**：两个 `instantiateFromTemplate` 重载
- **关键改造**：旧实现里 `template.getDagDefinition()` 是 JSON 字符串，通过 `dagDefinitionParser.parseDag(...)` 解析为 typed `DagDefinition`；`template.getParameterSchema()` 同理通过 `parseParameterDefaults(...)`。本 service **不依赖** `SubmitDagRunUseCase`（避免 application 互调），而是直接调 `TaskOrderRepository.create + TaskRecordRepository.create` 完成实例化。这意味着 6.1 和 6.2 内部各自有一段相似的"DAG 节点→TaskRecord"构造逻辑——决议：把该逻辑抽到 `domain.dag.DagInstantiator` 纯 domain 类（无 port 依赖，输入 `DagDefinition + orderKey + idGenerator`，输出 `List<TaskRecord>`），两 service 共用。
- **method body**：
  - `taskTemplateRepository.find(criteria)` 拿模板；若空返回 `Optional.empty()`（与旧实现 lenient 语义一致）
  - 用 `uow.execute(...)` 包装：创建 TaskOrder + 创建所有 TaskRecord。

#### 6.3 `ScheduleDagRunService implements ScheduleDagRunUseCase`
- **deps**：`TaskRecordRepository`, `AgentDispatcher`, `AgentStatusRepository`, `TaskDispatchRepository`, `UnitOfWork`, `Clock`, `LoadBalanceStrategy`
- **methods**：`start`, `findReadyTasks`, `triggerReadyTasks`, `onTaskCompleted`
- **关键点**：
  - 旧 `triggerReadyTasks` 内部 `taskDispatcher.dispatch(task)` 直接拿到 `DispatchResult`，包含选 agent + HTTP 调用 + 持久化 dispatch record 全部混在一起。本 service 必须**只**调 `AgentDispatcher.dispatch(targetAgent, task)`，**选 agent 由谁负责？**——抽到 `domain.dispatcher.AgentSelector`（pure domain，无 port 依赖）：
    ```
    domain/dispatcher/AgentSelector.java  // pure: select(List<AgentStatus>, executionKey, LoadBalanceStrategy)
    ```
  - `triggerReadyTasks(orderKey)`：
    1. `findReadyTasks(orderKey)`
    2. 对每个 task：`activeAgents = agentStatusRepository.findAllActive()` → `selected = AgentSelector.select(activeAgents, task.executionKey, loadBalance)` → `agentDispatcher.dispatch(selected, task)`（throws AgentUnreachableException）→ `taskRecordRepository.updateStatus(task.id, DISPATCHED)`，写 TaskDispatchRecord。
  - `onTaskCompleted` 中级联触发后继 ready 的逻辑抽到 `domain.dag.TaskCompletionPolicy.onCompleted(...)`（pure），返回"应该被触发的 successor IDs"列表，service 用 ports 落地。

#### 6.4 `ManualTaskOperationService implements ManualTaskOperationUseCase`
- **deps**：`TaskRecordRepository`, `AgentDispatcher`, `AgentStatusRepository`, `TaskDispatchRepository`, `UnitOfWork`, `Clock`, `LoadBalanceStrategy`
- **methods**：`runNow/forceOk/kill/hold/resume`，依旧搬旧实现，按上述规则改造。
- `forceOk` 内部沿用 `TaskCompletionPolicy.onCompleted(...)`（与 6.3 共享）。

#### 6.5 `DispatchTaskService implements DispatchTasksUseCase`
- **deps**：`AgentStatusRepository`, `TaskDispatchRepository`, `AgentDispatcher`, `Clock`, `LoadBalanceStrategy`
- **method**：`DispatchResult dispatch(TaskRecord)`
  - 旧逻辑：findAllActive → filter execKey → filter capacity → loadBalance.select → submitToAgent → 写 TaskDispatchRecord
  - 新实现：上述步骤搬入（复用 `AgentSelector`）；HTTP 部分由 `AgentDispatcher.dispatch(agent, task)` 抽象代替。返回值 `DispatchResult` 继续用 dag-si 的同名 record。
  - **去掉的代码**：所有 `HttpClient` / `HttpRequest` / Jackson serialization / `MediaType` 直接使用——这些进 Task 7 的 `DefaultTaskDispatcher`（adapter 角色，实现 AgentDispatcher port）

#### 6.6 `QueryDagTasksService implements QueryDagTasksUseCase`
- **deps**：`TaskRecordRepository`
- **methods**：直接 delegate；唯一非平凡逻辑是 `findAll` 里 `count > MAX_QUERY_SIZE` 校验（搬 `dag-si.Constants.MAX_QUERY_SIZE`）。
- `findTaskByOrderKey` 与 `findByOrderKey` 都委托给 `taskRecordRepository.find(criteria)`/`search(criteria)`——为保留旧语义，前者用 `search`、后者用 `find`（旧实现就是这样）。

#### 6.7 `QueryDagRunStatusService implements QueryDagRunStatusUseCase`
- **deps**：`TaskRecordRepository`
- **method**：`isSuccess(orderKey)` 直接搬旧 `TaskDagServiceImpl.isSuccess`（`isOrdered && isSuccess`）。

#### 6.8 `AllocateTaskIdsService implements AllocateTaskIdsUseCase`
- **deps**：`IdGenerator`
- **method**：`getNextTaskIds(count)` 循环 `idGenerator.nextTaskId()`，返回 `List.copyOf(ids)`（不再 `ImmutableList.copyOf`，去 Guava）。

#### 6.9 `TaskOrderApplicationService implements ManageTaskOrderUseCase, QueryTaskOrderUseCase`
- **deps**：`TaskOrderRepository`, `TaskRecordRepository`, `UnitOfWork`
- **methods**：
  - `isOrdered`：直接 `taskOrderRepository.findByKey(key).isPresent()`
  - `createOrder`：`Objects.requireNonNull`；若已存在抛 `OrderKeyAlreadyExistsException`（已存在于 application 包，Task 3 已建）
  - `updateOrderByKey`：若不存在抛 `DagNotFoundException`
  - `deleteOrderByKey`：用 `uow.execute(...)` 包 `taskRecordRepository.count(...)` + `taskOrderRepository.deleteByKey` + `taskRecordRepository.deleteByOrderKey`（与旧实现 `jdbi.useTransaction` 等价）

#### 6.10 `TaskTemplateApplicationService implements ManageTaskTemplateUseCase, QueryTaskTemplateUseCase`
- **deps**：`TaskTemplateRepository`
- **methods**：直接搬 `TaskTemplateServiceImpl` 全部 6 个方法，把 `taskTemplateDao` → `taskTemplateRepository`。
- 旧 `update` 内部调 `template.incrementVersionSeq()` 保留——这是 entity 自身的方法，属于 domain。

#### 6.11 `RegisterAgentService implements RegisterAgentUseCase`
- **deps**：`AgentRepository`, `AgentStatusRepository`, `AgentWhitelistRepository`, `TokenIssuer`, `UnitOfWork`, `Clock`
- **methods**：`registerAgent(req, clientIp)`, `unregisterAgent(unreg)`
- 旧 `DefaultAgentRegistryService` 的 in-memory `agentCache` **不进** application service——cache 是基础设施关注点（TD-3 待 step 3 评估）。本 service 每次都查 `agentStatusRepository`。
- `loadAllFromDatabase` 启动 warm cache → 删除。
- whitelist 校验逻辑（`isAllowedByWhitelist`）搬 domain service `domain.agent.WhitelistMatcher`，纯 pure（输入 `clientIp + List<AgentWhitelist>` → boolean，内部用 `IpAddressMatcher` 这个已搬的 domain util）。

#### 6.12 `AgentHeartbeatService implements AgentHeartbeatUseCase`
- **deps**：`AgentStatusRepository`, `Clock`
- **method**：`reportAgentStatus(report)`：检查 `exists` → `updateStatus(...)`。

#### 6.13 `ReportTaskResultService implements ReportTaskResultUseCase`
- **deps**：`TaskRecordRepository`, `Clock`
- **methods**：`reportTaskResult(单)` 与 batch overload。
- 关键点：旧实现传入 `taskRecord.getStatus()` 给 `stop()`，这显然是 bug（应传 `output.success ? SUCCESS : ERROR`），但本次重构**保持行为不变**，bug 留作 TD-4。

#### 6.X 新增的 pure-domain 辅助类
- `domain/dag/DagInstantiator.java` — 6.1 + 6.2 共用
- `domain/dag/TaskCompletionPolicy.java` — 6.3 + 6.4 共用
- `domain/dispatcher/AgentSelector.java` — 6.3 + 6.4 + 6.5 共用
- `domain/agent/WhitelistMatcher.java` — 6.11 用

这些类**零依赖 outbound port**（计算输入由 service 用 port 取来传入），保持 `domain` 包"零基础设施"的 ArchUnit 规则。

#### Step 6.X: 验证
- [ ] Run: `mvn -f dag-task/pom.xml -pl dag-scheduler-domain compile -q` — 必须 BUILD SUCCESS（仅编译，无 test）
- [ ] Run: `mvn -f dag-task/pom.xml -pl dag-scheduler-domain test -q` — SmokeTest 通过，HexagonalArchitectureTest 仍 @Disabled。
- [ ] **本 Task 不写单元测试**——单测放到 Task 12 之后单独补，因 fake repository 工作量本身约 2hr，与本 task 解耦更易并行。

#### Commit message（人工）
```
refactor(scheduler-domain): implement 13 application services (step 2/Task 6)

Implements 15 inbound use cases via 13 application service classes
under top.ilovemyhome.dagtask.scheduler.application:

- SubmitDagRunService                     -> SubmitDagRunUseCase
- InstantiateDagTemplateService           -> InstantiateDagTemplateUseCase
- ScheduleDagRunService                   -> ScheduleDagRunUseCase
- ManualTaskOperationService              -> ManualTaskOperationUseCase
- DispatchTaskService                     -> DispatchTasksUseCase
- QueryDagTasksService                    -> QueryDagTasksUseCase
- QueryDagRunStatusService                -> QueryDagRunStatusUseCase
- AllocateTaskIdsService                  -> AllocateTaskIdsUseCase
- TaskOrderApplicationService             -> ManageTaskOrderUseCase + QueryTaskOrderUseCase
- TaskTemplateApplicationService          -> ManageTaskTemplateUseCase + QueryTaskTemplateUseCase
- RegisterAgentService                    -> RegisterAgentUseCase
- AgentHeartbeatService                   -> AgentHeartbeatUseCase
- ReportTaskResultService                 -> ReportTaskResultUseCase

Adds pure-domain helpers in scheduler.domain.*:
- DagInstantiator, TaskCompletionPolicy, AgentSelector, WhitelistMatcher

ArchUnit still @Disabled — enabled in Task 11.
```

---

### Task 7: 8 个 DaoJdbiImpl 实现新 port + DefaultTaskDispatcher 实现 AgentDispatcher

**目标**：让旧基础设施类**同时**满足新旧两套接口，使得 dag-allinone-muserver / dag-admin 切换到 use case 时**不需要新 adapter wrapper**——直接复用现有 DaoJdbiImpl 实例（已经在旧 module 里、已 DI 装配好）。Step 3 才把它们物理搬入 `dag-scheduler-adapter-persistence-jdbc`。

#### 8 个 DaoJdbiImpl

| DaoJdbiImpl | 当前接口 | 新增 implements |
|---|---|---|
| `AgentDaoJdbiImpl` | `AgentDao` | `AgentRepository` |
| `AgentStatusDaoJdbiImpl` | `AgentStatusDao` | `AgentStatusRepository` |
| `AgentTokenDaoJdbiImpl` | `AgentTokenDao` | `AgentTokenRepository` |
| `AgentWhitelistDaoJdbiImpl` | `AgentWhitelistDao` | `AgentWhitelistRepository` |
| `TaskDispatchDaoJdbiImpl` | `TaskDispatchDao` | `TaskDispatchRepository` |
| `TaskOrderDaoJdbiImpl` | `TaskOrderDao` | `TaskOrderRepository` |
| `TaskRecordDaoJdbiImpl` | `TaskRecordDao` | `TaskRecordRepository` |
| `TaskTemplateDaoJdbiImpl` | `TaskTemplateDao` | `TaskTemplateRepository` |

#### 每个 DaoJdbiImpl 改造
- [ ] 加 `implements <NewRepository>` 到 class 声明
- [ ] 若新 port 比旧 Dao 多了方法（不应该——Task 5 已对齐），新增的方法此处实现（用 jdbi handle 实现 SQL）
- [ ] 若新 port 方法签名与旧 Dao 完全一致，**无需添加任何代码**——同一方法满足两份合约
- [ ] **不要**移除旧 Dao 接口的方法

#### `DefaultTaskDispatcher` 改造
- [ ] 加 `implements port.out.AgentDispatcher`
- [ ] 实现新方法 `DispatchAck dispatch(AgentStatus targetAgent, TaskRecord task)`：搬旧 `submitToAgent` 主体逻辑（HTTP POST + 解析 response code）；网络异常翻译为 `AgentUnreachableException`；agent 拒绝时返回 `new DispatchAck(false, msg)`，成功返回 `new DispatchAck(true, "")`。
- [ ] **保留**旧 `dispatch(TaskRecord)` 方法（标 `@Deprecated forRemoval=true, since="step 2"`）和所有 `killTask` / `forceOkTask` / `findAllActiveAgents` 等——继续给 `dag-allinone-muserver/InProcessTaskDispatcher` 与旧调用者使用。
- [ ] **不要**让 DispatchTaskService 依赖这个类——它通过 `AgentDispatcher` port 注入。

#### 旧 `*ServiceImpl` 加 `@Deprecated`
- [ ] 7 个旧实现：`DagManageServiceImpl`、`DagScheduleServiceImpl`、`DagQueryServiceImpl`、`TaskTemplateServiceImpl`、`TaskOrderServiceImpl`、`TaskDagServiceImpl`、`DefaultAgentRegistryService` 类声明加 `@Deprecated(forRemoval=true, since="step 2 hexagonal refactor")`，加 JavaDoc `@deprecated` 标签说明替代物。
- [ ] **不删除**——dag-allinone-muserver 步骤 9 切完依赖才安全删（Task 12.5）。

#### 验证
- [ ] Run: `mvn -f dag-task/pom.xml -pl dag-scheduler -am compile -q` → BUILD SUCCESS
- [ ] Run: `mvn -f dag-task/pom.xml -pl dag-scheduler test -q` → 旧测试仍 GREEN（不应有方法签名冲突）

#### Commit message（人工）
```
refactor(scheduler): adapt legacy DaoJdbiImpl + DefaultTaskDispatcher to new ports (step 2/Task 7)

- 8 *DaoJdbiImpl now implements both legacy *Dao and new port.out.*Repository
- DefaultTaskDispatcher additionally implements port.out.AgentDispatcher;
  legacy dispatch(TaskRecord) kept as @Deprecated forRemoval
- 7 legacy *ServiceImpl annotated @Deprecated; actual removal in Task 12.5
  once dag-admin / dag-allinone-muserver have migrated to use cases

Legacy paths still work — no runtime behavior change.
```

---

### Task 8: dag-admin/WorkflowApi 改调 use case

#### 调研结果
唯一受影响文件：`dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/WorkflowApi.java`

引用：
- `DagManageService.instantiateFromTemplate` → 改 `InstantiateDagTemplateUseCase.instantiateFromTemplate`
- `TaskTemplateService.find/findAll/createTemplate/updateTemplate/deleteVersion` → 改 `QueryTaskTemplateUseCase` + `ManageTaskTemplateUseCase`

`AgentAdminApi/AgentWhitelistAdminApi/ExecutionApi/StatsApi` **不引用** scheduler service，无需改。

#### Step 8.1
- [ ] 替换 2 个 field 类型：
  - `private final TaskTemplateService taskTemplateService;` → 2 个字段 `QueryTaskTemplateUseCase queryTemplate;` + `ManageTaskTemplateUseCase manageTemplate;`
  - `private final DagManageService dagManageService;` → `InstantiateDagTemplateUseCase instantiateUseCase;`
- [ ] 构造器改成接收 3 个 use case
- [ ] 6 个方法体里调用替换：`taskTemplateService.find(criteria, page)` → `queryTemplate.find(criteria, page)`；`createTemplate/updateTemplate/deleteVersion` → `manageTemplate.*`；`dagManageService.instantiateFromTemplate` → `instantiateUseCase.instantiateFromTemplate`
- [ ] import 同步替换

#### Step 8.2: pom 依赖
- [ ] `dag-admin/pom.xml` 加 `dag-scheduler-domain` 依赖（之前依赖 `dag-si` 的 service 接口；现在需要新模块的 inbound port 接口）

#### 验证
- [ ] Run: `mvn -f dag-task/pom.xml -pl dag-admin -am compile -q` → BUILD SUCCESS
- [ ] Run: `mvn -f dag-task/pom.xml -pl dag-admin test -q` → GREEN

#### Commit message
```
refactor(admin): WorkflowApi switches to use case ports (step 2/Task 8)

Drops dependencies on legacy DagManageService and TaskTemplateService;
now consumes inbound ports: InstantiateDagTemplateUseCase,
QueryTaskTemplateUseCase, ManageTaskTemplateUseCase.
```

---

### Task 9: dag-allinone-muserver 装配点更新

#### 受影响文件（核对结果）

| 文件 | 当前用法 | 改动 |
|---|---|---|
| `application/AllInOneAppContext.java` | 引用 `DagScheduleService dagScheduleService = dagSchedulerServer.getDagScheduleService();` | 改为引用 `ScheduleDagRunUseCase`（从 use-case 工厂取得） |
| `application/AllInOneWebServerBootstrap.java` | `new TaskTemplateApi(...)`, `new DagManageApi(...)`, `new WorkflowApi(...)`, `new AgentRegistryApi(...)` 用 `schedulerServer.getXxxService()` | 改为通过 use case 字段注入 |
| `client/InProcessSchedulerClient.java` | `private final DagScheduleService dagScheduleService;` | 改为 `ScheduleDagRunUseCase` |
| `client/InProcessSchedulerClientTest.java`（test） | `@Mock private DagScheduleService dagScheduleService;` | 改 `@Mock ScheduleDagRunUseCase` |
| `dispatcher/InProcessTaskDispatcherTest.java`（test） | 使用 `TaskDispatcher` 接口 | 大概率无需改（in-process dispatcher 不走新 port） |

#### Step 9.1: 提供 use case 实例
- [ ] `AllInOneAppContext` 内构造各 application service：
  ```java
  Clock clock = new SystemClock();
  IdGenerator idGen = new SequenceIdGenerator(jdbi);   // adapter，本 task 内简易实现
  UnitOfWork uow = new JdbiUnitOfWork(jdbi);           // adapter，本 task 内简易实现
  TaskOrderRepository taskOrderRepo = taskOrderDaoJdbiImpl; // 已 implements 新接口
  // ... 8 个 repositories 全部从已有 DAO 实例取（Task 7 让它们 implements 了新接口）
  ScheduleDagRunUseCase scheduleUseCase = new ScheduleDagRunService(
      taskRecordRepo, agentDispatcher, agentStatusRepo, taskDispatchRepo, uow, clock, loadBalance);
  // ... 13 个 services
  ```
- [ ] **简易 adapter 临时类**（放 `dag-allinone-muserver` 的 `application/` 或 `adapter/` 子包，step 3 搬走）：
  - `SystemClock implements Clock` — 1 行
  - `SequenceIdGenerator implements IdGenerator` — 通过 jdbi 调 PG 序列；3 个方法
  - `JdbiUnitOfWork implements UnitOfWork` — `<T> T execute(Supplier<T> work) { return jdbi.inTransaction(h -> work.get()); }`
  - `JacksonDagDefinitionParser implements DagDefinitionParser` — 用现有 ObjectMapper
  - `LegacyTokenIssuer implements TokenIssuer` — delegate 旧 `TokenService`

#### Step 9.2: WebServerBootstrap 改造
- [ ] 把 4 个 `new XxxApi(svc)` 改成 `new XxxApi(useCase)`；这里 Api 类位于 dag-scheduler / dag-admin，构造器签名 step 8 已改

#### Step 9.3: InProcessSchedulerClient
- [ ] field、构造器、方法体里所有 `dagScheduleService.onTaskCompleted(...)` 换 `scheduleUseCase.onTaskCompleted(...)`
- [ ] 对应 test mock 类型也改

#### 验证
- [ ] Run: `mvn -f dag-task/pom.xml -pl dag-allinone-muserver -am verify -q` → BUILD SUCCESS
- [ ] Run: `mvn -f dag-task/pom.xml -pl dag-allinone-muserver -am test -q` → GREEN（`AllInOneStartupTest` 是关键整测）

#### Commit message
```
refactor(allinone): wire 13 application services and migrate clients (step 2/Task 9)

- AllInOneAppContext: construct application services from existing
  *DaoJdbiImpl instances (which now satisfy port.out.*Repository)
- AllInOneWebServerBootstrap: pass use cases (not legacy services) to
  TaskTemplateApi / DagManageApi / WorkflowApi / AgentRegistryApi
- InProcessSchedulerClient + tests: switch to ScheduleDagRunUseCase
- Add temporary local adapters: SystemClock, SequenceIdGenerator,
  JdbiUnitOfWork, JacksonDagDefinitionParser, LegacyTokenIssuer
  (to be moved to scheduler-adapter modules in step 3)
```

---

### Task 10: 清理 dag-si pom 依赖

#### 范围
当前 `dag-si/pom.xml` 含 `zora-jdbi` (provided) — 是因为 `dag-si.persistence.*Dao` 接口曾用 `Page/Pageable`。
本步骤先**评估**：dag-si 里还有谁用 zora-jdbi.page？

#### Step 10.1
- [ ] Run: `grep -rE "zora.jdbi.page|zora.jdbi.Jdbi" D:/project/zora-apps/dag-task/dag-si/src/main`
- [ ] 若仍有引用（极可能：search criteria / page 返回类型）：保留依赖；只移除明显未用的依赖（如 jakarta.ws.rs-api 若 dag-si 不再做 controller annotation）
- [ ] 不强求大动；这一 task 主要为 step 3 铺路

#### 验证
- [ ] Run: `mvn -f dag-task/pom.xml -pl dag-si -am compile -q`

#### Commit message
```
chore(si): tighten dag-si dependencies (step 2/Task 10)

Drop unused dependencies; zora-jdbi stays (provided) until step 3
replaces SearchCriteria / Page with domain-owned types.
```

---

### Task 11: 启用 HexagonalArchitectureTest

#### Step 11.1
- [ ] `dag-task/dag-scheduler-domain/src/test/java/top/ilovemyhome/dagtask/scheduler/arch/HexagonalArchitectureTest.java`：
  - 删除类上的 `@Disabled` 注解
  - 在第一条规则的 `resideInAnyPackage(...)` 列表里，**移除** `top.ilovemyhome.zora.jdbi..` 这一行（注释 `// TD-1: zora-jdbi.page.{Page,Pageable} still leaks in UseCase signatures; remove this exemption when step 3 replaces with domain types.`），让 application service 可继续使用 Page/Pageable
  - 其他 zora.* 限制不变

#### Step 11.2
- [ ] Run: `mvn -f dag-task/pom.xml -pl dag-scheduler-domain test -q`
- [ ] Expected: SmokeTest + 3 条 ArchUnit 规则均 GREEN
- [ ] 若 fail：常见原因
  - 应用层 import 了某个被禁包 → 反思设计，要么搬该用法到 adapter，要么扩 exemption（必须写注释说明）
  - domain 包反向 import port → 真正违规，必须修复

#### Commit message
```
test(scheduler-domain): enable hexagonal architecture guard (step 2/Task 11)

Adds permanent exemption for top.ilovemyhome.zora.jdbi.* (Page/Pageable
leak documented as TD-1; will be lifted in step 3).
```

---

### Task 12: 全 repo verify + 启动整测

#### Step 12.1
- [ ] Run:
  ```bash
  mvn -f D:/project/zora-apps/dag-task/pom.xml clean verify \
      -pl '!dag-agent,!dag-agent-muserver,!dag-agent-cli'
  ```
- [ ] Expected: BUILD SUCCESS。非 agent 模块全过。
- [ ] dag-agent 预存在的 jakarta JAX-RS provider + Mockito 配置问题（4 failures + 2 errors）与本次重构无关，已 exclude。

#### Step 12.2: api-test
- [ ] 启动 allinone：`mvn -f D:/project/zora-apps/dag-task/pom.xml -pl dag-allinone-muserver exec:java`（或按现有约定）
- [ ] 跑 `dag-task/api-test/` 下 scheduler / admin 相关 `.http` 测试，全部 200/2xx 通过

#### 不 commit；仅记录验证结果。

---

### Task 12.5: 物理删除 dag-si 旧 service 接口 + @Deprecated ServiceImpl

#### Step 12.5.1: 删 dag-si 内旧 service 接口
- [ ] 删 `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/service/` 整个目录（`DagManageService`、`DagScheduleService`、`DagQueryService`、`TaskOrderService`、`TaskTemplateService`、`TaskDagService`、`AgentRegistryService` 等接口）—— 它们只在 @Deprecated 实现里 implements，没人 import 了（Task 8/9 已切完）
- [ ] 仍需用到的 `dag-si.dto.*`、`enums.*`、`agent.*` 实体不动

#### Step 12.5.2: 删 7 个 @Deprecated `*ServiceImpl`
- [ ] `DagManageServiceImpl`、`DagScheduleServiceImpl`、`DagQueryServiceImpl`、`TaskTemplateServiceImpl`、`TaskOrderServiceImpl`、`TaskDagServiceImpl`、`DefaultAgentRegistryService` 物理删除
- [ ] `dag-scheduler/core/server/DagSchedulerBuilder.java` 中 wiring 这些 service 的代码同步删除（或转为构造 application service）—— 评估：DagSchedulerBuilder 是否还需要？若 dag-allinone-muserver 已直接 wire use case，DagSchedulerBuilder / DagSchedulerServer 可以**整体退役**——但本 task 范围内不删 builder/server 类，留 step 3

#### Step 12.5.3: TaskOrder / TaskTemplate `@JsonDeserialize` 评估
- [ ] **核对**：当前两个实体上的 `@JsonDeserialize` 是否在 ObjectMapper 自定义 deserializer 调用链上必要
- [ ] 若 web adapter 已用 DagDefinitionParser parse 模板 JSON，TaskTemplate 持久化时仍是字符串 dagDefinition，因此 `@JsonDeserialize` **依然需要**保留 ——不要再删（Task 1 v1 已翻车）
- [ ] 仅在确认无用并跑完测试后才考虑删；本步骤倾向**不动**

#### 验证
- [ ] Run: `mvn -f dag-task/pom.xml clean verify -pl '!dag-agent,!dag-agent-muserver,!dag-agent-cli'`

#### Commit message
```
refactor: remove deprecated scheduler service interfaces and implementations (step 2/Task 12.5)

- Delete dag-si/service/* (legacy service interfaces, no callers remain)
- Delete 7 @Deprecated *ServiceImpl in dag-scheduler (replaced by
  scheduler.application.* services in Task 6)
- Keep TaskOrder/TaskTemplate @JsonDeserialize (still required by
  Jackson serialization on persistence layer)
```

---

### Task 13: 更新 CLAUDE.md + docs/10-*

#### Step 13.1: `dag-task/CLAUDE.md`
- [ ] 在六边形重构 section 模块表里把 `dag-scheduler-domain` 阶段从 "步骤 1 ✅ 已建骨架" 改为 "步骤 2 ✅ Domain migrated"
- [ ] 添加段落：步骤 2 完成；剩余步骤 3（搬 adapters）与 4（删旧 dag-scheduler / dag-scheduler-muserver + allinone 直接依赖新模块）尚未开始
- [ ] 删除"在此重构完成前，新增 dag-scheduler 相关代码请加到新模块"的过渡指引——改为"现在所有 dag-scheduler 新增代码应加到 `dag-scheduler-domain.application` 或对应 adapter 模块"

#### Step 13.2: `dag-task/docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`
- [ ] 步骤表把 step 2 从"待办"改为"✅ 已完成"
- [ ] 追加新 section "技术决策记录" 下：
  - 决策 4：JSON 解析责任通过 `DagDefinitionParser` outbound port 反转（避免 domain 依赖 Jackson）
  - 决策 5：Token 签发通过 `TokenIssuer` outbound port 反转
  - 决策 6：Agent in-memory cache 移除（每次查 repository），TD-3 待 step 3 评估是否再加 CachePort
- [ ] 追加 TD-1 ~ TD-4 列表：
  - TD-1：`zora-jdbi.page.{Page,Pageable}` 在 application/port 签名中残留
  - TD-2：`TaskRecordRepository.start/stop` 仍收 `LocalDateTime`，应改 `Instant`
  - TD-3：Agent cache 已移除，若 perf 问题加 CachePort
  - TD-4：`ReportTaskResultService` 直接复用 `taskRecord.getStatus()` 而非根据 success 判断（旧实现 bug 保留）

#### Commit message
```
docs: mark step 2 complete and record new TD items (step 2/Task 13)
```

---

### Task 14: 最终 stage（不 commit）

- [ ] Run: `git -C D:/project/zora-apps status`
- [ ] Run: `git -C D:/project/zora-apps add dag-task/`
- [ ] 报告给用户：所有变更已 stage，等待人工 commit
- [ ] 给用户准备 9 个建议 commit message（Task 5/6/7/8/9/10/11/12.5/13），每个独立 commit 而非合并，便于回滚

---

## 风险与已知 issue

1. **TD-1**: `zora-jdbi.page.*` 残留 → step 3 用 `domain.query.Page<T>` 替代
2. **TD-2**: `TaskRecordRepository.start/stop` 接 `LocalDateTime` 而非 `Instant`
3. **TD-3**: 移除了 Agent in-memory cache（旧 `DefaultAgentRegistryService.agentCache`），若整测发现 perf 退化，step 3 加 CachePort
4. **TD-4**: `ReportTaskResultService` 保留旧 bug（用 `taskRecord.getStatus()`），等 dag-agent 端定义清楚再修
5. **dag-agent 测试预存在问题**：4 failures + 2 errors，与本重构无关，verify 时 exclude
6. **TaskHelper 已删除**（Task 4 commit 226720a），TaskOutput.createErrorOutput 是 sole impl
7. **JSON parsing 责任转移**：通过新增 `DagDefinitionParser` outbound port 完成；adapter 在 dag-allinone-muserver / scheduler-app 提供 `JacksonDagDefinitionParser`
8. **DefaultTaskDispatcher 双重身份**：本 step 仍单一类同时承担"选 agent + HTTP + 持久化 dispatch record"和"实现 AgentDispatcher port"两个职责。step 3 拆：
   - 选 agent → `domain.dispatcher.AgentSelector`（Task 6 已搬）
   - HTTP 投递 → `adapter.dispatcher.HttpAgentDispatcher`（实现 AgentDispatcher port）
   - 持久化 dispatch record → application service 内调 TaskDispatchRepository
9. **15 vs 13 service 数**：合并了 TaskOrder + TaskTemplate 的 Manage/Query
10. **Branch**：`refactor`，仓库根 `D:/project/zora-apps`，dag-task 是子目录
11. **CLAUDE.md 约定**：所有 commit 由人工执行；agent 仅 stage

## 文件位置速查

- Spec: `dag-task/docs/superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md`
- Step 1 plan: `dag-task/docs/superpowers/plans/2026-06-07-dag-scheduler-hexagonal-step1-skeleton.md`
- 本 plan: `dag-task/docs/superpowers/plans/2026-06-07-dag-scheduler-hexagonal-step2-domain-migration.md`
- Session resume: `dag-task/docs/superpowers/SESSION-RESUME.md`
- 架构编号文档: `dag-task/docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`
- ArchUnit 测试: `dag-task/dag-scheduler-domain/src/test/java/top/ilovemyhome/dagtask/scheduler/arch/HexagonalArchitectureTest.java`
- Inbound ports: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/port/in/`
- Outbound ports: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/port/out/`
- Application services（待建）: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/application/`
- Legacy 7 ServiceImpl: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/{service,task,agent}/`
- Legacy 8 DaoJdbiImpl: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dao/`
- DefaultTaskDispatcher: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dispatcher/DefaultTaskDispatcher.java`
- dag-admin/WorkflowApi: `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/WorkflowApi.java`
- dag-allinone 受影响文件：`AllInOneAppContext.java`、`AllInOneWebServerBootstrap.java`、`InProcessSchedulerClient.java`（+ 2 个测试）
