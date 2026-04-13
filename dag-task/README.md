# dag-task

一个高性能的分布式DAG（有向无环图）任务调度系统，支持复杂工作流编排、分布式执行和完整的可观测性。

## 📋 项目概述

**dag-task** 是一个企业级的分布式任务调度系统，采用DAG模型描述任务依赖关系，支持：

- ✅ 复杂工作流定义（DAG图模型）
- ✅ 分布式任务执行（Server + 多Agent架构）
- ✅ 灵活的任务执行模式（同步/异步）
- ✅ 完善的任务生命周期管理
- ✅ 数据库驱动的任务调度（高效、一致性强）
- ✅ 可视化Web管理界面
- ✅ 完整的API接口
- ✅ 失败重试和死信队列机制

**适用场景**：工作流引擎、ETL数据处理、任务编排、批处理系统等。

---

## 🏗️ 架构设计

### 系统架构

```
┌──────────────────────┐
│   Web UI (Next.js)   │  ← 可视化管理界面
├──────────────────────┤
│  Scheduler Server    │  ← 调度中心（8081）
│ (dag-scheduler)      │
├──────────────────────┤
│  Agent Executor      │  ← 任务执行器（8080）
│ (dag-agent)          │
├──────────────────────┤
│  PostgreSQL Database │  ← 持久化存储
└──────────────────────┘
```

### DAG工作流流程

```
DAG定义 → 任务依赖验证 → 就绪任务查询 → 分发执行 → 结果回调 → 后继任务就绪 → 递进执行
```

---

## 📦 模块结构

项目采用分层模块化架构，共6个后端模块 + 1个前端模块：

### 核心模块

| 模块 | 类型 | 职责 | 说明 |
|------|------|------|------|
| **dag-si** | 领域模型层 | 定义所有核心领域对象和接口 | 不包含实现逻辑，纯接口定义 |
| **dag-scheduler** | 核心实现层 | 任务调度、DAG验证、执行引擎 | 数据库驱动的DAG调度算法 |
| **dag-scheduler-muserver** | HTTP服务层 | REST API服务、MuServer嵌入 | 调度中心服务端 |
| **dag-scheduler-web** | 前端UI | 可视化DAG构建、监控 | Next.js 14 + Tailwind CSS |
| **dag-agent** | Agent核心层 | 任务执行、队列管理、结果上报 | 三队列模式（pending/running/finished） |
| **dag-agent-muserver** | HTTP服务层 | Agent REST API、MuServer嵌入 | 任务执行器服务端 |

### 详细模块说明

#### 1. dag-si（领域模型层）
- **职责**：定义所有核心抽象和接口
- **主要类**：
  - `Task<I, O>` - 抽象任务基类
  - `TaskContext<I, O>` - 任务执行上下文
  - `TaskRecord`, `TaskOrder` - 持久化模型
  - `TaskDagService<I, O>` - 核心服务接口
  - `TaskStatus`, `OrderType` - 枚举类型
- **依赖**：zora-common, jackson, slf4j
- **特点**：独立的领域模型层，易于扩展和测试

#### 2. dag-scheduler（核心调度引擎）
- **职责**：DAG构建、验证、任务调度和执行引擎
- **核心功能**：
  - DAG循环检测（在线验证）
  - 数据库驱动的就绪检查（SQL优化）
  - Caffeine缓存管理（LRU + 自动过期）
  - 线程池任务执行（固定大小 + 有界队列）
  - 支持SyncTask（同步）和AsyncTask（异步）
  - 完善的超时处理和错误处理
- **依赖**：dag-si, zora-common, zora-jdbi
- **特点**：SQL-first设计，避免内存溢出，高效的DAG管理

#### 3. dag-scheduler-muserver（调度服务）
- **职责**：基于MuServer的HTTP服务器
- **功能**：
  - 任务管理API（创建、更新、查询）
  - DAG工作流部署API
  - 调度触发API
  - Agent管理API
  - OpenAPI/Swagger文档
