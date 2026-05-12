# Per-Task Logging Design

## Overview

Add per-task execution log files to `dag-agent`. Each task gets its own log file under a configurable directory. The log captures both the task execution's internal log messages and the raw stdout/stderr of bash scripts.

## Architecture

The solution extends the `TaskExecution` interface with a new overloaded method that accepts a `TaskLogWriter`, while keeping backward compatibility via a default method delegation. Only tasks that need per-task logging (e.g. `BashTaskExecution`) override the new method. Simple tasks continue using the legacy single-argument method and produce no per-task log files.

## Components

### 1. TaskLogWriter Interface (dag-si)

A new interface in the domain model:

```java
public interface TaskLogWriter {
    void info(String message);
    void warn(String message);
    void error(String message);
    void stdout(String message);
    void stderr(String message);
}
```

- `info`/`warn`/`error` — for execution class internal messages
- `stdout`/`stderr` — for raw subprocess output, prefixed with `[STDOUT]` / `[STDERR]` in the file

### 2. TaskExecution Interface Extension (dag-si)

Add a default method to `TaskExecution`:

```java
default TaskOutput execute(TaskInput input, TaskLogWriter logWriter) {
    return execute(input);
}
```

Existing implementations (`EchoExecution`, `LongRunningExecution`) are unaffected.

### 3. FileTaskLogWriter (dag-agent)

Implementation of `TaskLogWriter` that writes to a file:

- File path: `<taskLogDir>/<taskId>.log`
- Each line prefixed with ISO timestamp and level/source
- Uses `BufferedWriter` with periodic `flush()`
- Auto-creates parent directories
- Thread-safe (synchronized on writer)

Log line format:
```
2026-05-12T14:30:00.123 [INFO] Starting bash execution for taskId=1
[STDOUT] hello
[STDOUT] world
[STDERR] error message
```

### 4. AgentConfiguration Extension (dag-agent)

New field:

```java
private String taskLogDir = "";
```

- Empty string (default) disables per-task logging
- Non-empty path enables per-task logging; directory auto-created
- Builder pattern updated accordingly

### 5. TaskExecutionEngine Integration (dag-agent)

In `executeTask(PendingTask)`:

1. Check if `taskLogDir` is configured
2. If configured, create `FileTaskLogWriter(taskId, taskLogDir)`
3. Call `execution.execute(input, logWriter)` (if writer created) or `execution.execute(input)` (fallback)
4. After execution, close the writer

### 6. BashTaskExecution Integration (dag-agent)

Override the new two-argument method:

1. Log internal messages via `logWriter.info()`/`warn()`/`error()`
2. `StreamGobbler` writes each line to both `logWriter.stdout()` / `logWriter.stderr()` and the SLF4J logger
3. Keep SLF4J logger for the agent's global console log

## Error Handling

| Scenario | Behavior |
|----------|----------|
| `taskLogDir` directory creation fails | Log error via SLF4J, fall back to non-file execution (no per-task log) |
| File write fails mid-execution | Log error via SLF4J, continue execution |
| Writer close fails | Log warning via SLF4J, do not affect task result |

## Testing Plan

| Test | Description |
|------|-------------|
| `FileTaskLogWriterTest.testWriteAndRead` | Verify file created and content matches |
| `FileTaskLogWriterTest.testDirectoryAutoCreate` | Verify parent directories created |
| `BashTaskExecutionTest.testTaskLogFileCreated` | Execute task with log dir set, verify file exists and contains stdout |

## Files

| File | Action |
|------|--------|
| `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/TaskLogWriter.java` | Create |
| `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/TaskExecution.java` | Modify |
| `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/log/FileTaskLogWriter.java` | Create |
| `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/config/AgentConfiguration.java` | Modify |
| `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/TaskExecutionEngine.java` | Modify |
| `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/execution/BashTaskExecution.java` | Modify |
| `dag-task/dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/log/FileTaskLogWriterTest.java` | Create |
