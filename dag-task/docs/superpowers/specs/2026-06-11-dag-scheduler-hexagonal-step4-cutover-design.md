# dag-scheduler 六边形重构 Step 4：切换入口并退役旧模块设计

**日期**：2026-06-11  
**范围**：`dag-task` / `dag-scheduler` 子系统  
**前置状态**：Step 2、Step 3 已完成；`dag-scheduler-domain`、`dag-scheduler-adapter-persistence-jdbc`、`dag-scheduler-adapter-web-muserver`、`dag-scheduler-app` 已存在。

## 1. 背景

当前六边形重构已经完成 domain 与 persistence adapter 迁移，但运行入口仍部分依赖旧模块：

- `dag-admin-muserver` 仍通过 `DagSchedulerBuilder` / `DagSchedulerServer` 获取旧 service 与 API。
- `dag-allinone` / `dag-allinone-muserver` 仍保留对 `dag-scheduler`、`dag-scheduler-muserver` 的依赖或旧 server 引用。
- 旧 `dag-scheduler` 中仍保留 legacy service、builder、server、部分 web API 与 `LegacyTokenIssuer`。
- 旧 `dag-scheduler-muserver` 仍在聚合模块中。

Step 4 的目标是完成最终切换：运行入口不再依赖旧 scheduler 模块，旧模块可以安全删除，架构文档标记重构完成。

## 2. 目标

1. `dag-admin-muserver` 和 `dag-allinone-muserver` 不再依赖 `dag-scheduler` / `dag-scheduler-muserver`。
2. Web API adapter 由 `dag-scheduler-adapter-web-muserver` 提供，调用 `dag-scheduler-domain` 的 inbound use case。
3. Scheduler 组装逻辑由 `dag-scheduler-app` 或入口模块本地 AppContext 负责，不再通过旧 `DagSchedulerBuilder` / `DagSchedulerServer`。
4. `dag-allinone` pom 不再聚合/传递旧 scheduler 模块。
5. 删除旧 `dag-scheduler`、`dag-scheduler-muserver` 模块及聚合 pom / dependencyManagement 中的引用。
6. 更新文档，记录最终模块结构、验证结果、剩余技术债。

## 3. 非目标

- 不修复 `dag-agent` 现有测试问题；verify 仍按既有约定排除 `dag-agent`、`dag-agent-muserver`、`dag-agent-cli`。
- 不引入 DI 容器，继续手工构造依赖。
- 不改 HTTP API 路径和响应协议，避免破坏现有 `api-test/`。
- 不新增第二种持久化 adapter。
- 不重构 `dag-admin` 自身业务结构，只替换其 scheduler 依赖来源。

## 4. 推荐架构

### 4.1 模块边界

完成后 scheduler 相关模块保留为：

```text
dag-scheduler-domain                       # domain + ports + application services
dag-scheduler-adapter-persistence-jdbc     # Jdbi/Flyway/outbound adapters
dag-scheduler-adapter-web-muserver         # MuServer inbound HTTP adapters
dag-scheduler-app                          # scheduler 手工 DI / context / optional standalone app
```

旧模块删除：

```text
dag-scheduler
dag-scheduler-muserver
```

### 4.2 运行入口依赖方向

`dag-admin-muserver` 和 `dag-allinone-muserver` 只允许依赖：

- `dag-si`（共享 DTO / entity / enum，直到后续单独治理）
- `dag-scheduler-domain`
- `dag-scheduler-adapter-persistence-jdbc`
- `dag-scheduler-adapter-web-muserver`
- `dag-scheduler-app`（如果采用集中组装方式）
- `dag-admin` / `dag-agent-muserver` 等本入口本来需要的模块

禁止依赖：

- `dag-scheduler`
- `dag-scheduler-muserver`
- `top.ilovemyhome.dagtask.core.*`
- `top.ilovemyhome.dagtask.scheduler.muserver.*` legacy module 包

## 5. 迁移设计

### 5.1 Web adapter 迁移

将旧 `dag-scheduler/core/interfaces` 中仍被运行入口使用的 HTTP API 能力迁移到 `dag-scheduler-adapter-web-muserver`，构造器直接接收 inbound use case，而不是 legacy service。

最小迁移对象包括：

- `TaskTemplateApi`：调用 `QueryTaskTemplateUseCase` / `ManageTaskTemplateUseCase`
- `DagManageApi`：调用 `InstantiateDagTemplateUseCase` / `SubmitDagRunUseCase`（按现有旧 API 实际方法决定）
- `AgentRegistryApi`：调用 `RegisterAgentUseCase` / `AgentHeartbeatUseCase` / `ReportTaskResultUseCase`

如果 `dag-admin` 中已有 `WorkflowApi` 可覆盖部分能力，则优先复用，避免重复实现。

### 5.2 App/context 组装

优先在 `dag-scheduler-app` 提供一个清晰的 `SchedulerContext` 或等价组装类，负责：

1. 接收 `Jdbi`、`ObjectMapper`、JWT/Token 配置、调度配置。
2. 创建 persistence adapter repositories。
3. 创建 cross-cutting adapters：`SystemClock`、`JdbiUnitOfWork`、`SequenceIdGenerator`、`JacksonDagDefinitionParser`、`LegacyTokenIssuer` 替代物。
4. 创建 13 个 application services。
5. 暴露 Web adapter 所需的 use case getters。

