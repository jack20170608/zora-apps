# dag-agent-cli 设计文档

## 背景与目标

`dag-agent-cli` 是一个命令行工具模块，用于**快速验证各种不同的 `TaskExecution` 实现**。它依赖 `dag-agent` 模块，每次只能从命令行触发**单个 task** 的执行。

## 设计原则

- **复用 `TaskExecutionEngine` 完整流程**：不走绕过路线，完整走 `pendingQueue → executorService.submit() → executeTask()` 流程，复用结果上报和日志链路。
- **可选上报**：默认本地执行（NoOp 模式），传入 `--server-url` 则向 scheduler server 注册并上报结果。
- **不依赖 MuServer**：CLI 是纯命令行工具，不需要 HTTP server。
- **轻量参数**：全部通过命令行参数传入，不需要配置文件。

## 架构

```
dag-agent-cli 命令行入口
       │
       ▼
  DagAgentCli (main class)
       │
       ├── 解析参数（picocli）
       │
       ├── 构造 AgentConfiguration
       │   ├── agentId = 指定或自动生成
       │   ├── dagServerUrl = --server-url 或空
       │   └── agentUrl = 空（CLI 不需要 HTTP）
       │
       ├── 选择 AgentSchedulerClient
       │   ├── --server-url 指定 → DefaultAgentSchedulerClient
       │   └── 未指定 → NoOpAgentSchedulerClient
       │
       ├── 构造 TaskExecutionEngine
       │   └── engine.start()
       │
       ├── engine.submitAndWait(taskId, executionClass, input, reportResult, timeout)
       │   └── 阻塞等待执行完成
       │
       ├── 输出结果到控制台
       │
       └── engine.stop() → JVM 退出
```

## 对 dag-agent 模块的扩展

### 1. TaskExecutionEngine.submitAndWait()

在 `TaskExecutionEngine` 中新增同步等待执行能力：

```java
public TaskExecuteResult submitAndWait(
    Long taskId,
    String executionClass,
    String inputJson,
    boolean reportResult,
    long timeoutMs
) throws TimeoutException, InterruptedException
```

**实现机制**：
- 调用现有的 `submit()` 将任务加入 `pendingQueue`
- 注册 `CompletableFuture<TaskExecuteResult>` 与 `taskId` 关联（使用 `ConcurrentHashMap<Long, CompletableFuture<TaskExecuteResult>>`）
- 后台 `queueProcessor` 执行 `executeTask()` 完毕后，在 `finishTask()` 之前查找并 complete 对应的 wait future
- `submitAndWait()` 阻塞等待 future，支持 `timeoutMs` 超时
- 超时后调用 `kill(taskId)` 并抛出 `TimeoutException`

**为什么这样设计**：
- 完整走引擎的执行流程（pending → running → finished）
- 结果上报链路完整复用（`resultReportQueue → resultReporterThread → AgentSchedulerClient`）
- 对现有代码改动最小，只在 `executeTask()` 末尾增加 wait future 通知逻辑

### 2. NoOpAgentSchedulerClient

新建类：

```java
public class NoOpAgentSchedulerClient implements AgentSchedulerClient
```

实现：
- `register()` → 返回 200 OK mock Response，记录 `LOGGER.info()`
- `unregister()` → 返回 200 OK mock Response
- `reportTaskResult()` → 记录日志并返回 200 OK，**不上报真实 server**

用途：`--server-url` 未指定时，CLI 使用此 client 避免连接真实 server。

## dag-agent-cli 模块结构

```
dag-agent-cli/
├── pom.xml                           # Maven 构建文件
├── metadata/
│   └── metadata.json                 # 模块元数据
├── README.md                         # 模块说明文档
├── src/main/java/top/ilovemyhome/dagtask/agent/cli/
│   ├── DagAgentCli.java              # 主入口类，含 main 方法
│   ├── CliArguments.java             # picocli 参数定义
│   └── ConsoleTaskLogWriter.java     # 控制台日志输出
└── src/test/java/top/ilovemyhome/dagtask/agent/cli/
    └── DagAgentCliTest.java          # 单元测试
```

## 命令行参数

| 参数 | 缩写 | 必需 | 默认值 | 说明 |
|---|---|---|---|---|
| `--execution` | `-e` | 是 | - | TaskExecution 实现类全限定名 |
| `--input` | `-i` | 否 | `{}` | 输入 JSON |
| `--server-url` | `-s` | 否 | - | Scheduler server URL，不传则 NoOp 模式 |
| `--agent-id` | `-a` | 否 | 自动生成 | Agent ID |
| `--task-log-dir` | `-l` | 否 | - | 任务日志目录 |
| `--timeout` | `-t` | 否 | 300000 | 超时时间（毫秒）|
| `--help` | `-h` | 否 | - | 显示帮助信息 |

## 输出格式

**成功：**
```
Task execution completed:
  Task ID: 1
  Status: SUCCESS
  Output: {"exitCode":0,"stdout":"hello\n","stderr":"","timedOut":false}
  Duration: 120ms
```

**失败：**
```
Task execution failed:
  Task ID: 1
  Status: FAILED
  Message: script is required and must not be blank
  Duration: 5ms
```

## 使用示例

**本地验证 BashTaskExecution：**
```bash
java -jar dag-agent-cli.jar \
  -e top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution \
  -i '{"script":"echo hello"}'
```

**带 server 上报验证：**
```bash
java -jar dag-agent-cli.jar \
  -e top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution \
  -i '{"script":"echo hello"}' \
  -s http://localhost:8080
```

## 依赖关系

```
dag-agent-cli
    ├── dag-agent（provided scope，复用 TaskExecutionEngine 和 NoOpAgentSchedulerClient）
    ├── dag-si
    ├── zora-common（picocli 从 zora-bom 引入）
    └── slf4j + logback
```

## 安全考虑

- `--input` 参数中的 JSON 内容应仅为测试数据，不应包含敏感信息
- `TaskFactory.createTaskForExecution()` 使用 `Class.forName()`，仅支持白名单中的 execution 类
- 超时后 `kill()` 会中断运行中的任务，避免资源泄漏
- CLI 执行完毕后 `engine.stop()` 确保线程池和后台线程正确关闭

## 边界条件与异常场景

| 场景 | 行为 |
|---|---|
| execution class 不存在 | `TaskFactory` 抛 `IllegalArgumentException`，CLI 捕获并输出错误信息 |
| input JSON 格式错误 | `TaskInput.of()` 抛异常，CLI 捕获并输出 |
| 执行超时 | `submitAndWait()` 抛 `TimeoutException`，CLI 调用 `kill()` 后输出超时信息 |
| engine 启动失败 | 输出错误信息并退出（非 0 状态码）|
| server-url 指定但连接失败 | `DefaultAgentSchedulerClient` 抛异常，CLI 捕获并输出 |
