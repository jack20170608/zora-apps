# dag-task-allinone 设计文档

> 将 dag-scheduler（调度中心）、dag-admin（管理后台）、dag-agent（任务执行器）合并为单一 JVM 进程的 AllInOne 部署模式。

---

## 1. 背景与目标

### 1.1 背景

dag-task 当前采用分布式三模块架构：

- **dag-scheduler-muserver**（端口 8001）：Agent 通信、任务调度
- **dag-admin-muserver**（端口 8000）：管理后台、Swagger、登录页
- **dag-agent-muserver**（端口 8080）：任务执行器

该架构适合生产环境分布式部署，但在以下场景下过于复杂：

- 单机快速验证（POC、开发调试）
- 轻量级内部使用（<1000 任务/天）
- 集成测试（需要启动三个进程）

### 1.2 目标

- 提供**单一可执行 JAR**，一个命令启动完整系统
- 统一到一个端口（8080），降低配置复杂度
- 内部组件间**直接方法调用**，消除 HTTP 开销
- 复用现有模块代码，**零侵入**修改

### 1.3 非目标

- 不替代现有分布式部署模式（allinone 是补充方案）
- 不修改 dag-scheduler、dag-admin、dag-agent 的源码
- 不引入新的存储后端或数据模型

---

## 2. 架构设计

### 2.1 模块结构

```
dag-task/
├── dag-si/                          (已有 — 领域模型 + DAO + DTO)
├── dag-scheduler/                   (已有 — 调度引擎核心)
├── dag-admin/                       (已有 — Admin 管理 API)
├── dag-agent/                       (已有 — Agent 核心)
├── dag-scheduler-muserver/          (已有 — Agent 通信端口)
├── dag-admin-muserver/              (已有 — Admin 管理端口)
├── dag-agent-muserver/              (已有 — Agent HTTP 服务)
├── dag-agent-cli/                   (已有 — Agent CLI)
├── dag-allinone/                    【新建】纯依赖聚合
└── dag-allinone-muserver/           【新建】统一启动入口
```

### 2.2 模块职责

| 模块 | 职责 | 依赖 |
|------|------|------|
| `dag-allinone` | Maven 聚合模块，声明对 dag-scheduler、dag-admin、dag-agent、dag-si 的依赖，统一版本管理 | dag-scheduler, dag-admin, dag-agent, dag-si |
| `dag-allinone-muserver` | 唯一可执行 JAR：初始化共享资源、启动调度器、启动管理后台、启动内嵌 Agent、启动统一 HTTP 服务 | dag-allinone |

### 2.3 进程内组件图