- **配置**：HOCON格式配置文件

#### 4. dag-scheduler-web（Web UI）
- **技术栈**：Next.js 14, TypeScript, Tailwind CSS, React
- **功能**：
  - DAG可视化构建器（拖拽构建工作流）
  - 任务模板管理
  - 实时任务监控
  - Dashboard概览
  - API集成

#### 5. dag-agent（Agent核心）
- **职责**：任务执行引擎和队列管理
- **三队列设计**：
  - `pendingQueue` - 待执行任务队列（容量：100）
  - `runningTasks` - 正在执行的任务（并发数可配）
  - `finishedTasks` - 完成历史（无界）
- **特性**：
  - 自动注册和心跳保活
  - 容量控制（超出返回429）
  - 死信队列机制（失败自动重试）
  - 优雅关闭（等待运行任务完成）
  - 支持任务杀死/取消/强制成功

#### 6. dag-agent-muserver（Agent服务）
- **职责**：Agent的HTTP服务器
- **功能**：
  - 任务提交接口
  - 任务查询接口
  - 任务管理接口（kill/cancel/forceOk）
  - 健康检查接口
  - OpenAPI文档

---

## 🛠️ 技术栈详解

| 技术 | 版本 | 用途 |
|------|------|------|
| **JDK** | 25 | 开发语言 |
| **Maven** | 3.x | 构建工具 |
| **Zora Framework** | 1.0.2 | 企业开发框架 |
| **Zora JDBI** | 1.0.2 | SQL-first数据库访问 |
| **Caffeine** | - | LRU缓存（DAG缓存） |
| **Jackson** | - | JSON序列化/反序列化 |
| **SnakeYAML** | 2.3 | YAML配置文件支持 |
| **MuServer** | - | 轻量级HTTP服务器 |
| **Next.js** | 14 | React框架 |
| **Tailwind CSS** | - | 原子化CSS框架 |
| **JUnit 5** | - | 单元测试框架 |
| **Mockito** | - | Mock测试库 |
| **SLF4J + Logback** | - | 日志框架 |
| **embedded-postgres** | - | 测试数据库 |

---

## 🚀 快速开始

### 前置条件

- JDK 25+
- Maven 3.8+
- PostgreSQL 14+ （生产环境）

### 1. 构建整个项目

```bash
cd dag-task
mvn clean install -DskipTests
```

### 2. 启动调度中心

```bash
cd dag-scheduler-muserver
mvn clean package

# 运行（端口8081）
java -Denv=local -cp "target/classes:target/lib/*" \
  top.ilovemyhome.dagtask.core.muserver.starter.SchedulerMain
```

### 3. 启动任务执行器

```bash
cd dag-agent-muserver
mvn clean package

# 运行（端口8080）
java -Denv=local -cp "target/classes:target/lib/*" \
  top.ilovemyhome.dagtask.agent.muserver.starter.AgentMain
```

### 4. 启动Web前端

```bash
cd dag-scheduler-web
npm install
npm run dev
```

访问 `http://localhost:3000` 进行管理。

---

## 📖 API文档

### 核心API端点

#### 调度中心 (Scheduler Server)

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/v1/agent/register` | Agent注册 |
| POST | `/api/v1/agent/unregister` | Agent注销 |
| POST | `/api/v1/agent/task/result` | 上报任务结果 |
| POST | `/api/v1/agent/status` | 上报Agent状态 |
| POST | `/api/v1/tasks` | 创建任务 |
| GET | `/api/v1/tasks/{id}` | 查询任务状态 |

#### 执行器 (Agent Server)

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/v1/tasks/submit` | 提交任务执行 |
| GET | `/api/v1/tasks/{id}` | 查询任务状态 |
| DELETE | `/api/v1/tasks/{id}` | 取消/杀死任务 |
| POST | `/api/v1/tasks/{id}/forceOk` | 强制成功 |
| GET | `/api/v1/status` | Agent健康检查 |

### OpenAPI文档

