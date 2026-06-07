# dag-scheduler 六边形（Ports & Adapters）重构设计

- 日期：2026-06-07
- 试点范围：仅 `dag-scheduler` 子系统
- 主要驱动力：为未来切换 Web 框架（Spring Web / Micronaut）与持久化技术（本地文件 / KV / 文档库）做准备，避免架构被锁死
- 状态：设计已确认，待写实施计划

---

## 1. 目标与原则

**目标**：在 `dag-scheduler` 试点 Ports & Adapters（六边形）架构，使 Web 框架与持久化技术**都可以独立替换**，但 core 不知道也不关心当前用的是谁。

**核心原则**：

1. **Core 零基础设施依赖**：`scheduler-domain` 模块只允许依赖 JDK、SLF4J、`dag-si`。**禁止 import**：
   - MuServer / Servlet API
   - Jdbi / `java.sql.*` / `javax.sql.DataSource`（DataSource 也是技术细节，不能漏到 core）
   - Jackson / 任何序列化库
   - Flyway
   - Spring / Micronaut / Guice / Avaje 等任何 DI 框架
   - `jakarta.*` / `javax.*`（除标准 JDK 自带）
2. **依赖方向严格内向**：adapters → application → domain，反向严禁。用 Maven 模块边界 + ArchUnit 测试强制约束。
3. **Port = 接口在内、实现在外**：
   - inbound port（UseCase 接口）由 web adapter 调用；
   - outbound port（Repository、UnitOfWork、Clock、IdGenerator、AgentDispatcher 等）由持久化/基础设施 adapter 实现。
4. **YAGNI**：只抽出今天 muserver + jdbc 就需要的端口。其它端口（缓存、事件、metrics 等）等真要用再加，不预先发明。

---

## 2. 模块拆分

把现有的 `dag-scheduler` + `dag-scheduler-muserver` 重组为 4 个 Maven 模块：

```
dag-scheduler/                       (pom 聚合模块)
├── dag-scheduler-domain/            纯领域 + ports（无任何框架依赖）
│   src/main/java/top/ilovemyhome/dagtask/scheduler/
│       ├── domain/...               (entities, value objects, domain services)
│       ├── application/...          (use case 实现：application services)
│       ├── port/in/...              (inbound ports：UseCase 接口、Command/Result records)
│       └── port/out/...             (outbound ports：Repository / UnitOfWork / Clock / IdGenerator / AgentDispatcher)
│
├── dag-scheduler-adapter-persistence-jdbc/
│   src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/persistence/jdbc/
│       └── 实现 port.out 下的 Repository / UnitOfWork
│   src/main/resources/db/migration/  (Flyway 迁移由此 adapter 接管)
│
├── dag-scheduler-adapter-web-muserver/
│   src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/
│       └── 调用 port.in 下的 UseCase
│
└── dag-scheduler-app/               组装入口（main + 手工 DI）
    src/main/java/top/ilovemyhome/dagtask/scheduler/app/
        └── SchedulerContext / SchedulerBootstrap / SchedulerConfig 加载
```

### 2.1 依赖图（强制）

```
                  scheduler-app
                 /      |       \
                v       v        v
       persistence    web      domain
           jdbc    muserver       ^
              \       |          /
               \      v         /
                +-> domain <---+
                    (port.out / port.in)
```

| 模块 | 依赖 |
|---|---|
| `scheduler-domain` | `dag-si`、SLF4J；**禁止**所有框架 |
| `scheduler-adapter-persistence-jdbc` | `scheduler-domain`、Jdbi、Flyway、HikariCP |
| `scheduler-adapter-web-muserver` | `scheduler-domain`、MuServer、Jackson |
| `scheduler-app` | `scheduler-domain` + 上述两个 adapter，提供 `main()` 和组装代码（手工 DI，无 Spring） |

### 2.2 ArchUnit 守护

放在 `scheduler-domain` 的测试代码里：

```java
@Test
void domain_must_not_depend_on_any_framework() {
    noClasses().that().resideInAPackage("..scheduler.domain..")
        .or().resideInAPackage("..scheduler.application..")
        .or().resideInAPackage("..scheduler.port..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "io.muserver..", "org.jdbi..", "org.springframework..",
            "io.micronaut..", "jakarta..", "javax.servlet..",
            "javax.sql..", "java.sql..",
            "com.fasterxml.jackson..", "org.flywaydb..")
        .check(importedClasses);
}

@Test
void port_out_method_signatures_must_not_throw_infra_exceptions() {
    // port.out 接口的方法 throws 列表里不允许出现 Jdbi / MuServer / Spring 等异常
}
```

