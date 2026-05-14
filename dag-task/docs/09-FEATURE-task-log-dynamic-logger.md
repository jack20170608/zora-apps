# Feature: 动态 Logger 任务日志

## 背景

当前 dag-agent 维护了一套独立的 `TaskLogWriter` 体系（接口 + FileTaskLogWriter + TaskLogContext + TaskLogAppender），用于将每个任务的日志隔离到独立文件。这套体系存在以下问题：

1. **重复造轮子**：SLF4J/Logback 已经提供了完善的日志框架，`TaskLogWriter` 属于重复实现
2. **日志重复写入**：`BashTaskExecution` 既直接调用 `logWriter.info()`，又通过 SLF4J logger 写日志，导致同一条内容出现两次
3. **维护成本高**：需要维护 ThreadLocal 绑定、自定义 Appender、文件写入等

## 目标

- 完全移除 `TaskLogWriter` 体系，让任务日志回归 SLF4J/Logback 原生机制
- 每个任务执行时动态创建一个绑定到专属日志文件的 Logger
- 基类负责 Logger 的生命周期（创建 → 执行 → 清理）
- Bash stdout/stderr 通过 Logger 统一写入任务日志文件
- 资源在任务结束时（成功/失败/异常）必定释放

## 设计

### 1. 删除的文件

| 文件 | 原因 |
|------|------|
| `dag-si/src/main/java/.../TaskLogWriter.java` | 自定义日志接口，不再需要 |
| `dag-agent/src/main/java/.../log/FileTaskLogWriter.java` | 文件写入实现，被 Logback FileAppender 替代 |
| `dag-agent/src/main/java/.../log/TaskLogContext.java` | ThreadLocal 绑定器，不再需要 |
| `dag-agent/src/main/java/.../log/TaskLogAppender.java` | Logback Appender，不再需要 |
| `dag-agent/src/test/java/.../log/FileTaskLogWriterTest.java` | 对应的测试 |
| `dag-agent/src/test/java/.../log/TaskLogAppenderTest.java` | 对应的测试 |
| `dag-agent/src/test/java/.../log/InMemoryTaskLogWriter.java` | 测试辅助类 |

### 2. 修改的接口

`TaskExecution.execute()` 签名从：
```java
TaskOutput execute(TaskInput input, TaskLogWriter logWriter);
```
改为：
```java
TaskOutput execute(TaskInput input);
```

### 3. 新增抽象基类

`AbstractTaskExecution`（位于 dag-agent）：

```
execute(TaskInput input)
  ├── 从 input.attributes() 读取 taskLogDir
  ├── setupLogger(taskId, taskLogDir)
  │     ├── 获取 LoggerContext
  │     ├── 以 task-{taskId} 为 name 获取/复用 Logger
  │     ├── 清理旧 appender (detachAndStopAllAppenders)
  │     ├── 创建 FileAppender + PatternLayoutEncoder
  │     ├── 绑定到 Logger
  │     └── setAdditive(false) + setLevel(INFO)
  ├── doExecute(input)  // 子类实现
  └── teardownLogger()
        └── detachAndStopAllAppenders()
```

**Logger 命名策略**：使用固定前缀 `task.{taskId}`，每次执行前 `detachAndStopAllAppenders()` 清理旧的，执行后再清理。这样同一个 taskId 重试时复用同一个 Logger 对象，不会导致 LoggerContext 内存泄漏。

**日志文件命名**：`{taskLogDir}/{taskId}.log`。当前引擎保证同一个 agent 上不会并发执行相同 taskId，所以不会有并发写冲突。

**taskLogDir 传递**：`TaskExecutionEngine.executeTask()` 在创建 `TaskInput` 时，将 `config.getTaskLogDir()` 放入 `attributes` Map 中。

### 4. BashTaskExecution 改造

`StreamGobbler` 不再持有 `TaskLogWriter`，改为持有 `Logger`：
```java
// stdout
logger.info("[STDOUT] {}", line);
// stderr
logger.error("[STDERR] {}", line);
```

Java 代码中的日志调用（如 `logger.info("Starting bash execution...")`）继续通过 `this.logger`（即动态创建的 task logger）写入同一个文件。

### 5. TaskExecutionEngine 改造

`executeTask()` 中移除所有 `TaskLogWriter` 相关逻辑：
- 不再创建 `FileTaskLogWriter`
- 不再调用 `TaskLogContext.set()` / `clear()`
- 将 `taskLogDir` 放入 `TaskInput.attributes()`

### 6. 日志格式

默认格式：`%d{yyyy-MM-dd HH:mm:ss.SSS} [%level] %msg%n`

可通过 `input.attributes().get("taskLogPattern")` 覆盖，如果不存在则使用默认格式。

## 安全与边界条件

1. **无 taskLogDir**：如果 `attributes` 中没有 `taskLogDir`，基类不创建 FileAppender，子类的 `this.logger` 就是普通 logger（可能输出到控制台或继承 root 配置）
2. **异常时资源释放**：`setupLogger()` 和 `teardownLogger()` 包裹在 try-finally 中，无论 `doExecute()` 成功、失败还是抛异常，Logger 的 appender 都会被停止和分离
3. **并发安全**：当前引擎通过 `pendingTaskIds` + `runningTasks` 保证同一个 agent 上不会并发执行相同 taskId，所以同一个 Logger 不会被两个线程同时操作
4. **空 attributes**：`TaskInput.attributes()` 可能为 null，基类需要做 null-safe 处理
