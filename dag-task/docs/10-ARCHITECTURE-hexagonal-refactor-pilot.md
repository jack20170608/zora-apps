# 10 - 架构演进：dag-scheduler 六边形重构试点

- 启动日期：2026-06-07
- 设计文档：[`superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md`](./superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md)
- 试点范围：仅 `dag-scheduler` 子系统
- 状态：已完成（步骤 4 / 4 已完成）

## 背景
项目长期演进可能切换 Web 框架（Spring Web / Micronaut）或持久化技术（本地文件 / KV）。为避免架构被锁死，本次重构在 `dag-scheduler` 试点 Ports & Adapters 架构，验证模式后再推广到 `dag-agent` / `dag-admin`。

## 迁移步骤
| 步骤 | 目标 | 状态 |
|---|---|---|
| 1 | 建 4 个新模块骨架 + ArchUnit 守护（不动旧代码） | ✅ 已完成 |
| 2 | 搬 domain：领域类、UseCase、ports 从旧 core 迁入 `dag-scheduler-domain` | ✅ 已完成 |
| 3 | 搬 adapters：dao 迁入 persistence-jdbc；Flyway 迁移迁入 persistence-jdbc；TD-1/TD-2 解决 | ✅ 已完成 |
| 4 | 切 allinone，删旧模块，归档总结 | ✅ 已完成 |

## 新模块结构
```
dag-scheduler-domain/                       纯领域 + ports（零基础设施依赖）
├── src/main/java/top/ilovemyhome/dagtask/scheduler/
│   ├── domain/                             (entities, value objects, domain services)
│   ├── application/                        (use case implementations)
│   ├── port/in/                            (inbound ports：UseCase 接口、Command/Result records)
│   └── port/out/                           (outbound ports：Repository / UnitOfWork / Clock / IdGenerator / AgentDispatcher)
└── src/test/java/.../arch/HexagonalArchitectureTest.java   (ArchUnit 守护，3 条规则全绿)

dag-scheduler-adapter-persistence-jdbc/     zora-jdbi/Flyway 实现 port.out
dag-scheduler-adapter-web-muserver/         zora-muserver 实现 port.in
dag-scheduler-app/                          组装入口（手工 DI + SchedulerContext + token 管理）
```

## 技术决策记录

### 决策 1：adapter 模块沿用 zora 包装而非 raw 库
- **背景**：原计划在 adapter 模块直接依赖 `jdbi3-core` / `HikariCP` / `mu-server` / `Jackson`。
- **决定**：改为沿用项目现有约定，adapter 模块依赖 `zora-jdbi` / `zora-rdb` / `zora-muserver` / `zora-common` / `zora-json` / `zora-config`。
- **理由**：与项目现有约定一致，迁移代码（步骤 3）零摆平；不需要重写 zora 提供的连接池/事务/默认 mapper/JSON 序列化能力。
- **影响**：ArchUnit 守护规则相应扩展，禁止 `dag-scheduler-domain` 依赖 `top.ilovemyhome.zora.jdbi..` / `..rdb..` / `..muserver..` 等基础设施包装；只允许 adapter 模块使用。

### 决策 2：手工 DI（不引入容器）
- 见设计文档 §4.1。`scheduler-app` 用手工构造函数注入，预留 Avaje Inject 升级路径。

### 决策 3：根 .gitignore 锚定 out/ 规则
- **背景**：根 `.gitignore` 第 40 行原为 `out/`（无锚定），会递归忽略任何名为 `out` 的目录——包括我们的 `port/out/` 包目录。
- **决定**：改为 `/out/`（只匹配根目录下的 IDEA 编译输出目录）。
- **理由**：避免步骤 2 写入 `port.out/DagRunRepository.java` 等文件时被静默忽略（这种 bug 极难发现）。

### 决策 4：JSON 解析责任通过 `DagDefinitionParser` outbound port 反转
- **背景**：旧 `DagManageServiceImpl.instantiateFromTemplate` 内部直接调用 `ObjectMapper.readTree`，导致 domain 层依赖 Jackson。
- **决定**：新增 outbound port `DagDefinitionParser`，将 JSON 字符串 → typed `DagDefinition` 的解析反转到 adapter。
- **理由**：保持 `dag-scheduler-domain` 零基础设施依赖，满足 ArchUnit 规则。
- **后续**：step 3 将 `JacksonDagDefinitionParser` 从 `dag-scheduler/core/adapter` 迁入 `dag-scheduler-adapter-persistence-jdbc` 或 `dag-scheduler-app`。