### 2.3 未来扩展示例

- 要加 Spring Web 适配？新建 `dag-scheduler-adapter-web-springweb`，`app` 模块换一行依赖即可。
- 要加文件持久化？新建 `dag-scheduler-adapter-persistence-file` 即可。
- **domain 模块一行不动**。

---

## 3. Ports 设计

### 3.1 Inbound Ports（UseCase，web adapter 调用）

```java
// port.in
public interface SubmitDagRunUseCase {
    DagRunId submit(SubmitDagRunCommand cmd);
}

public interface QueryDagRunUseCase {
    Optional<DagRunView> findById(DagRunId id);
    Page<DagRunView> query(DagRunQuery query);
}

public interface DispatchTasksUseCase {           // 调度器主循环触发
    DispatchResult dispatchOnce();
}

public interface ReportTaskResultUseCase {        // agent 回调结果
    void report(TaskResultReport report);
}

public interface RegisterAgentUseCase { ... }
public interface AgentHeartbeatUseCase { ... }
```

**纪律**：

- **命令对象**（`SubmitDagRunCommand`、`TaskResultReport` 等）是不可变 record，**不含任何 web 字段**（无 HTTP header / cookie / sessionId）。web adapter 负责把 HTTP 请求拆解成命令。
- **返回值** 是领域 view 对象，不是 JSON / DTO。序列化是 web adapter 的事。
- 命名用 `UseCase` 后缀，一个用例一个接口，不要把所有方法塞进一个胖 service。

### 3.2 Outbound Ports（core 定义，adapter 实现）

```java
// port.out — 领域语义 Repository（不是通用 CRUD，不暴露 SQL / Specification）
public interface DagRunRepository {
    DagRunId nextId();
    void save(DagRun run);
    Optional<DagRun> findById(DagRunId id);
    List<DagRun> findRunnable(int limit);                     // 调度循环用
    List<DagRun> findStuck(Instant olderThan, int limit);     // 监控用
}

public interface TaskExecutionRepository {
    void save(TaskExecution execution);
    List<TaskExecution> findByDagRun(DagRunId runId);
    List<TaskExecution> findRunnableIn(DagRunId runId);       // 依赖已就绪的任务
    int markTimeoutsAsFailed(Instant deadline);
}

public interface AgentRepository {
    void register(Agent agent);
    void heartbeat(AgentId id, Instant at);
    List<Agent> findAlive(AgentTag tag, Instant aliveSince);
    void markOffline(AgentId id, Instant at);
}

// port.out — 横切能力
public interface UnitOfWork {
    <T> T execute(Supplier<T> work);                          // 显式事务边界
    default void execute(Runnable work) {
        execute(() -> { work.run(); return null; });
    }
}

public interface Clock {                                      // 不用 java.time.Clock，避免被框架抢走
    Instant now();
}

public interface IdGenerator {
    String newId();
}

// port.out — 调度器向 agent 发任务（今天是 HTTP，明天可能是 gRPC / MQ）
public interface AgentDispatcher {
    DispatchAck dispatch(AgentId target, TaskAssignment assignment);
}
```

### 3.3 端口设计纪律

1. **Repository 不返回 Optional<List>、不接受 Specification、不暴露 SQL**。每个方法都对应一个真实用例。
2. **不抽象不需要的**：今天没有缓存就不要 `CachePort`；今天没有事件就不要 `EventPublisher`。
3. **UnitOfWork 的一致性语义文档化**：
   - JDBC 实现 = 真事务（默认 READ_COMMITTED，必要时显式声明 SERIALIZABLE 范围）
   - 未来文件实现 = 进程内锁 + 最终一致
   - 未来 KV 实现 = 单 key 原子
   - **core 不假设强事务**，application service 写代码时按"可能弱一致"的最坏假设来。
4. **AgentDispatcher 是 outbound port**：现在调用 agent 的 HTTP client 在 core 看来也是基础设施，要藏到 adapter 后面。

---

## 4. 组装、配置、错误处理

### 4.1 组装（手工 DI + Avaje 友好留口）

`scheduler-app` 模块里一个 `SchedulerContext` 类负责手工 DI：