```
┌─────────────────────────────────────────────────────────────┐
│                    dag-allinone-muserver (JVM)               │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              AllInOneApp (main entry)                  │ │
│  └────────────────────┬───────────────────────────────────┘ │
│                       │                                      │
│         ┌─────────────┼─────────────┐                       │
│         ▼             ▼             ▼                       │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐              │
│  │ DagScheduler│ │  DagAdmin  │ │  Embedded  │              │
│  │   Server    │ │   Server   │ │   Agent    │              │
│  └──────┬──────┘ └──────┬─────┘ └──────┬─────┘              │
│         │               │              │                     │
│         └───────┬───────┘              │                     │
│                 │                      │                     │
│                 ▼                      ▼                     │
│  ┌──────────────────────────────────────────────┐           │
│  │         AllInOneAppContext                   │           │
│  │  ┌─────────────┐ ┌──────────────────┐       │           │
│  │  │ DataSource  │ │      Jdbi        │       │           │
│  │  │  (shared)   │ │    (shared)      │       │           │
│  │  └─────────────┘ └──────────────────┘       │           │
│  └──────────────────────────────────────────────┘           │
│                        │                                     │
│         ┌──────────────┼──────────────┐                     │
│         ▼              ▼              ▼                     │
│  ┌──────────┐ ┌──────────────┐ ┌──────────┐                │
│  │InProcess │ │ InProcess    │ │ TaskExec │                │
│  │TaskDispat│ │ SchedulerCli │ │ Manager  │                │
│  │ cher     │ │ ent          │ │          │                │
│  └──────────┘ └──────────────┘ └──────────┘                │
│         │              │              │                     │
│         └──────────────┼──────────────┘                     │
│                        │                                     │
│                        ▼                                     │
│  ┌──────────────────────────────────────────────┐           │
│  │         MuServer (port 8080)                 │           │
│  │                                              │           │
│  │  /api/schedule/*   Scheduler API (Cookie JWT)   │           │
│  │  /api/admin/*      Admin API (Cookie JWT)   │           │
│  │  /api/agent/*      Agent API (Cookie JWT)   │           │
│  │  /login            Login Page (no auth)     │           │
│  │  /swagger/*        Swagger UI (no auth)     │           │
│  │  /static/*         Static Assets (no auth)  │           │
│  └──────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 端口与路由设计

### 3.1 统一端口

所有服务共享 **8080** 端口，通过路径前缀区分。

### 3.2 路径映射表

| 原端口 | 原路径 | 统一路径 | 来源模块 | 认证 |
|--------|--------|---------|---------|------|
| 8001 | `/api/agent/register` | `/api/schedule/agent/register` | dag-scheduler | Cookie JWT |
| 8001 | `/api/agent/heartbeat` | `/api/schedule/agent/heartbeat` | dag-scheduler | Cookie JWT |
| 8001 | `/api/agent/result` | `/api/schedule/agent/result` | dag-scheduler | Cookie JWT |
| 8001 | `/api/dag/*` | `/api/schedule/dag/*` | dag-scheduler | Cookie JWT |
| 8001 | `/api/template/*` | `/api/schedule/template/*` | dag-scheduler | Cookie JWT |
| 8001 | `/api/order/*` | `/api/schedule/order/*` | dag-scheduler | Cookie JWT |
| 8000 | `/api/workflow/*` | `/api/admin/workflow/*` | dag-admin | Cookie JWT |
| 8000 | `/api/execution/*` | `/api/admin/execution/*` | dag-admin | Cookie JWT |
| 8000 | `/api/stats/*` | `/api/admin/stats/*` | dag-admin | Cookie JWT |
| 8000 | `/api/agent-admin/*` | `/api/admin/agent/*` | dag-admin | Cookie JWT |
| 8000 | `/login` | `/login` | dag-admin | 免认证 |
| 8000 | `/swagger/*` | `/swagger/*` | dag-admin | 免认证 |
| 8000 | `/static/*` | `/static/*` | dag-admin | 免认证 |
| 8080 | `/api/submit` | `/api/agent/submit` | dag-agent-muserver | Cookie JWT |
| 8080 | `/api/kill/{id}` | `/api/agent/kill/{id}` | dag-agent-muserver | Cookie JWT |
| 8080 | `/api/force-ok/{id}` | `/api/agent/force-ok/{id}` | dag-agent-muserver | Cookie JWT |
| 8080 | `/api/health` | `/api/agent/health` | dag-agent-muserver | **免认证** |
| 8080 | `/api/ping` | `/api/agent/ping` | dag-agent-muserver | **免认证** |

### 3.3 认证策略

AllInOne 模式下**统一使用 Cookie JWT 认证**：

- 白名单路径免认证：`/login`、`/api/agent/health`、`/api/agent/ping`、`/swagger/*`、`/static/*`
- 其余所有端点走 Cookie JWT 认证
- 原 Agent 通信的 Bearer Token 认证在 allinone 模式下不再使用（不存在外部 Agent）

---

## 4. 核心组件设计

> **注**：以下 Java 代码片段中的 API（如 `DagSchedulerBuilder`、`DagAdminServer`、`TaskExecutionManager` 的构造方式等）为**概念性伪代码**，展示设计意图。实际实现时将根据现有模块的真实 API 签名进行调整，保持设计目标不变。

### 4.1 AllInOneApp

```java
public class AllInOneApp {

    private final AllInOneAppContext appContext;
    private final MuServer muServer;

    public static void main(String[] args) {
        String env = System.getProperty("env", "dev");
        AllInOneApp app = new AllInOneApp(env);
        app.start();
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    public AllInOneApp(String env) {
        Config config = loadConfig(env);
        this.appContext = new AllInOneAppContext(config);
        this.muServer = new AllInOneWebServerBootstrap(config, appContext).build();
    }

    public void start() {
        appContext.start();  // 启动 DB + Scheduler + Admin + Agent
        muServer.start();
    }

    public void stop() {
        muServer.stop();
        appContext.stop();   // 优雅关闭
    }
}
```

### 4.2 AllInOneAppContext

```java
public class AllInOneAppContext {

    private final Config config;
    private final Jdbi jdbi;
    private final DagSchedulerServer dagSchedulerServer;
    private final DagAdminServer dagAdminServer;  // 管理 API 的 Service 容器
    private final TaskExecutionManager taskExecutionManager;
    private final EmbeddedAgentBootstrap agentBootstrap;

    public AllInOneAppContext(Config config) {
        this.config = config;
        this.jdbi = initJdbi(config);

        // 1. 启动调度器（注入 InProcessTaskDispatcher）
        this.dagSchedulerServer = DagSchedulerBuilder.builder()
            .jdbi(jdbi)
            .dispatcher(new InProcessTaskDispatcher(/* 延后注入 */))
            .build();

        // 2. 启动 Admin Service
        this.dagAdminServer = DagAdminServer.builder()
            .jdbi(jdbi)
            .build();

        // 3. 启动内嵌 Agent
        this.agentBootstrap = new EmbeddedAgentBootstrap(config, jdbi);
        this.taskExecutionManager = agentBootstrap.getTaskExecutionManager();

        // 4. 将 InProcessTaskDispatcher 与 TaskExecutionManager 关联
        ((InProcessTaskDispatcher) dagSchedulerServer.getDispatcher())
            .bindTaskExecutionManager(taskExecutionManager);

        // 5. 设置 Agent 的结果回调（直接调用 Scheduler）
        agentBootstrap.setResultReporter(
            new InProcessSchedulerClient(dagSchedulerServer)
        );
    }

    public void start() {
        dagSchedulerServer.start();  // 启动调度循环
        dagAdminServer.start();      // 初始化 Admin Service
        agentBootstrap.start();      // 注册 local-agent 到数据库
    }

    public void stop() {
        taskExecutionManager.gracefulShutdown();
        dagSchedulerServer.stop();
        // DataSource 关闭由 Bootstrap 处理
    }

    // Getters for WebServerBootstrap
    public DagSchedulerServer getDagSchedulerServer() { ... }
    public DagAdminServer getDagAdminServer() { ... }
    public TaskExecutionManager getTaskExecutionManager() { ... }
    public Jdbi getJdbi() { ... }
}
```

### 4.3 InProcessTaskDispatcher

```java
/**
 * In-process task dispatcher that replaces HTTP-based DefaultTaskDispatcher.
 * Directly submits tasks to TaskExecutionManager via method call.
 */
