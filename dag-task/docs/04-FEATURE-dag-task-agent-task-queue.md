# 04. dag-task-agent 任务队列设计与实现

## 概述

dag-task-agent 是基于 DAG 任务调度系统的执行节点，负责接收 dag-server 分发的任务并执行，执行完成后将结果报告回 dag-server。

本文档描述了 dag-task-agent 的任务队列执行模型设计，包含三个任务队列（pending/running/finished）和带容量限制的任务调度机制。

## 架构设计

### 整体流程

```
┌─────────────┐     ┌─────────────────────────────────────┐
│  dag-server │────▶│  POST /api/submit                    │
└─────────────┘     └─────────────────────────────────────┘
                          │
                          ▼
                   ┌──────────────┐ 容量检查
                   │  pendingQueue │────▶ 满了 → 429 拒绝
                   └──────────────┘
                          │
                          ▼
               ┌──────────────────────┐
               │  后台队列处理器线程    │ 自动取出任务
               └──────────────────────┘
                          │
                          ▼
                   ┌──────────────┐
                   │ runningTasks  │ 提交给线程池执行
                   └──────────────┘
                          │
                          ▼
                  执行完成/失败/被杀死
                          │
                          ▼
                   ┌──────────────┐ 报告结果给 server
                   │ finishedTasks│ 保存执行历史
                   └──────────────┘
```

### 三个队列说明

| 队列 | 数据结构 | 作用 | 容量限制 |
|------|----------|------|----------|
| **pendingQueue** | `ArrayBlockingQueue<PendingTask>` | 保存等待执行的任务，当线程池忙的时候，任务在这里排队等待 | 有界，由 `maxPendingTasks` 配置，默认 100 |
| **runningTasks** | `ConcurrentHashMap<Long, RunningTask>` | 保存当前正在执行的任务，包含任务信息和 Future 对象用于取消 | 当前大小等于正在执行的任务数，最大等于 `maxConcurrentTasks` |
| **finishedTasks** | `ConcurrentLinkedQueue<FinishedTask>` | 保存所有已经完成的任务（成功、失败、被杀死、强制成功）的执行历史 | 无界，随进程生命周期 |

### 任务状态流转

```
┌─────────┐
│ 提交任务 │
└────┬────┘
     │
     ▼
┌────────────┐  pending 满 → 拒绝
│  PENDING   │
└────┬───────┘
     │
     ▼
┌────────────┐
│  RUNNING   │
└────┬───────┘
     │
     ▼
┌────────────┐
│  FINISHED   │
└────────────┘
```

特殊流转：
- **kill**: PENDING → 直接移除 → FINISHED (cancelled)
- **kill**: RUNNING → cancel future → 移除 → FINISHED (cancelled)
- **force-ok**: PENDING → 直接移除 → 报告成功 → FINISHED (success, forced)
- **force-ok**: RUNNING → cancel future → 报告成功 → FINISHED (success, forced)

## Agent 注册到 Dag-Server

启动时，如果 `autoRegister` 为 `true`，Agent 会自动发送注册请求到 dag-server 的 `/api/agent/register` 端点。

### 注册请求体

```json
{
  "agentId": "agent-01",
  "agentUrl": "http://localhost:8080",
  "maxConcurrentTasks": 4,
  "maxPendingTasks": 100,
  "supportedExecutionKeys": ["com.example.TaskA", "com.example.TaskB"]
}
```

**字段说明**：
| 字段 | 说明 |
|------|------|
| `agentId` | Agent ID |
| `agentUrl` | Agent 访问地址 |
| `maxConcurrentTasks` | 最大并发执行任务数 |
| `maxPendingTasks` | 等待队列最大容量 |
| `supportedExecutionKeys` | 该 Agent 能够执行的任务 executionKey 列表（任务名称）|

dag-server 可以根据这些信息进行任务路由，将特定任务分发到支持它的 Agent 上执行。

## 配置说明

### 配置参数

可以通过系统属性或者环境变量配置以下参数：

| 参数 | 系统属性 | 环境变量 | 默认值 | 说明 |
|------|----------|----------|--------|------|
| `port` | `agent.port` | `AGENT_PORT` | `8080` | Agent 监听端口 |
| `host` | `agent.host` | `AGENT_HOST` | `localhost` | Agent 主机名 |
| `dagServerUrl` | `agent.dagServerUrl` | `AGENT_DAG_SERVER_URL` | 必填 | Dag 服务器地址 |
| `agentId` | `agent.agentId` | `AGENT_ID` | 主机名 | Agent ID，用于注册 |
| `autoRegister` | `agent.autoRegister` | `AGENT_AUTO_REGISTER` | `true` | 是否自动注册到 dag-server |
| `maxConcurrentTasks` | `agent.maxConcurrentTasks` | `AGENT_MAX_CONCURRENT_TASKS` | `4` | 最大并发执行任务数（线程池大小） |
| `maxPendingTasks` | `agent.maxPendingTasks` | `AGENT_MAX_PENDING_TASKS` | `100` | 等待队列最大容量 |
| `supportedExecutionKeys` | `agent.supportedExecutionKeys` | `AGENT_SUPPORTED_EXECUTION_KEYS` | 空 | 该 Agent 能够执行的任务 executionKey 列表，逗号分隔。例如: `com.example.TaskA,com.example.TaskB` |

### 启动示例