```java
public final class SchedulerContext {
    private final SubmitDagRunUseCase submitDagRunUseCase;
    private final DispatchTasksUseCase dispatchTasksUseCase;
    // ... 其它 use case

    public static SchedulerContext bootstrap(SchedulerConfig config) {
        // 1) 基础设施
        DataSource ds = HikariFactory.create(config.datasource());
        Jdbi jdbi = JdbiFactory.create(ds);
        FlywayMigrator.migrate(ds);

        // 2) outbound adapters (实现 port.out)
        Clock clock = new SystemClock();
        IdGenerator idGen = new UuidIdGenerator();
        UnitOfWork uow = new JdbiUnitOfWork(jdbi);
        DagRunRepository dagRunRepo = new JdbiDagRunRepository(jdbi);
        TaskExecutionRepository taskExecRepo = new JdbiTaskExecutionRepository(jdbi);
        AgentRepository agentRepo = new JdbiAgentRepository(jdbi);
        AgentDispatcher dispatcher = new HttpAgentDispatcher(config.agentHttp());

        // 3) application services (实现 port.in，依赖 port.out)
        var submit = new SubmitDagRunService(dagRunRepo, idGen, clock, uow);
        var dispatch = new DispatchTasksService(dagRunRepo, taskExecRepo,
                                                agentRepo, dispatcher, clock, uow);
        // ...

        return new SchedulerContext(submit, dispatch, ...);
    }
}
```

**为什么手工 DI**：四个模块加起来类不会太多，手工 DI 让"谁依赖谁"一眼看穿；不引入 Spring/Guice 即避免 `app` 模块被 DI 框架绑架。

**Avaje 友好留口**：

- 所有类一律构造函数注入，**不写任何 setter / field 注入**。
- 不使用静态单例 / `ServiceLoader`。
- 命名规范每个 adapter 模块对应一个 Avaje module（将来加 `@Module` 注解和 APT 即可，业务代码零修改）。
- 何时升级到 Avaje Inject：当 `SchedulerContext.bootstrap()` 超过约 150 行难以维护时再做。

### 4.2 配置

- `SchedulerConfig` 是 POJO / record，定义在 `domain` 模块（或独立的 `scheduler-config` 包），**不依赖任何配置库**。
- 配置加载（从 yaml / env / 外部配置中心）在 `app` 模块完成，可以用任何工具。
- core 不知道配置从哪来。

### 4.3 错误处理

三层错误模型：

| 层 | 异常类型 | 谁抛 | 谁处理 |
|---|---|---|---|
| Domain | `DomainException`（及子类：`DagNotFoundException`、`InvalidStateTransitionException`…）| application / domain service | inbound adapter 翻译成 HTTP/gRPC 错误 |
| Port out 失败 | `PersistenceException`、`OptimisticLockException`、`AgentUnreachableException`（**定义在 port.out 里**）| outbound adapter | application service 决策重试/降级/上抛 |
| Adapter 内部 | `JdbiException` / `MuException` 等技术异常 | adapter 内部 | **必须**在 adapter 边界翻译成上面两类，**不允许穿透到 core** |

**`PersistenceException` 定义在 core 的 port.out 包**——这是六边形的一个折中：application service 需要 catch 它来做重试决策，纯派做法会丢信息或发明假领域异常。该类自身**不依赖任何具体技术**，ArchUnit 强制约束。

- ArchUnit 规则：`port.out` 接口的方法签名 **不允许 throws 任何 `org.jdbi.*` / `io.muserver.*` 异常**。
- adapter-web 里有一个 `ExceptionMapper`，把 `DomainException` 子类映射成 HTTP 状态码（404 / 409 / 422）；将来 Spring adapter 自己写一个 `@ControllerAdvice` 即可。

### 4.4 测试策略

- **domain 模块**：纯单元测试，所有 port.out 用手写 Fake（`InMemoryDagRunRepository` 等）。**不许引入 Mockito 写 Repository 的 mock**——Fake 更接近真实行为且能复用。
- **adapter-persistence-jdbc**：集成测试，用 Testcontainers + 真实 PG / MySQL，验证 Repository 契约。
- **adapter-web-muserver**：用 MuServer 启 in-process 服务，对 UseCase 用 Mockito mock，只验证"HTTP ↔ Command/Result"映射。
- **契约测试（首期范围）**：
  - 给 3 个核心 Repository（`DagRunRepository` / `TaskExecutionRepository` / `AgentRepository`）和 `UnitOfWork` 写抽象契约测试基类。
  - JDBC adapter 继承运行（Testcontainers）。
  - 不实现第二个 adapter；契约基类先写好，将来加 file/KV 时立刻能验证。
  - 工作量约多 20%，但这是六边形"可换持久化"的最后一公里证据。