- Scheduler: `http://localhost:8081/api.html` (Swagger UI)
- Agent: `http://localhost:8080/api.html` (Swagger UI)

---

## ⚙️ 配置说明

### 调度中心配置 (scheduler.conf)

```hocon
# MuServer HTTP配置
muserver {
  port = 8081
  host = "0.0.0.0"
  idleTimeout = 30000
}

# 调度中心配置
scheduler {
  # DAG缓存配置
  dagCache {
    maxSize = 10000
    expireAfterWriteMinutes = 60
  }
  
  # 线程池配置
  threadPool {
    coreSize = 4
    maxSize = 16
    queueCapacity = 1024
  }
  
  # 数据库配置
  database {
    url = "jdbc:postgresql://localhost:5432/dag_task"
    user = "postgres"
    password = "password"
  }
}
```

### 执行器配置 (agent.conf)

```hocon
# MuServer HTTP配置
muserver {
  port = 8080
  host = "0.0.0.0"
}

# Agent配置
dag-agent {
  agentId = "agent-001"
  agentUrl = "http://localhost:8080"
  dagServerUrl = "http://localhost:8081"
  autoRegister = true
  
  # 队列容量
  maxConcurrentTasks = 4
  maxPendingTasks = 100
  
  # 死信队列持久化目录（每个失败批次一个文件，读写分离）
  deadLetterPersistencePath = "./dead-letter"
  
  # 支持的执行类型
  supportedExecutionKeys = [
    "top.ilovemyhome.dagtask.core.DefaultTaskExecution"
  ]
}
```

---

## 📊 任务生命周期

### 任务状态转移

```
INIT
  ↓
RUNNING (by Scheduler)
  ↓
┌─────────┬──────────┬─────────┬────────┐
│         │          │         │        │
SUCCESS  ERROR    TIMEOUT   SKIPPED   UNKNOWN
```

### 工作流执行流程

1. **任务创建** - 通过API创建任务记录和DAG依赖关系
2. **DAG验证** - 系统自动检查是否存在循环依赖
3. **就绪检查** - 查询所有依赖任务已完成的任务
4. **任务分发** - 调度中心将就绪任务分发给Agent
5. **任务执行** - Agent执行任务，管理lifecycle
6. **结果上报** - 执行完成后上报结果给调度中心
7. **后继触发** - 调度中心自动触发依赖该任务的后继任务

---

## 🔄 高级特性

### 1. 死信队列机制

Agent会自动持久化失败的结果上报，并在后台定期重试：

```java
// 自动重试配置
// 目录模式（推荐）- 每个失败批次一个文件，读写分离
deadLetterPersistencePath = "./dead-letter"
// 兼容旧配置 - 单个文件模式
// deadLetterPersistenceFile = "./dead-letter.jsonl"
retryInterval = 30000  // 30秒重试一次
```

### 2. 三队列并发模型

```
提交 → pendingQueue (有界) → runningTasks (并发) → 报告 → finishedTasks
         ↑                      ↓
         └──── 容量控制 (429) ──┘
```

### 3. 灵活的任务执行模式

```java
// 同步任务 - 立即返回结果
class SyncTask extends Task {
  execute() { return result; }
}

// 异步任务 - 等待外部事件
class AsyncTask extends Task {
  async execute() { 
    startAsync();
    await externalEvent();
    return result;
  }
}
```

### 4. 支持任务操作

- **cancel** - 取消待执行任务
- **kill** - 强制停止运行任务
- **forceOk** - 强制标记为成功

---

## 📁 项目结构

