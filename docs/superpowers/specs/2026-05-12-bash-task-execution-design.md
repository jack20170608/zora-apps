# BashTaskExecution Design

## Overview

A `TaskExecution` implementation for `dag-task-agent` that executes bash scripts as tasks.
The script content and parameters are provided via `TaskInput.input` as a JSON object.

## Input Format

The `TaskInput.input` field must contain a JSON object matching the following structure:

```json
{
  "script": "echo $MY_VAR",
  "timeoutSeconds": 300,
  "workingDirectory": "/tmp",
  "env": {"MY_VAR": "hello"},
  "shell": "bash"
}
```

### Fields

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `script` | Yes | — | The bash script content to execute |
| `timeoutSeconds` | No | `300` | Maximum execution time in seconds |
| `workingDirectory` | No | `null` | Working directory for the process |
| `env` | No | `{}` | Additional environment variables to inject |
| `shell` | No | `"bash"` | Shell interpreter to use |

The shell command is constructed as: `shell -c script`

## Output Format

The `TaskOutput.output` field contains a `Result` record:

```java
record Result(
    int exitCode,
    String stdout,
    String stderr,
    boolean timedOut
) {}
```

| Field | Description |
|-------|-------------|
| `exitCode` | Process exit code, or -1 if unobtainable |
| `stdout` | Standard output content |
| `stderr` | Standard error content |
| `timedOut` | Whether the process was terminated due to timeout |

`TaskOutput.isSuccess` is `true` only when `exitCode == 0` and `timedOut == false`.

## Execution Flow

1. Deserialize `TaskInput.input` into `Param` via `JacksonUtil`
2. Validate parameters (script must be non-empty, timeoutSeconds > 0)
3. Construct `ProcessBuilder` with the command: `[shell, "-c", script]`
4. If `workingDirectory` is specified, set it on the `ProcessBuilder`
5. If `env` is specified, inject variables into the process environment
6. Start the process
7. Launch two independent threads to asynchronously read `stdout` and `stderr`
8. Main thread calls `process.waitFor(timeout, TimeUnit.SECONDS)`
9. If timeout occurs:
   - Call `destroy()` (SIGTERM)
   - Wait up to 5 seconds for graceful shutdown
   - If still alive, call `destroyForcibly()` (SIGKILL)
   - Set `timedOut = true`
10. Wait for stdout/stderr reader threads to complete
11. Assemble `Result` and return `TaskOutput`

## Error Handling

| Scenario | Behavior |
|----------|----------|
| `script` is null/blank | Throw `IllegalArgumentException`, return `TaskOutput.fail` |
| `timeoutSeconds` <= 0 | Same as above |
| Process fails to start | Catch exception, return `TaskOutput.fail` with message |
| Process exits non-zero | Return `TaskOutput.fail`, include exit code in `Result` |
| Process times out | `timedOut = true`, return `TaskOutput.fail` with captured output |
| Output read interrupted | Exception message appended to corresponding output field |

## Process Deadlock Prevention

A single `Process` has two output streams (`stdout` and `stderr`) with fixed-size OS buffers.
If the parent process only reads `stdout` while the child fills `stderr`, the child blocks on a full `stderr` buffer and never terminates — causing deadlock.

To prevent this, both streams are read by **independent daemon threads** running concurrently with the process.

## Timeout Handling

The timeout mechanism uses `process.waitFor(timeout, TimeUnit.SECONDS)`. If the timeout expires:

1. `destroy()` sends a termination signal (SIGTERM on Unix)
2. Wait up to 5 seconds for the process to exit voluntarily
3. If still alive after the grace period, `destroyForcibly()` sends SIGKILL
4. Reader threads are allowed to drain remaining output

This provides a balance between allowing cleanup (SIGTERM) and ensuring termination (SIGKILL).

## Files

| File | Path |
|------|------|
| Implementation | `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/execution/BashTaskExecution.java` |
| Unit Tests | `dag-task/dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/execution/BashTaskExecutionTest.java` |

## Testing Plan

| Test Case | Description |
|-----------|-------------|
| `testBasicExecution` | Execute `echo hello`, verify exitCode=0, stdout contains "hello" |
| `testTimeout` | Execute `sleep 10` with timeout=1s, verify timedOut=true |
| `testNonZeroExitCode` | Execute `exit 1`, verify isSuccess=false, exitCode=1 |
| `testWorkingDirectory` | Execute `pwd` with workingDirectory=/tmp, verify stdout contains /tmp |
| `testEnvVariables` | Execute `echo $VAR` with env={"VAR":"hello"}, verify stdout contains "hello" |
| `testInvalidParam` | Missing script field, verify IllegalArgumentException |

## Security Notes (Future Iteration)

The following security enhancements are **not** included in this version and should be added in a future iteration:

- `workingDirectory` whitelist (restrict to allowed paths)
- Environment variable blacklist (prevent overriding PATH, HOME, etc.)
- Script content length limit
- Dangerous command pattern detection