### 决策 5：Token 签发通过 `TokenIssuer` outbound port 反转
- **背景**：旧 `DefaultAgentRegistryService.registerAgent` 内部调用 `TokenService.generateToken`，引入 zora 基础设施依赖。
- **决定**：新增 outbound port `TokenIssuer`，将 token 签发反转到 adapter。
- **理由**：同决策 4，保持 domain 零基础设施。

### 决策 6：移除 Agent in-memory cache（每次查 repository）
- **背景**：旧 `DefaultAgentRegistryService` 维护 `agentCache`（`Map<String, AgentStatus>`），启动时从 DB warm。
- **决定**：新 `RegisterAgentService` 每次直接查 `AgentStatusRepository`，不维护内存缓存。
- **理由**：cache 是基础设施关注点，不应留在 application service；当前 agent 数量极少，性能影响可忽略。
- **风险**：TD-3 — 若整测发现 perf 退化，step 3 评估是否新增 `CachePort`。

## 验收标准
（详见设计文档 §8）

### 步骤 1 验收结果
- ✅ 4 个新模块各自能编译，各跑 1 个 SmokeTest 通过
- ✅ `HexagonalArchitectureTest`（3 个规则）已写就，当前 `@Disabled` skip
- ✅ 旧 `dag-scheduler` / `dag-scheduler-muserver` / `dag-admin` / `dag-admin-muserver` 不受影响，全部 BUILD SUCCESS
- ⚠️ `dag-agent` 测试预存在的 jakarta JAX-RS provider + Mockito 配置问题（4 failures + 2 errors），**与本步骤无关**，待后续单独修复

### 步骤 2 验收结果
- ✅ 13 个 application service 类（实现 15 个 inbound use case）位于 `dag-scheduler-domain/scheduler/application`，零基础设施依赖
- ✅ 8 个 `*DaoJdbiImpl` 同时实现新旧两套 Repository 接口，adapter 无需额外 wrapper
- ✅ `DefaultTaskDispatcher` 额外实现 `AgentDispatcher` outbound port
- ✅ `dag-admin/WorkflowApi` 改用 `QueryTaskTemplateUseCase` + `ManageTaskTemplateUseCase` + `InstantiateDagTemplateUseCase`
- ✅ `dag-allinone-muserver` 的 `AllInOneAppContext` / `InProcessSchedulerClient` 改用 `ScheduleDagRunUseCase`
- ✅ `HexagonalArchitectureTest` 3 条规则全绿（ArchUnit `1.4.2` 为支持 JDK 25 的最低版本）
- ✅ `mvn verify -pl '!dag-agent,!dag-agent-muserver,!dag-agent-cli'` BUILD SUCCESS，全部测试通过
- ⚠️ 旧 `dag-si/service/*` 接口和 7 个 `@Deprecated` ServiceImpl **未物理删除**（DagSchedulerServer 仍引用），留 step 4 整体退役

### 步骤 3 验收结果
- ✅ `domain.query.Page<T>` + `domain.query.Pageable` 创建，替换 zora-jdbi 类型（TD-1 解决）
- ✅ `TaskRecordRepository.start/stop` 改为 `Instant`，DaoJdbiImpl 内部转换（TD-2 解决）
- ✅ `HexagonalArchitectureTest` 移除所有 zora 包豁免，3 条规则全绿
- ✅ 8 个 `*DaoJdbiImpl` 物理迁入 `dag-scheduler-adapter-persistence-jdbc`
- ✅ 4 个 cross-cutting adapter（SystemClock, JdbiUnitOfWork, SequenceIdGenerator, JacksonDagDefinitionParser）迁入 persistence-jdbc
- ✅ Flyway 迁移文件从 `dag-si` 迁入 `dag-scheduler-adapter-persistence-jdbc`
- ✅ `dag-scheduler` pom 添加 persistence-jdbc 依赖，DagSchedulerBuilder 引用新包
- ✅ `dag-admin-muserver` / `dag-allinone-muserver` pom 添加 persistence-jdbc 依赖，AppContext 引用新包
- ✅ `WorkflowApi` 改用 domain `Page` / `Pageable`
- ✅ `mvn verify -pl '!dag-agent,!dag-agent-muserver,!dag-agent-cli' -am` BUILD SUCCESS
- ⚠️ `LegacyTokenIssuer` 未迁入（依赖 `TokenService` 仍在 dag-scheduler），留 step 4
- ⚠️ Web API 类（`TaskTemplateApi` 等）仍留在 `dag-scheduler/core/interfaces`，新 web adapter 留 step 4