```
dag-task/
├── dag-si/                   # 领域模型层
│   ├── src/main/java/
│   │   └── enums/           # TaskStatus, OrderType等
│   └── pom.xml
├── dag-scheduler/            # 核心调度引擎
│   ├── src/main/java/
│   └── pom.xml
├── dag-scheduler-muserver/   # 调度中心HTTP服务
│   ├── src/main/java/
│   └── pom.xml
├── dag-scheduler-web/        # Web管理界面
│   ├── app/
│   ├── components/
│   ├── lib/
│   └── package.json
├── dag-agent/                # Agent核心
│   ├── src/main/java/
│   │   ├── agent/core/
│   │   │   └── TaskExecutionEngine.java  # 队列和执行管理
│   │   └── agent/config/
│   └── pom.xml
├── dag-agent-muserver/       # Agent HTTP服务
│   ├── src/main/java/
│   └── pom.xml
├── docs/                     # 架构文档
│   ├── 01-ARCHITECTURE-initial-setup.md
│   ├── 02-ARCHITECTURE-analysis-and-improvements.md
│   ├── 03-ARCHITECTURE-db-driven-dag.md
│   ├── 04-FEATURE-dag-task-agent-task-queue.md
│   └── 05-FEATURE-task-status-transition.md
├── api-test/                 # API测试脚本
└── pom.xml                   # 父POM
```

---

## 🧪 测试

### 运行单元测试

```bash
mvn clean test
```

### 运行集成测试

```bash
mvn clean verify
```

### API测试

项目在 `api-test/` 目录提供了API测试脚本：
- `01_Agent_Registry_Api_Test.http` - Agent注册API测试
- `02_Task_Template_Api_Test.http` - 任务模板API测试

使用 IntelliJ IDEA 或 VS Code REST Client 插件可直接运行这些脚本。

---

## 📚 文档

详细的架构和设计文档在 `docs/` 目录：

- **01-ARCHITECTURE-initial-setup.md** - 项目初始架构设计
- **02-ARCHITECTURE-analysis-and-improvements.md** - 架构分析和8项改进建议
- **03-ARCHITECTURE-db-driven-dag.md** - DAG数据库驱动优化
- **04-FEATURE-dag-task-agent-task-queue.md** - Agent三队列设计详解
- **05-FEATURE-task-status-transition.md** - 任务状态转移规则

---

## 🔧 开发指南

### 创建自定义Task执行器

```java
public class MyCustomTask implements TaskExecution {
  @Override
  public TaskOutput execute(TaskInput input) throws Exception {
    // 你的业务逻辑
    return TaskOutput.success(result);
  }
}

// 在配置中注册
supportedExecutionKeys = ["com.example.MyCustomTask"]
```

### 添加自定义API端点

在 `dag-scheduler-muserver` 或 `dag-agent-muserver` 中添加新的REST Resource类，遵循JAX-RS规范。

---

## 🎯 最佳实践

1. **DAG设计**
   - 避免过大的DAG（>1000节点），考虑分解为子工作流
   - 使用有意义的任务ID，便于追踪和调试
   - 为关键任务设置合理的超时时间

2. **Agent部署**
   - 使用多个Agent实例实现并发执行
   - 监控Agent心跳和队列深度
   - 定期检查死信队列，及时处理失败

3. **性能优化**
   - 调整线程池大小匹配硬件和任务特性
   - 监控缓存命中率，优化DAG缓存大小
   - 使用连接池复用数据库连接

4. **故障处理**
   - 定期重放死信队列中的失败报告
   - 设置合理的重试策略
   - 监控系统日志，及时发现异常

---

## 📝 版本说明

- **当前版本**：1.0.1-SNAPSHOT
- **语言版本**：JDK 25
- **状态**：Active Development

---

## 🤝 贡献指南

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

---

## 📄 License

Copyright © 2026 zora-apps

本项目采用开源协议，详见LICENSE文件。

---

## 📞 获取帮助

- 📖 查看文档：`docs/` 目录
- 🐛 报告Issue：通过项目Issue功能
- 💬 讨论设计：通过Discussion功能

---

## 🗺️ 路线图

- [x] DAG任务调度核心功能
- [x] 分布式Agent执行
- [x] 死信队列机制
- [ ] 可视化监控仪表板增强
- [ ] 条件分支执行支持
- [ ] 任务失败自动重试
- [ ] 与Spring Cloud集成
- [ ] Kubernetes部署支持
- [ ] 性能监控和告警集成