public class InProcessTaskDispatcher implements TaskDispatcher {

    private TaskExecutionManager taskExecutionManager;
    private final TaskDispatchDao dispatchDao;
    private final String dagServerUrl;
    private final ObjectMapper objectMapper;

    @Override
    public DispatchResult dispatch(TaskRecord task) {
        // 1. Build SubmitRequest (same logic as DefaultTaskDispatcher)
        SubmitRequest request = buildSubmitRequest(task);

        // 2. Find embedded agent (always "local-agent")
        AgentStatus localAgent = findLocalAgent();

        // 3. Direct method call instead of HTTP
        SubmitResponse response = taskExecutionManager.submit(request);

        // 4. Record dispatch tracking
        if (response.isSuccess()) {
            dispatchDao.create(TaskDispatchRecord.builder()
                .taskId(task.getId())
                .agentId(localAgent.getAgentId())
                .agentUrl("in-process")
                .status(DispatchStatus.DISPATCHED)
                .build());
        }

        return response.isSuccess()
            ? DispatchResult.success(task.getId(), response.getMessage())
            : DispatchResult.failure(task.getId(), response.getMessage());
    }

    @Override
    public boolean killTask(TaskDispatchRecord dispatchItem, String dealer, String reason) {
        return taskExecutionManager.kill(dispatchItem.taskId());
    }

    @Override
    public boolean forceOk(TaskDispatchRecord dispatchItem, String dealer, String reason) {
        return taskExecutionManager.forceOk(dispatchItem.taskId());
    }

    @Override
    public boolean forceNok(TaskDispatchRecord dispatchItem, String dealer, String reason) {
        return taskExecutionManager.forceNok(dispatchItem.taskId());
    }

    // Called after construction to bind TaskExecutionManager
    public void bindTaskExecutionManager(TaskExecutionManager manager) {
        this.taskExecutionManager = manager;
    }
}
```

### 4.4 InProcessSchedulerClient

```java
/**
 * In-process scheduler client that replaces HTTP-based result reporting.
 * Directly calls DagSchedulerServer.receiveTaskEvent().
 */