`dag-admin-muserver` / `dag-allinone-muserver` 应使用该 context，而不是各自重新维护一份大规模 wiring。若现有 allinone 组装习惯要求入口本地 context，也应把重复逻辑压到 `dag-scheduler-app` 的工厂中。

### 5.3 TokenIssuer 退役旧依赖

`LegacyTokenIssuer` 当前仍在旧 `dag-scheduler`。Step 4 必须将其迁出旧模块，放到合适 adapter：

- 若仅依赖 token service / jwt config 且与 persistence 无关，放入 `dag-scheduler-app` 更合适。
- 若依赖 Jdbi token 表或已有 persistence adapter 能力，放入 `dag-scheduler-adapter-persistence-jdbc`。

选择以实际代码依赖为准，原则是不让任何新模块反向依赖旧 `dag-scheduler`。

### 5.4 删除旧模块

删除前必须全仓确认没有引用：

- `dag-scheduler` artifact
- `dag-scheduler-muserver` artifact
- `DagSchedulerServer`
- `DagSchedulerBuilder`
- `top.ilovemyhome.dagtask.core.`
- `top.ilovemyhome.dagtask.si.service.` legacy service 接口

只有确认无运行入口和测试引用后，才删除旧模块目录和 pom 引用。

## 6. 错误处理与安全边界

1. **默认拒绝旧依赖回流**：若新模块需要旧模块的类，应迁移该类或重写 adapter，而不是添加旧模块依赖。
2. **删除前验证引用为空**：避免误删仍被入口使用的代码。
3. **HTTP 兼容优先**：API 路径、请求字段、响应结构保持不变；必要时复制旧 API 行为。
4. **事务边界不扩大**：application service 仍通过 `UnitOfWork` 控制事务，web adapter 不直接开事务。
5. **配置缺失默认失败**：JWT/token/DB 配置缺失时应启动失败并记录明确错误，不 silently fallback 到不安全默认值。
6. **不绕过 ArchUnit**：若 domain 因 Step 4 新代码引入基础设施依赖，视为设计错误，应迁移到 adapter/app。

## 7. 验证策略

### 7.1 编译与单测

逐步验证：

```bash
mvn -f dag-task/pom.xml -pl dag-scheduler-adapter-web-muserver -am test -q
mvn -f dag-task/pom.xml -pl dag-scheduler-app -am test -q
mvn -f dag-task/pom.xml -pl dag-admin-muserver -am test -q
mvn -f dag-task/pom.xml -pl dag-allinone-muserver -am test -q
```

最终验证：

```bash
mvn -f dag-task/pom.xml clean verify -pl '!dag-agent,!dag-agent-muserver,!dag-agent-cli'
```

### 7.2 引用扫描

最终必须确认：

```bash
rg "DagSchedulerServer|DagSchedulerBuilder|top\.ilovemyhome\.dagtask\.core\.|top\.ilovemyhome\.dagtask\.si\.service\." dag-task
rg "<artifactId>dag-scheduler</artifactId>|<artifactId>dag-scheduler-muserver</artifactId>" dag-task --glob 'pom.xml'
```

允许命中历史文档；不允许命中生产代码、测试代码或有效 pom 依赖。

### 7.3 HTTP/API 验证

如果本地可启动 allinone：

1. 启动 `dag-allinone-muserver`。
2. 运行 `dag-task/api-test/` 下 scheduler/admin 相关 `.http` 测试。
3. 确认原有 2xx 场景仍通过，错误场景响应语义不变。

## 8. 完成定义

1. 旧 `dag-scheduler` / `dag-scheduler-muserver` 模块目录已删除。
2. 聚合 `dag-task/pom.xml` 不再声明旧模块与旧 dependencyManagement。
3. `dag-admin-muserver`、`dag-allinone`、`dag-allinone-muserver` 不再依赖旧 scheduler artifact。
4. 全仓生产/测试代码不再引用 legacy `DagSchedulerServer` / `DagSchedulerBuilder` / `top.ilovemyhome.dagtask.core.*`。
5. `dag-scheduler-domain` ArchUnit 测试仍全绿。
6. 非 agent 模块 `clean verify` 通过。
7. `docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md` 标记 step4 完成并回填总结。
8. 按项目约定：代码不由 Claude commit；最终只报告变更与建议 commit message。

## 9. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 旧 Web API 行为复制不完整 | 迁移前读取旧 API 实现，逐方法对齐路径、参数、响应；保留兼容测试 |
| app/context wiring 过大难以一次写对 | 先建立集中 `SchedulerContext`，再逐入口替换；每步运行模块级测试 |
| 删除旧模块后发现隐性依赖 | 删除前后都做引用扫描；先从 pom 依赖移除并编译，再物理删除 |
| TokenIssuer 迁移位置选错 | 以依赖方向判断；不得让 adapter/domain 依赖 app 或旧 core |
| allinone 启动整测依赖本地环境 | 若环境不可用，明确记录未执行原因；至少完成编译、单测、引用扫描 |

## 10. 交付物

- Step 4 implementation plan：`docs/superpowers/plans/2026-06-11-dag-scheduler-hexagonal-step4-cutover.md`
- 更新后的架构文档：`docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`
- 新/迁移后的 web adapter 与 scheduler app wiring 代码
- 删除旧模块后的 pom 清理
- 验证结果记录