---

## 5. 迁移路线（不动现有 allinone，分步切换）

每一步都可独立 commit、可回滚。每一步完成后要求：

- `mvn clean verify` 全绿
- `dag-allinone` 启动整测通过
- 至少一个 HTTP api-test（`api-test/` 下）通过

### 步骤 1：建模块骨架

- 新建 `dag-scheduler-domain` / `-adapter-persistence-jdbc` / `-adapter-web-muserver` / `-app` 四个空模块
- 配 Maven 聚合 + `flatten-maven-plugin`（按 parent pom 模板）
- 加 ArchUnit 测试（先 mark 为 disabled，等代码搬进来再开启）
- `dag-scheduler` 旧模块**保留**不动

### 步骤 2：搬 domain

- 从 `dag-scheduler/core` 把领域类、service 接口搬到 `-domain`
- 定义 port.in / port.out（按 §3）
- 用 Fake 写完单元测试
- **旧代码继续编译运行**（domain 是新增）

### 步骤 3：搬 adapters

- 把 dao 实现搬到 `-adapter-persistence-jdbc` 实现 port.out
- Flyway 迁移文件 `db/migration` 一并搬到此模块的 `src/main/resources`
- 把 `dag-scheduler-muserver` 控制器搬到 `-adapter-web-muserver` 调 port.in
- 补契约测试 + 集成测试
- 开启 ArchUnit 全部规则

### 步骤 4：切 allinone，删旧模块

- `dag-allinone` 把对 `dag-scheduler` + `dag-scheduler-muserver` 的依赖换成 `dag-scheduler-app`（或直接依赖 domain + 两个 adapter，按 allinone 的组装习惯）
- 旧 `dag-scheduler` + `dag-scheduler-muserver` 整体删除
- 在 `docs/` 下补一份 `10-ARCHITECTURE-hexagonal-refactor-pilot.md` 编号文档作为最终记录

---

## 6. 风险与应对

| 风险 | 应对 |
|---|---|
| 现有 dao 里"跨 service 的复杂 SQL"难拆成单一 Repository | 步骤 2 搬 domain 时先识别这些 SQL，按"哪个 use case 用"拆 Repository 方法；实在拆不开报警，回到设计讨论 |
| Flyway 迁移文件位置变了，启动找不到 | adapter-persistence-jdbc 接管 `db/migration` 目录，allinone 启动时由该 adapter 自己跑迁移；步骤 3 启动整测验证 |
| ArchUnit 引入新依赖 | 只在 `-domain` test scope，不影响产物 |
| JDK 25 + ArchUnit / Testcontainers 兼容性 | 步骤 1 完成时立即 `mvn test` 验证；如不兼容回到设计讨论选替代方案 |

---

## 7. 非目标（YAGNI）

明确**本期不做**：

- ❌ 不抽 cache / event publisher / metrics port
- ❌ 不动 `dag-agent` / `dag-admin` / `dag-allinone` 的内部结构（只让 allinone 引用方式变更）
- ❌ 不引入任何 DI 容器（仅留 Avaje 友好结构）
- ❌ 不引入 Spring / Micronaut（仅证明"未来可加"）
- ❌ 不实现第二个持久化 adapter（仅写契约测试基类，证明"未来可加"）
- ❌ 不改 dag-si 现有领域模型（如果 si 内有技术耦合，记入后续待办，本期不动）

---

## 8. 验收标准

本次重构完成的判定标准：

1. `dag-scheduler/` 下只有 4 个新模块 + pom 聚合，旧 `dag-scheduler` / `dag-scheduler-muserver` 已删除
2. `mvn clean verify` 全绿（包含 ArchUnit + Testcontainers 集成测试 + 契约测试）
3. `dag-allinone` 启动整测通过
4. `api-test/` 下原有 scheduler 相关 HTTP 测试全部通过
5. `scheduler-domain` 模块的 `pom.xml` 不含任何 Web / 持久化 / DI 框架依赖
6. 文档 `docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md` 写就，包含最终目录结构和迁移总结