public class InProcessSchedulerClient implements AgentSchedulerClient {

    private final DagSchedulerServer schedulerServer;

    @Override
    public boolean resultReport(TaskExecuteResult result) {
        // Direct method call instead of HTTP POST to /api/schedule/agent/result
        return schedulerServer.receiveTaskEvent(
            result.taskId(),
            TaskStatus.valueOf(result.status()),
            result.output()
        );
    }

    @Override
    public boolean heartbeat(AgentStatusReport report) {
        // Embedded agent does not need heartbeat (always alive)
        return true;
    }

    @Override
    public boolean unregister(AgentUnregistration request) {
        // Embedded agent cannot unregister
        return true;
    }
}
```

### 4.5 EmbeddedAgentBootstrap

```java
/**
 * Boots the embedded agent within the same JVM process.
 * Registers a "local-agent" record in the database.
 */
public class EmbeddedAgentBootstrap {

    private final TaskExecutionManager taskExecutionManager;
    private final AgentConfiguration config;
    private final Jdbi jdbi;
    private AgentSchedulerClient schedulerClient;

    public EmbeddedAgentBootstrap(Config appConfig, Jdbi jdbi) {
        this.jdbi = jdbi;
        this.config = AgentConfiguration.builder()
            .agentId("local-agent")
            .dagServerUrl("http://localhost:8080")
            .maxConcurrentTasks(appConfig.getInt("agent.maxConcurrentTasks"))
            .maxPendingTasks(appConfig.getInt("agent.maxPendingTasks"))
            .build();
        this.taskExecutionManager = new TaskExecutionManager(config, null);
    }

    public void start() {
        // Register "local-agent" in t_agent table
        jdbi.useHandle(handle -> {
            handle.createUpdate("""
                INSERT INTO t_agent (agent_id, agent_url, status, max_concurrent_tasks,
                                     max_pending_tasks, created_at, updated_at, embedded)
                VALUES (:agentId, :url, :status, :maxConcurrent, :maxPending, NOW(), NOW(), true)
                ON CONFLICT (agent_id) DO UPDATE SET
                    status = :status, updated_at = NOW()
                """)
                .bind("agentId", config.getAgentId())
                .bind("url", config.getDagServerUrl())
                .bind("status", AgentStatus.ACTIVE.name())
                .bind("maxConcurrent", config.getMaxConcurrentTasks())
                .bind("maxPending", config.getMaxPendingTasks())
                .execute();
        });

        // Start the queue processor
        taskExecutionManager.start();
    }

    public void setResultReporter(AgentSchedulerClient client) {
        this.schedulerClient = client;
        this.taskExecutionManager.setResultReporter(client);
    }

    public TaskExecutionManager getTaskExecutionManager() {
        return taskExecutionManager;
    }
}
```

### 4.6 AllInOneWebServerBootstrap

```java
/**
 * Builds a single MuServer that hosts all APIs on port 8080.
 */
public class AllInOneWebServerBootstrap {

    public MuServer build() {
        MuServerBuilder builder = MuServerBuilder.httpServer()
            .withHttpPort(8080);

        // 1. Cookie JWT Security Handler (whitelist for public paths)
        builder = builder.addHandler(new AllInOneSecurityHandler(
            List.of("/login", "/api/agent/health", "/api/agent/ping",
                    "/swagger", "/static")
        ));

        // 2. Admin handlers (from dag-admin)
        builder = builder.addHandler(new LoginHandler(appContext));
        builder = builder.addHandler(new SwaggerHandler(appContext));
        builder = builder.addHandler(new WorkflowApi(appContext));
        builder = builder.addHandler(new ExecutionApi(appContext));
        builder = builder.addHandler(new StatsApi(appContext));
        builder = builder.addHandler(new AgentAdminApi(appContext));

        // 3. Scheduler handlers (from dag-scheduler)
        builder = builder.addHandler(new AgentRegistryApi(appContext));
        builder = builder.addHandler(new DagManageApi(appContext));
        builder = builder.addHandler(new TaskTemplateApi(appContext));
        builder = builder.addHandler(new TaskOrderApi(appContext));

        // 4. Agent handlers (from dag-agent-muserver, path-prefixed with /api/agent)
        builder = builder.addHandler(new TaskAgentResource(appContext));

        // 5. Static assets
        builder = builder.addHandler(staticAssetHandler());

        return builder.start();
    }
}
```

### 4.7 AllInOneSecurityHandler

```java
/**
 * Unified Cookie JWT security handler for all-in-one mode.
 * Whitelist paths are exempt from authentication.
 */