```bash
# 基本启动
java -jar dag-task-agent-1.0.0.jar \
  -Dagent.dagServerUrl=http://localhost:8080 \
  -Dagent.agentId=agent-01

# 自定义并发和队列大小，指定支持的任务
java -jar dag-task-agent-1.0.0.jar \
  -Dagent.port=8081 \
  -Dagent.dagServerUrl=http://dag-server:8080 \
  -Dagent.agentId=agent-01 \
  -Dagent.maxConcurrentTasks=8 \
  -Dagent.maxPendingTasks=200 \
  -Dagent.supportedExecutionKeys=top.ilovemyhome.dagtask.core.TestTaskExecution,com.example.MyCustomTask
```

## API 接口

### `GET /api/ping`
心跳检测

**响应**：
```
pong
```

---

### `GET /api/health`
获取 Agent 健康状态和队列统计

**响应示例**：
```json
{
  "running": true,
  "agentId": "agent-01",
  "port": 8080,
  "dagServerUrl": "http://localhost:8080",
  "maxConcurrentTasks": 4,
  "maxPendingTasks": 100,
  "supportedExecutionKeysCount": 2,
  "supportedExecutionKeys": ["com.example.TaskA", "com.example.TaskB"],
  "pendingSize": 5,
  "runningSize": 4,
  "finishedSize": 128
}
```

**字段说明**：
| 字段 | 说明 |
|------|------|
| `running` | Agent 是否正在运行 |
| `agentId` | Agent ID |
| `maxConcurrentTasks` | 配置的最大并发数 |
| `maxPendingTasks` | 配置的等待队列最大容量 |
| `supportedExecutionKeysCount` | 支持的任务执行类型数量 |
| `supportedExecutionKeys` | 支持的任务 executionKey 列表（该 Agent 能够执行的任务名称） |
| `pendingSize` | 当前等待队列中的任务数 |
| `runningSize` | 当前正在执行的任务数 |
| `finishedSize` | 已完成任务总数 |

---

### `POST /api/submit`
提交任务执行请求

**请求体**：
```json
{
  "taskId": 123,
  "executionClass": "com.example.MyTask",
  "input": "{\"param\": \"value\"}"
}
```

**成功响应 (202 Accepted)**：
```json
{
  "success": true,
  "message": "Task 123 accepted for execution",
  "taskId": 123,
  "pendingPosition": 5
}
```

**队列满响应 (429 Too Many Requests)**：
```json
{
  "error": "Pending queue is full",
  "capacity": 100,
  "currentSize": 100
}
```

**错误响应 (400 Bad Request)**：参数验证失败

---

### `POST /api/kill/{taskId}`
杀死正在执行或者等待中的任务

**成功响应**：
```json
{
  "success": true,
  "message": "Task 123 killed successfully"
}
```

**404 响应**：任务不存在于 pending 或 running 队列

---

### `POST /api/force-ok/{taskId}`
强制任务成功，不管当前是什么状态

**成功响应**：
```json
{
  "success": true,
  "message": "Task 123 marked as successful (removed from running)"
}
```

**404 响应**：任务不存在于 pending 或 running 队列

## 设计要点

### 1. 背压 (Backpressure)
当 pending 队列达到最大容量时，新的提交会被立刻拒绝，返回 429 状态码，避免 agent 被过多请求压垮。

### 2. 并发安全
所有队列都使用并发集合：
- `ArrayBlockingQueue` - 天生线程安全的阻塞队列
- `ConcurrentHashMap` - 线程安全的哈希表
- `ConcurrentLinkedQueue` - 线程安全的链表

### 3. 自动调度
后台守护线程 `queueProcessor` 自动从 pending 队列取出任务并提交给线程池执行，不需要外部调度。

### 4. 固定大小线程池
使用 `Executors.newFixedThreadPool(maxConcurrentTasks)` 创建固定大小线程池，控制最大并发数，避免资源耗尽。

### 5. 支持对 pending 任务操作
kill 和 force-ok 都支持对还没开始执行的 pending 任务进行操作，避免不必要的执行。

## 架构分层与实现类

遵循单一职责原则，任务管理和 REST API 分离：

| 类 | 说明 | 职责 |
|----|------|------|
| `AgentConfiguration` | 配置持有类 | 保存所有配置参数，包括 `supportedExecutionKeys`、`maxConcurrentTasks`、`maxPendingTasks` |
| `DagTaskAgent` | 主入口 | 启动 MuServer HTTP 服务器，初始化所有组件 |
| `TaskExecutionManager` | 核心任务生命周期管理器 | 管理三个任务队列 (pending/running/finished)，后台调度执行，处理 submit/kill/forceOk |
| `TaskAgentResource` | REST API 资源 | HTTP 端点处理，委托业务逻辑给 `TaskExecutionManager` |
| `DagServerClient` | Dag 服务器客户端 | 注册和结果报告 |

## 总结

这个设计满足了以下需求：
1. ✓ 三个任务队列：pending/running/finished
2. ✓ 后台自动调度，空闲线程执行 pending 任务
3. ✓ 队列满时拒绝 server 请求，实现背压
4. ✓ health 端点报告完整队列统计
5. ✓ 支持 kill 和 force-ok 操作 pending 和 running 任务
6. ✓ 注册时向 dag-server 报告该 Agent 能够执行的任务 executionKey 列表，方便 server 进行任务分发