### 步骤 4 验收结果
- ✅ `dag-scheduler-adapter-web-muserver` 提供 `TaskOrderApi`、`TaskTemplateApi`、`DagManageApi`、`AgentRegistryApi`，调用 inbound use case / repository port
- ✅ `dag-scheduler-app` 提供 `SchedulerContext`、`HttpAgentDispatcher`、`LegacyTokenIssuer`、`TokenService`、`TokenManagementApi`
- ✅ `dag-admin-muserver` 改用 `SchedulerContext` 与新 web adapter，不再依赖旧 `dag-scheduler`
- ✅ `dag-allinone` / `dag-allinone-muserver` 改用新模块，不再依赖旧 `dag-scheduler` / `dag-scheduler-muserver`
- ✅ 旧 `dag-scheduler` / `dag-scheduler-muserver` 模块已从 reactor 与 dependencyManagement 删除，并物理删除目录
- ✅ 最终 Java / pom 引用扫描无 `DagSchedulerServer`、`DagSchedulerBuilder`、`top.ilovemyhome.dagtask.core.*`、旧 scheduler artifact 命中
- ✅ `mvn -f dag-task/pom.xml clean verify -pl '!dag-agent,!dag-agent-muserver,!dag-agent-cli' -q` BUILD SUCCESS
- ⚠️ `dag-agent` 测试仍有预存 JAX-RS RuntimeDelegate / Mockito 问题，且 `dag-agent/CLAUDE.md` 明确不要增加 muserver 依赖；本次未修改该问题

## 已知技术债务（TD）

| ID | 描述 | 计划修复步骤 |
|---|---|---|
| TD-1 | ✅ `zora-jdbi.page.{Page,Pageable}` 已替换为 `domain.query.Page<T>` / `Pageable` | 已完成 |
| TD-2 | ✅ `TaskRecordRepository.start/stop` 已改为 `Instant`，DaoJdbiImpl 内部转换 | 已完成 |
| TD-3 | Agent in-memory cache 已移除（每次查 repository）；step 4 allinone 整测未发现性能/功能阻塞，若后续压测发现退化再加 `CachePort` | 后续按需 |
| TD-4 | `ReportTaskResultService` 直接复用 `taskRecord.getStatus()` 而非根据 success 判断（旧实现 bug 保留） | 待 dag-agent 端定义清楚后修复 |

## 总结

本次试点完成了 `dag-scheduler` 从旧 core/server 结构到 Ports & Adapters 的完整迁移：domain/application/ports、JDBC persistence adapter、MuServer web adapter 与手工 DI app context 均已落位，旧 `dag-scheduler` / `dag-scheduler-muserver` 模块已退役删除。

主要踩坑与经验：
- 根 `.gitignore` 的 `out/` 规则必须锚定为 `/out/`，否则会误忽略 `port/out/` 包。
- token 生成依赖 JJWT，不能放入 domain；最终放在 `dag-scheduler-app`，domain 仅保留 `TokenIssuer` port。
- JAX-RS `Response` 单测需要 test-scope RuntimeDelegate provider（本次使用 Jersey common），但 `dag-agent` 子项目明确不要增加 muserver 依赖，相关预存测试问题应单独处理。
- 删除旧模块前必须做 Java 与 pom 引用扫描，先切入口再删目录，避免 reactor 断裂。

推广建议：后续迁移其他子系统时，继续采用“先 domain/ports，再 adapter，再入口 cutover，最后删旧模块”的节奏；每一步都保留可运行验证命令与引用扫描清单。