public class AllInOneSecurityHandler implements MuHandler {

    private final JwtHelper jwtHelper;
    private final Set<String> whitelistPaths;

    @Override
    public boolean handle(MuRequest request, MuResponse response) {
        String path = request.uri().getPath();

        // 1. Check whitelist
        if (isWhitelisted(path)) {
            return false; // Continue to next handler
        }

        // 2. Validate Cookie JWT
        Cookie cookie = request.cookie("token");
        if (cookie == null || !jwtHelper.validate(cookie.value())) {
            response.status(401);
            response.write("Unauthorized");
            return true;
        }

        // 3. Set user context in request attribute
        request.attribute("user", jwtHelper.extractUser(cookie.value()));
        return false; // Continue to next handler
    }
}
```

---

## 5. 数据流设计

### 5.1 启动时序

```
1. AllInOneApp.main()
   ├── 2. loadConfig(env) ──→ application.conf + application-{env}.conf
   ├── 3. new AllInOneAppContext(config)
   │   ├── 3.1 DatabaseBootstrap.start()
   │   │   ├── DataSource (HikariCP)
   │   │   ├── Jdbi
   │   │   └── Flyway migrate
   │   ├── 3.2 DagSchedulerServer.start()
   │   │   ├── Init DAOs (TaskRecordDao, TaskOrderDao, ...)
   │   │   ├── Init Services (DagManageService, DagQueryService, ...)
   │   │   ├── Inject InProcessTaskDispatcher
   │   │   └── Start scheduler loop thread
   │   ├── 3.3 DagAdminServer.start()
   │   │   └── Init Admin Service beans
   │   ├── 3.4 EmbeddedAgentBootstrap.start()
   │   │   ├── Create TaskExecutionManager
   │   │   ├── Create AgentConfiguration (agentId="local-agent")
   │   │   ├── INSERT/UPSERT "local-agent" into t_agent
   │   │   ├── Set InProcessSchedulerClient as result reporter
   │   │   └── Start queue processor thread
   │   └── 3.5 Bind InProcessTaskDispatcher ↔ TaskExecutionManager
   └── 4. AllInOneWebServerBootstrap.build().start()
       ├── MuServer on port 8080
       ├── Register all handlers (Admin + Scheduler + Agent)
       └── Register SecurityHandler
```

### 5.2 任务执行流（单次完整链路）

```
[用户] POST /api/schedule/dag/start/{orderKey} (Cookie JWT)
         │
         ▼
[DagManageApi] dagScheduleService.start(orderKey)
         │
         ▼
[DagScheduleServiceImpl]
    ├── findReadyTasksForOrder(orderKey) ──→ SQL 查询
    └── for each task: taskDispatcher.dispatch(task)
         │
         ▼
[InProcessTaskDispatcher]
    ├── Build SubmitRequest
    ├── taskExecutionManager.submit(request)
    └── Record dispatch tracking in DB
         │
         ▼
[TaskExecutionManager]
    ├── pendingQueue.add(task)
    └── queueProcessor 取出 → threadPool.execute()
         │
         ▼
[TaskExecutionEngine]
    ├── Load TaskExecution by executionClass
    ├── Execute task (doExecute)
    └── On complete: resultReporter.report(result)
         │
         ▼
[InProcessSchedulerClient]
    └── dagSchedulerServer.receiveTaskEvent(taskId, status, output)
         │
         ▼
[DagSchedulerServer]
    ├── Update task status in DB
    ├── findReadySuccessors(taskId) ──→ SQL 查询
    └── For each ready successor: dispatch(successor)
         │
         ▼
    (递归触发后继任务，直到所有任务完成)
```

### 5.3 结果回调流

```
[TaskExecutionEngine] 任务完成
    │
    ├── 成功 → resultReport(TaskExecuteResult.success(...))
    ├── 失败 → resultReport(TaskExecuteResult.error(...))
    └── 超时 → resultReport(TaskExecuteResult.timeout(...))
         │
         ▼
[InProcessSchedulerClient]
    └── dagSchedulerServer.receiveTaskEvent(taskId, SUCCESS, output)
         │
         ▼
[DagSchedulerServer]
    ├── taskRecordDao.updateStatus(taskId, SUCCESS)
    ├── task.success() → findReadySuccessors(taskId)
    └── 触发后继任务调度
