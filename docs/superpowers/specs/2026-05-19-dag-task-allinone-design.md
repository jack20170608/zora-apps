# dag-task-allinone 模块设计文档

## 1. 背景与目标

### 1.1 现状

dag-task 项目当前包含多个独立模块：
- `dag-si`：领域模型
- `dag-scheduler-core`：调度核心逻辑
- `dag-scheduler-muserver`：调度中心 HTTP API（MuServer）
- `dag-agent-core`：任务执行核心
- `dag-agent-muserver`：任务执行器 HTTP API（MuServer）
- `dag-admin-core`：管理后台核心
- `dag-admin-muserver`：管理后台 HTTP API（MuServer）
- `dag-admin-web`：前端管理界面（Next.js 14）

当前系统基于 PostgreSQL + JDBI 构建，各模块独立部署，通过 REST API 通信。

### 1.2 目标

新增 `dag-task-allinone` 模块，实现**单个服务一键启动**，满足以下场景：
- 开发/测试快速验证
- 演示/体验环境
- 轻量生产部署（低负载小团队）

核心特征：
- 单进程运行：scheduler + agent + admin API 全部内嵌
- 单端口访问：MuServer 同时提供 API 和前端静态资源
- 内嵌存储：无需外部数据库，默认使用内嵌存储（H2 或 RocksDB）
- 存储可插拔：未来可扩展 RocksDB、图数据库等持久化策略

---

## 2. 方案选择

经过 3 个方案的对比，最终选择 **方案1：Aggregator + Full Migration**。

| 维度 | 方案1（Aggregator + Full Migration） | 方案2（Sidecar Adapter） | 方案3（Lean Kernel） |
|---|---|---|---|
| 现有模块改动 | 全量迁移到通用 Repository | 零改动 | 不依赖现有模块 |
| 存储可插拔 | 是，统一接口层 | 否，双轨并行 | 是，但独立代码 |
| 长期维护成本 | 低 | 高（两套路径） | 高（两套业务逻辑） |
| 与现有系统一致性 | 高 | 低 | 低 |
| 实现工作量 | 大 | 小 | 中 |

选择理由：
- 用户明确要求全量 DAO 抽象 + 现有模块全量迁移
- 统一的 Repository 接口层符合领域驱动设计，长期架构最健康
- all-in-one 只是"组装层"，业务逻辑复用现有 core，避免重复造轮子

---

## 3. 模块划分与依赖关系

### 3.1 新增模块

| 模块 | 类型 | 职责 |
|---|---|---|
| `dag-repository-api` | 接口层 | 定义所有 Repository 接口、查询条件、分页、事务边界。零外部依赖（除 dag-si 领域模型） |
| `dag-repository-jdbi` | 实现层 | 基于现有 JDBI + Flyway，对接 PostgreSQL / H2（SQL 存储实现） |
| `dag-repository-rocksdb` | 实现层 | 基于 RocksDB JNI，实现全套 Repository 接口（KV 存储实现） |
| `dag-task-allinone` | 聚合启动 | 组装 scheduler + agent + admin API + 前端静态资源 + 内嵌存储 |

### 3.2 依赖关系图

```
dag-task-allinone
  ├── dag-scheduler-muserver (API 层)
  ├── dag-agent-muserver     (API 层)
  ├── dag-admin-muserver     (API 层)
  ├── dag-repository-jdbi    or  dag-repository-rocksdb
  └── dag-admin-web          (前端静态资源，打包时嵌入)

dag-scheduler-core / dag-agent-core / dag-admin-core
  └── dag-repository-api     (仅依赖接口，不依赖实现)

dag-repository-jdbi / dag-repository-rocksdb
  └── dag-repository-api
```

### 3.3 迁移路径

1. 新增 `dag-repository-api` 模块，定义通用 Repository 接口（独立于 dag-si，保持 dag-si 纯净）
2. 现有 core 模块（dag-scheduler-core 等）中的 JDBI DAO 类，全部迁移到 `dag-repository-jdbi`
3. core 模块中的 Service 层，从注入 `Jdbi` 改为注入 `RepositoryFactory` 或具体 Repository 接口
4. core 模块的 pom.xml 中，依赖从 JDBI 相关改为 `dag-repository-api`
5. `dag-task-allinone` 作为聚合入口，按需注入具体的 Repository 实现

---

## 4. Repository 接口层设计（dag-repository-api）

### 4.1 核心接口

```java
/**
 * 基础 CRUD 接口，所有领域 Repository 的父接口
 */
public interface Repository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);
    boolean existsById(ID id);
}

/**
 * 支持分页查询的 Repository
 */
public interface PageableRepository<T, ID> extends Repository<T, ID> {
    Page<T> findAll(QueryCondition condition, PageRequest pageRequest);
    long count(QueryCondition condition);
}
```

