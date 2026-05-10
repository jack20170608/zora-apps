# Feature: Scheduler / Admin Module Split

## 背景与目标

当前 `dag-scheduler-muserver` 同时承载了三类职责：
1. **Agent 通信 API**（注册、心跳、结果上报）
2. **Admin 管理 API**（Token、白名单、工作流、任务、Agent 管理、统计）
3. **Web 基础设施**（登录页、Cookie JWT 认证、Swagger、静态资源）

这三类流量的安全模型、QPS 特征、稳定性要求完全不同，且当前 Agent Token 认证与 Admin Cookie 认证体系互相不通，导致认证链路断裂。

本功能旨在将 Admin 管理职责从 `dag-scheduler-muserver` 中拆分出来，形成独立的 `dag-admin` 和 `dag-admin-muserver` 模块，使 Agent 通信端口与 Admin 管理端口彻底分离。

## 模块划分

```
dag-task/
├── dag-si/                    (已有 — 领域模型 + DAO + DTO)
├── dag-scheduler/             (已有 — 调度引擎核心 + Agent API + 服务实现)
├── dag-scheduler-muserver/    (改造 — 仅保留 Agent 通信端口)
├── dag-admin/                 (新建 — Admin 管理 API)
├── dag-admin-muserver/        (新建 — Admin MuServer 包装)
└── dag-admin-web/             (重命名 — 原 dag-scheduler-web)
```

## 代码归属

### dag-scheduler（保留不变）

| 类 | 说明 |
|---|---|
| `core/interfaces/AgentRegistryApi` | Agent 注册、心跳、结果上报 |
| `core/interfaces/DagManageApi` | DAG 实例化 |
| `core/interfaces/TaskOrderApi` | Task Order CRUD |
| `core/interfaces/TaskTemplateApi` | Template CRUD |
| `scheduler/token/TokenService` | Token 生成/验证 |
| `scheduler/token/TokenManagementApi` | Token 管理 API |
| `core/agent/*` | Agent 注册服务实现 |
| `core/dispatcher/*` | 任务调度器 |
| `core/dao/*` | DAO 实现 |

### dag-scheduler-muserver（改造后）

| 类 | 说明 |
|---|---|
| `server/App` | 主入口 |
| `server/application/AppContext` | 初始化 `DagSchedulerServer` |
| `server/application/WebServerBootstrap` | 仅注册 `AgentRegistryApi`，独立端口 |
| `server/web/security/SecurityHandler` | 改为 Bearer Token 认证 |

**移除**：Admin API、Swagger UI、登录页、Cookie 认证。

### dag-admin（新建）

从 `dag-scheduler-muserver` 迁移：

| 来源 | 目标 | 说明 |
|---|---|---|
| `server/interfaces/api/WorkflowApi` | `dag-admin/interfaces/api/` | 工作流管理 |
| `server/interfaces/api/ExecutionApi` | `dag-admin/interfaces/api/` | 执行实例管理 |
| `server/interfaces/api/StatsApi` | `dag-admin/interfaces/api/` | 统计面板 |
| `server/interfaces/api/AgentAdminApi` | `dag-admin/interfaces/api/` | Agent 管理 |
| `server/interfaces/api/AgentWhitelistAdminApi` | `dag-admin/interfaces/api/` | 白名单管理 |

### dag-admin-muserver（新建）

| 类 | 说明 |
|---|---|
| `server/App` | 主入口，独立 JVM 进程 |
| `server/application/AppContext` | 初始化 DB + `DagSchedulerServer`（不启调度循环） |
| `server/application/WebServerBootstrap` | 注册所有 Admin API + Swagger + 登录页 |
| `server/web/security/SecurityHandler` | Cookie JWT 认证 |
| `server/web/LoginHandler` | 登录页 |
| `server/web/security/JwtHelper` | 从 `dag-scheduler-muserver` 迁移 |

### dag-admin-web（重命名自 dag-scheduler-web）

仅修改 `pom.xml` 中的 `artifactId` 和 `name`。

## 模块依赖关系

```
dag-si
  ^
dag-scheduler ──> (依赖 dag-si)
  ^                ^
  |              dag-admin ──> (依赖 dag-si)
  |                ^
  +--------------+
        ^
dag-admin-muserver ──> (依赖 dag-scheduler + dag-admin + dag-si)
        ^
dag-admin-web ──> (Maven 聚合，无代码依赖)
```

`dag-admin-muserver` POM 依赖：
- `dag-si`
- `dag-scheduler`
- `dag-admin`

`dag-scheduler-muserver` POM 依赖：
- `dag-si`
- `dag-scheduler`

## dag-task/pom.xml 变更

```xml
<modules>
    <module>dag-si</module>
    <module>dag-scheduler</module>
    <module>dag-scheduler-muserver</module>
    <module>dag-admin</module>           <!-- 新增 -->
    <module>dag-admin-muserver</module>  <!-- 新增 -->
    <module>dag-agent</module>
    <module>dag-agent-muserver</module>
    <module>dag-admin-web</module>       <!-- 重命名 -->
</modules>
```

`dependencyManagement` 中新增 `dag-admin` 和 `dag-admin-muserver` 条目。

## 关键设计决策

### dag-admin-muserver 是否也启动 DagSchedulerServer？

**是**，但仅用于获取服务 bean（`DagManageService`、`TaskTemplateService` 等），**不启动调度循环**。`DagSchedulerServer.start()` 中只启动线程池，真正的调度扫描在 `TaskDispatcher` 中，当前已耦合在 `DagSchedulerBuilder` 中。后续如需要，可添加 `adminMode` 开关来跳过调度器启动。

### 数据库是否共享？

**是**。`dag-scheduler-muserver` 和 `dag-admin-muserver` 连接到同一个数据库，通过事务隔离保证一致性。

### 端口分配

| 模块 | 默认端口 | 用途 |
|---|---|---|
| dag-scheduler-muserver | 8001 | Agent 通信，内网 |
| dag-admin-muserver | 8000 | Admin 管理，可公网+网关 |

## 代码迁移工作量

| 操作 | 数量 | 说明 |
|------|------|------|
| 新建模块 | 2 | dag-admin, dag-admin-muserver |
| 新建 POM | 2 | 使用 parent pom 模板 |
| 迁移 Java 类 | 5 | WorkflowApi, ExecutionApi, StatsApi, AgentAdminApi, AgentWhitelistAdminApi |
| 新建 Java 类 | 3 | App, AppContext, WebServerBootstrap (dag-admin-muserver) |
| 修改 Java 类 | 2 | dag-scheduler-muserver 的 WebServerBootstrap, AppContext |
| 修改 POM | 2 | dag-task/pom.xml, dag-scheduler-web/pom.xml |
| 新建配置 | 1 | dag-admin-muserver 的 application.conf |

总计约 **15 个文件**的新建或修改。