```

---

## 6. 错误处理

| 场景 | 行为 | 备注 |
|------|------|------|
| 数据库连接失败 | `DatabaseBootstrap` 抛异常，`main()` 打印错误后 `System.exit(1)` | Fail-fast |
| 端口 8080 被占用 | `MuServer.start()` 抛 `BindException`，进程退出 | Fail-fast |
| 调度循环异常 | `DagSchedulerServer` 内部捕获，记录日志，继续下一轮扫描 | 已有机制 |
| 任务执行异常 | `TaskExecutionManager` 捕获，标记 ERROR，上报结果 | 已有机制 |
| 内嵌 Agent 启动失败 | `EmbeddedAgentBootstrap` 抛异常，`AllInOneAppContext` 向上传播 | Fail-fast |
| JWT 验证失败 | `AllInOneSecurityHandler` 返回 401 | 已有机制 |
| 优雅关闭 | ShutdownHook：停止 MuServer → 停止调度循环 → gracefulShutdown Agent → 关闭 DataSource | 按依赖顺序 |

---

## 7. 安全设计

### 7.1 认证

- **统一 Cookie JWT**：所有端点共用一套认证
- **白名单路径**：`/login`、`/api/agent/health`、`/api/agent/ping`、`/swagger/*`、`/static/*`
- **无 Bearer Token**：allinone 模式下不存在外部 Agent，无需 Bearer Token 认证

### 7.2 Agent 标识

- `local-agent` 在 `t_agent` 表中带有 `embedded = true` 标记
- 调度器知道 `local-agent` 是内嵌的，不发送 HTTP 请求
- `InProcessTaskDispatcher` 的 `agentUrl` 字段记录为 `"in-process"`

### 7.3 资源隔离

- `TaskExecutionManager` 有独立的 `maxConcurrentTasks` 限制
- 调度器线程池与 Agent 线程池分离
- 共享 `DataSource` 的连接池由 HikariCP 管理

---

## 8. 配置设计

### 8.1 配置文件

```
dag-allinone-muserver/src/main/resources/config/
├── application.conf         (根配置)
├── application-dev.conf     (开发环境)
└── application-prod.conf    (生产环境)
```

### 8.2 配置项

```hocon
# Database (shared)
database {
    url = "jdbc:postgresql://localhost:5432/dagtask"
    username = "dagtask"
    password = "dagtask"
    pool {
        maxSize = 20
    }
}

# AllInOne Server
server {
    port = 8080
    host = "0.0.0.0"
}

# Embedded Agent
agent {
    maxConcurrentTasks = 8
    maxPendingTasks = 100
    taskLogDir = "/tmp/dagtask/logs"
}

# JWT
jwt {
    secret = "your-secret-key"
    expiration = 86400  # 24 hours
}

# Scheduler
scheduler {
    scanInterval = 5000  # ms
    maxRetries = 3
}
```

---

## 9. 测试策略

### 9.1 单元测试

| 测试类 | 目标 | Mock 依赖 |
|--------|------|----------|
| `InProcessTaskDispatcherTest` | 任务分发逻辑 | TaskExecutionManager, TaskDispatchDao |
| `InProcessSchedulerClientTest` | 结果回调逻辑 | DagSchedulerServer |
| `EmbeddedAgentBootstrapTest` | Agent 启动与注册 | Jdbi, AgentConfiguration |
| `AllInOneSecurityHandlerTest` | 认证白名单 | JwtHelper |
| `AllInOneAppContextTest` | 上下文初始化 | Config, DatabaseBootstrap |

### 9.2 集成测试

| 测试类 | 场景 | 说明 |
|--------|------|------|
| `AllInOneIntegrationTest` | 端到端 | 使用 embedded-postgres，完整链路验证 |
| `AllInOneStartupTest` | 启动/停止 | 验证各组件按正确顺序启动和关闭 |

### 9.3 集成测试示例

```java
@Test
public void testEndToEndDagExecution() {
    // 1. Start AllInOneApp with embedded Postgres
    AllInOneApp app = new AllInOneApp("test");
    app.start();

    try {
        // 2. Login to get JWT cookie
        String jwt = login("admin", "password");

        // 3. Create task templates
        createTaskTemplate("TaskA", jwt);
        createTaskTemplate("TaskB", jwt);

        // 4. Create DAG with dependency A → B
        String orderKey = createDagOrder("TaskA", "TaskB", jwt);

        // 5. Trigger execution
        startDag(orderKey, jwt);

        // 6. Wait and verify
        await().atMost(30, SECONDS).until(() ->
            getTaskStatus("TaskB", jwt).equals("SUCCESS")
        );
    } finally {
        app.stop();
    }
}
```

---

## 10. 迁移与兼容性

### 10.1 数据库兼容性

- 使用与分布式模式**相同的数据库 Schema**
- `t_agent` 表新增 `embedded` 字段（BOOLEAN，默认 false）
- Flyway 迁移脚本：V6__add_agent_embedded_flag.sql

### 10.2 API 兼容性

- AllInOne 的 API 路径是分布式模式的**子集**（路径前缀不同）
- 客户端需要调整 base URL 和路径前缀

### 10.3 部署差异

| 维度 | 分布式模式 | AllInOne 模式 |
|------|-----------|--------------|
| 进程数 | 3 | 1 |
| 端口数 | 3 | 1 |
| 认证 | Bearer Token + Cookie JWT | Cookie JWT only |
| 任务分发 | HTTP | 方法调用 |
| 适用场景 | 生产分布式 | 开发/测试/轻量生产 |

---

## 11. 文件清单

### 11.1 新建文件

| 文件 | 模块 | 说明 |
|------|------|------|
| `dag-allinone/pom.xml` | dag-allinone | 聚合模块 POM |
| `dag-allinone-muserver/pom.xml` | dag-allinone-muserver | 启动入口 POM |
| `dag-allinone-muserver/src/main/java/.../AllInOneApp.java` | dag-allinone-muserver | 主入口 |
| `dag-allinone-muserver/src/main/java/.../AllInOneAppContext.java` | dag-allinone-muserver | 上下文初始化 |
| `dag-allinone-muserver/src/main/java/.../InProcessTaskDispatcher.java` | dag-allinone-muserver | 内嵌任务分发器 |
| `dag-allinone-muserver/src/main/java/.../InProcessSchedulerClient.java` | dag-allinone-muserver | 内嵌结果回调 |
| `dag-allinone-muserver/src/main/java/.../EmbeddedAgentBootstrap.java` | dag-allinone-muserver | 内嵌 Agent 启动器 |
| `dag-allinone-muserver/src/main/java/.../AllInOneWebServerBootstrap.java` | dag-allinone-muserver | 统一 Web 服务启动 |
| `dag-allinone-muserver/src/main/java/.../AllInOneSecurityHandler.java` | dag-allinone-muserver | 统一安全处理器 |
| `dag-allinone-muserver/src/main/java/.../database/DatabaseBootstrap.java` | dag-allinone-muserver | 数据库初始化 |
| `dag-allinone-muserver/src/main/resources/config/application.conf` | dag-allinone-muserver | 根配置 |
| `dag-allinone-muserver/src/main/resources/config/application-dev.conf` | dag-allinone-muserver | 开发配置 |
| `dag-allinone-muserver/src/main/resources/config/application-prod.conf` | dag-allinone-muserver | 生产配置 |
| `dag-allinone-muserver/metadata/metadata.json` | dag-allinone-muserver | 模块元数据 |
| `dag-allinone-muserver/README.md` | dag-allinone-muserver | 使用说明 |
| `dag-si/src/main/resources/db/migration/postgresql/V6__add_agent_embedded_flag.sql` | dag-si | Flyway 迁移 |

### 11.2 修改文件

| 文件 | 模块 | 修改内容 |
|------|------|---------|
| `dag-task/pom.xml` | dag-task | 新增 `dag-allinone` 和 `dag-allinone-muserver` 模块 |
| `dag-task/pom.xml` | dag-task | dependencyManagement 新增条目 |

---

## 12. 风险评估

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| MuServer handler 路由冲突 | 中 | 高 | 严格路径前缀隔离 + 集成测试验证 |
| 线程池资源争用 | 低 | 中 | 独立配置 maxConcurrentTasks |
| 内嵌 Agent 与分布式 Agent 数据不一致 | 低 | 低 | embedded=true 标记区分 |
| 数据库连接池耗尽 | 低 | 高 | HikariCP 合理配置 maxSize |
| 单点故障（单进程） | 高 | 高 | 文档明确 allinone 仅用于轻量场景 |

---

*文档完成* ✅ | 2026-05-31