### 4.2 查询条件抽象

```java
/**
 * 通用查询条件，覆盖现有 90%+ 查询场景
 */
public class QueryCondition {
    private final List<Filter> filters;
    private final List<Sort> sorts;

    public static Filter eq(String field, Object value);
    public static Filter ne(String field, Object value);
    public static Filter gt(String field, Object value);
    public static Filter gte(String field, Object value);
    public static Filter lt(String field, Object value);
    public static Filter lte(String field, Object value);
    public static Filter in(String field, Collection<?> values);
    public static Filter between(String field, Object from, Object to);
    public static Filter like(String field, String pattern);
    public static Filter isNull(String field);
}
```

### 4.3 事务边界

```java
/**
 * 工作单元，替代现有的 JDBI 事务管理
 */
public interface UnitOfWork {
    void run(Runnable operation);
    <T> T call(Callable<T> operation);
}

/**
 * Repository 工厂，Service 层的统一入口
 */
public interface RepositoryFactory {
    <R extends Repository<?, ?>> R getRepository(Class<R> type);
    UnitOfWork currentUnitOfWork();
}
```

### 4.4 领域专用 Repository 清单

| 领域实体 | Repository 接口 | 核心方法示例 |
|---|---|---|
| Workflow | `WorkflowRepository` | findByName, findByStatus, findByTag |
| TaskDefinition | `TaskDefinitionRepository` | findByWorkflowId, findByStatus, findByType |
| TaskExecution | `TaskExecutionRepository` | findByWorkflowExecutionId, findByAgentId, findRunningTasks |
| WorkflowExecution | `WorkflowExecutionRepository` | findByWorkflowId, findByStatus, findByTimeRange |
| Dependency | `DependencyRepository` | findByWorkflowId, findUpstreamTasks, findDownstreamTasks |
| Agent | `AgentRepository` | findByStatus, findHealthyAgents, updateHeartbeat |
| AgentTask | `AgentTaskRepository` | findByAgentId, findByExecutionId |
| TaskTemplate | `TaskTemplateRepository` | findByCategory, findByName |
| ExecutionLog | `ExecutionLogRepository` | findByExecutionId |
| SystemConfig | `SystemConfigRepository` | findByKey |

> 注：以上为初版清单，实际实现时需根据现有 JDBI DAO 完整梳理后最终确定。

---

## 5. 前端集成方案

### 5.1 方案：静态资源内嵌

- `dag-admin-web`（Next.js）通过 `next export` 或等效命令打包为静态 HTML/CSS/JS
- 构建时，`dag-task-allinone` 通过 Maven Resource Plugin 将前端静态资源复制到 `src/main/resources/web`
- MuServer 启动时，注册静态资源处理器：
  - `/api/*` -> REST API 路由
  - `/` 及 `/*` -> 静态资源路由（单页应用模式需支持前端路由 fallback）

### 5.2 访问方式

- 单端口访问，例如 `http://localhost:8080`
- API 统一前缀 `/api/v1/...`
- 前端页面通过 `/` 直接访问

---

## 6. 内嵌存储方案

### 6.1 默认实现：H2（PostgreSQL 兼容模式）

- `dag-repository-jdbi` 默认使用 H2（内存模式或文件模式）
- Flyway 迁移脚本可直接运行（H2 兼容大部分 PostgreSQL 语法）
- 启动最快，零配置，适合开发和演示

### 6.2 可选实现：RocksDB

- `dag-repository-rocksdb` 基于 RocksDB JNI
- 需要为每个 Repository 接口实现 KV 映射逻辑
- 适合高性能单机场景

### 6.3 配置切换

```yaml
# allinone.yml
storage:
  type: jdbi-h2   # jdbi-h2 | jdbi-postgresql | rocksdb
  # H2 配置
  h2:
    mode: file    # memory | file
    path: ./data/dag-task
  # PostgreSQL 配置（连接外部库）
  postgresql:
    url: jdbc:postgresql://localhost:5432/dagtask
    username: dag
    password: ***
  # RocksDB 配置
  rocksdb:
    path: ./data/rocksdb
```

---

## 7. all-in-one 服务组装

### 7.1 启动时组件初始化顺序

1. **配置加载**：读取 `allinone.yml`，确定存储类型和端口等参数
2. **存储初始化**：
   - 根据 `storage.type` 实例化对应的 RepositoryFactory
   - 若是 jdbi 实现，执行 Flyway 迁移
   - 若是 rocksdb 实现，初始化 RocksDB 实例和列族
3. **核心服务初始化**：
   - Scheduler 引擎（dag-scheduler-core）
   - Agent 执行器（dag-agent-core，本地直接执行，不走 HTTP）
   - Admin 管理逻辑（dag-admin-core）
4. **HTTP 服务启动**（MuServer）：
   - 注册 scheduler API 路由
   - 注册 agent API 路由
   - 注册 admin API 路由
   - 注册静态资源路由（前端页面）
5. **自检与就绪**：输出访问地址和状态信息

### 7.2 Agent 本地执行模式

在 all-in-one 模式下，agent 不走 HTTP 与 scheduler 通信，而是直接调用本地 API：

```java
// AllInOne 模式下，Agent 使用 LocalSchedulerClient 替代 HTTP Client
public class LocalSchedulerClient implements AgentSchedulerClient {
    private final SchedulerEngine schedulerEngine;
    // 直接内存调用，无网络开销
}
```

这样既保留了 agent 的完整逻辑，又避免了在单进程内走 HTTP 的性能损耗。

---

## 8. 安全与边界考量

### 8.1 默认安全策略

- all-in-one 模式默认**关闭 JWT 认证**（单机场景下无意义）
- 提供配置开关，可手动启用认证（若需暴露到外部网络）
- 默认监听 `127.0.0.1`，防止未授权外部访问

### 8.2 数据持久化边界

- H2 内存模式：进程退出数据丢失，适合开发和演示
- H2 文件模式：数据持久化到本地文件，适合测试
- RocksDB：数据持久化到本地文件，适合高性能单机场景
- PostgreSQL：连接外部数据库，适合轻量生产

### 8.3 资源隔离

- Scheduler、Agent、Admin 虽在同一进程，但各自拥有独立的线程池
- Agent 的任务执行线程与 Scheduler 的调度线程隔离，避免相互阻塞

---

## 9. 构建与打包

### 9.1 Maven 构建流程

```bash
# 全量构建（包含前端打包）
mvn clean install -pl dag-task-allinone -am

# dag-task-allinone 输出：
# target/dag-task-allinone-1.0.0.jar（fat jar，包含所有依赖和前端资源）
```

### 9.2 前端资源嵌入

`dag-task-allinone/pom.xml` 中配置：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-web-resources</id>
            <phase>generate-resources</phase>
            <goals><goal>copy-resources</goal></goals>
            <configuration>
                <outputDirectory>${project.build.outputDirectory}/web</outputDirectory>
                <resources>
                    <resource>
                        <directory>${project.basedir}/../dag-admin-web/dist</directory>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 9.3 运行方式

```bash
# 直接运行
java -jar dag-task-allinone-1.0.0.jar

# 指定配置
java -jar dag-task-allinone-1.0.0.jar --config=/path/to/allinone.yml
```

---

## 10. 测试策略

### 10.1 Repository 实现测试

- `dag-repository-jdbi`：使用 H2 内存数据库运行全套集成测试
- `dag-repository-rocksdb`：使用临时 RocksDB 目录运行全套集成测试
- 两类测试共享同一套测试用例（通过抽象基类），确保行为一致性

### 10.2 all-in-one 端到端测试

- 启动完整的 all-in-one 服务
- 通过 API 创建 Workflow -> 触发执行 -> 验证结果
- 验证前端页面可正常访问

---

## 11. 后续扩展（预留）

### 11.1 图数据库支持

- 当 DAG 规模扩大、依赖分析复杂化时，可新增 `dag-repository-graph`
- 基于通用 Repository 接口，将 DAG 结构映射为图模型（节点=Task，边=Dependency）
- 图查询（上游依赖、下游影响、环检测）可直接利用图数据库能力

### 11.2 配置化组件开关

```yaml
# 未来可支持的组件开关
components:
  scheduler: true
  agent: true
  admin: true
  web: true
```

---

## 12. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| 现有 JDBI DAO 迁移工作量大 | 进度延迟 | 分阶段迁移，先迁移核心表，再迁移辅助表 |
| H2 与 PostgreSQL 语法差异 | 部分 Flyway 脚本不兼容 | 迁移时修复不兼容语句，CI 中同时跑 H2 和 PostgreSQL 测试 |
| RocksDB 实现复杂度高 | 初期不可用 | 第一阶段只交付 jdbi-h2，RocksDB 作为第二阶段 |
| 前端静态资源体积大 | jar 包膨胀 | 前端启用 gzip + tree-shaking，MuServer 支持静态资源压缩传输 |

---

*设计文档版本：v0.1*
*创建日期：2026-05-19*
*状态：待用户审阅*
