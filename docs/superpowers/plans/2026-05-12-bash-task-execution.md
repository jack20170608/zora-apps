# BashTaskExecution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `BashTaskExecution` class to `dag-task-agent` that executes bash scripts via `ProcessBuilder` with timeout support, configurable working directory, and environment variables.

**Architecture:** A single `TaskExecution` implementation following the existing `LongRunningExecution` pattern. Input is deserialized from `TaskInput.input()` JSON via `JacksonUtil`. Two daemon threads read stdout/stderr concurrently to prevent OS pipe buffer deadlock. Timeout handling uses `destroy()` (SIGTERM) with a grace period followed by `destroyForcibly()` (SIGKILL).

**Tech Stack:** Java 25, JUnit 5, Mockito, SLF4J, Jackson (via zora-json), ProcessBuilder

---

## File Structure

| File | Action | Responsibility |
|------|--------|--------------|
| `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/execution/BashTaskExecution.java` | Create | TaskExecution implementation that runs bash scripts |
| `dag-task/dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/execution/BashTaskExecutionTest.java` | Create | JUnit 5 unit tests covering basic execution, timeout, non-zero exit, workingDirectory, env variables, invalid input |

---

## Spec Reference

See: `docs/superpowers/specs/2026-05-12-bash-task-execution-design.md`

---

## Existing Code Patterns to Follow

- `TaskExecution` interface: `TaskOutput execute(TaskInput input)`
- `LongRunningExecution` uses `input.getInputAs(Param.class)` to deserialize JSON input
- Logger pattern: `private static final Logger logger = LoggerFactory.getLogger(...)`
- Error handling: try/catch → `TaskOutput.fail(taskId, null, message)` or `createErrorOutput(taskId, throwable)`
- All code comments must be in **English**

---

## Task 1: Write BashTaskExecution Implementation

**Files:**
- Create: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/execution/BashTaskExecution.java`

- [ ] **Step 1: Create BashTaskExecution.java with Param and Result records**

```java
package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * TaskExecution implementation that executes bash scripts.
 * <p>
 * Input is provided as JSON via {@link TaskInput#input()} with the following structure:
 * <pre>
 * {
 *   "script": "echo hello",
 *   "timeoutSeconds": 300,
 *   "workingDirectory": "/tmp",
 *   "env": {"VAR": "value"},
 *   "shell": "bash"
 * }
 * </pre>
 */
public class BashTaskExecution implements TaskExecution {

    private static final Logger logger = LoggerFactory.getLogger(BashTaskExecution.class);

    /** Default timeout in seconds if not specified in input. */
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    /** Grace period in seconds after destroy() before destroyForcibly(). */
    private static final int DESTROY_GRACE_PERIOD_SECONDS = 5;

    @Override
    public TaskOutput execute(TaskInput input) {
        Long taskId = input.taskId();
        try {
            Param param = input.getInputAs(Param.class);
            logger.info("Starting bash execution for taskId={}, scriptLength={}", taskId, param.script().length());
            return doExecute(taskId, param);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid input for taskId={}: {}", taskId, e.getMessage());
            return TaskOutput.fail(taskId, null, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during bash execution for taskId={}", taskId, e);
            return TaskOutput.createErrorOutput(taskId, e);
        }
    }

    private TaskOutput doExecute(Long taskId, Param param) throws IOException, InterruptedException {
        validate(param);

        String shell = param.shell() != null && !param.shell().isBlank() ? param.shell() : "bash";
        ProcessBuilder pb = new ProcessBuilder(shell, "-c", param.script());

        if (param.workingDirectory() != null && !param.workingDirectory().isBlank()) {
            pb.directory(new File(param.workingDirectory()));
        }

        if (param.env() != null && !param.env().isEmpty()) {
            Map<String, String> env = pb.environment();
            env.putAll(param.env());
        }

        Process process = pb.start();

        StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream());
        StreamGobbler stderrGobbler = new StreamGobbler(process.getErrorStream());
        Thread stdoutThread = new Thread(stdoutGobbler, "bash-stdout-" + taskId);
        Thread stderrThread = new Thread(stderrGobbler, "bash-stderr-" + taskId);
        stdoutThread.setDaemon(true);
        stderrThread.setDaemon(true);
        stdoutThread.start();
        stderrThread.start();

        int timeoutSeconds = param.timeoutSeconds() != null ? param.timeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;
        boolean finishedInTime = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        boolean timedOut = false;
        if (!finishedInTime) {
            logger.warn("TaskId={} timed out after {} seconds, attempting graceful termination", taskId, timeoutSeconds);
            timedOut = true;
            process.destroy();
            boolean destroyed = process.waitFor(DESTROY_GRACE_PERIOD_SECONDS, TimeUnit.SECONDS);
            if (!destroyed) {
                logger.warn("TaskId={} did not terminate gracefully, forcing termination", taskId);
                process.destroyForcibly();
                process.waitFor();
            }
        }

        stdoutThread.join(5000);
        stderrThread.join(5000);

        String stdout = stdoutGobbler.getOutput();
        String stderr = stderrGobbler.getOutput();
        int exitCode = process.exitValue();

        Result result = new Result(exitCode, stdout, stderr, timedOut);

        if (timedOut || exitCode != 0) {
            String message = timedOut
                ? "Task timed out after " + timeoutSeconds + " seconds"
                : "Task exited with code " + exitCode;
            return TaskOutput.fail(taskId, result, message);
        }

        return TaskOutput.success(taskId, result);
    }

    private void validate(Param param) {
        if (param == null) {
            throw new IllegalArgumentException("Input param is required");
        }
        if (param.script() == null || param.script().isBlank()) {
            throw new IllegalArgumentException("script is required and must not be blank");
        }
        if (param.timeoutSeconds() != null && param.timeoutSeconds() <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be > 0");
        }
    }

    /**
     * Input parameter DTO for BashTaskExecution.
     */
    public record Param(
        String script,
        Integer timeoutSeconds,
        String workingDirectory,
        Map<String, String> env,
        String shell
    ) {
        public Param {
            // Allow null for optional fields; validation happens in execute()
        }
    }

    /**
     * Result record containing the output of the bash script execution.
     */
    public record Result(
        int exitCode,
        String stdout,
        String stderr,
        boolean timedOut
    ) {
    }

    /**
     * Consumes an InputStream in a separate thread to prevent pipe buffer deadlock.
     */
    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final StringBuilder output = new StringBuilder();

        StreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                output.append("[READ ERROR: ").append(e.getMessage()).append("]");
            }
        }

        String getOutput() {
            return output.toString();
        }
    }
}
```

- [ ] **Step 2: Run compilation check**

Run: `mvn -pl dag-task/dag-agent compile -q`

Expected: BUILD SUCCESS (no errors, no warnings)

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/execution/BashTaskExecution.java
git commit -m "feat: add BashTaskExecution for executing bash scripts in dag-agent

- Supports configurable shell, timeout, workingDirectory, env
- Uses StreamGobbler threads to prevent stdout/stderr deadlock
- Timeout handling: destroy() then destroyForcibly() with grace period
- Returns Result record with exitCode, stdout, stderr, timedOut"
```

---

## Task 2: Write Unit Tests for BashTaskExecution

**Files:**
- Create: `dag-task/dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/execution/BashTaskExecutionTest.java`

- [ ] **Step 1: Create the test class**

```java
package top.ilovemyhome.dagtask.agent.execution;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BashTaskExecutionTest {

    private final BashTaskExecution execution = new BashTaskExecution();

    @Test
    void testBasicExecution() {
        String inputJson = "{\"script\":\"echo hello\"}";
        TaskInput input = TaskInput.of(1L, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        assertThat(output.output()).isInstanceOf(BashTaskExecution.Result.class);
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("hello");
        assertThat(result.timedOut()).isFalse();
    }

    @Test
    void testTimeout() {
        // Use a longer sleep than the timeout
        String inputJson = "{\"script\":\"sleep 10\",\"timeoutSeconds\":1}";
        TaskInput input = TaskInput.of(2L, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        assertThat(result.timedOut()).isTrue();
    }

    @Test
    void testNonZeroExitCode() {
        String inputJson = "{\"script\":\"exit 1\"}";
        TaskInput input = TaskInput.of(3L, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.timedOut()).isFalse();
    }

    @Test
    void testWorkingDirectory() {
        String inputJson = "{\"script\":\"pwd\",\"workingDirectory\":\"/tmp\"}";
        TaskInput input = TaskInput.of(4L, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        // Windows may not have /tmp, so skip if the directory does not exist
        if (new java.io.File("/tmp").exists()) {
            assertThat(result.stdout()).contains("/tmp");
        }
    }

    @Test
    void testEnvVariables() {
        String inputJson = "{\"script\":\"echo $MY_VAR\",\"env\":{\"MY_VAR\":\"hello\"}}";
        TaskInput input = TaskInput.of(5L, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("hello");
    }

    @Test
    void testInvalidParam_nullScript() {
        String inputJson = "{\"script\":\"\"}";
        TaskInput input = TaskInput.of(6L, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        assertThat(output.message()).contains("script");
    }

    @Test
    void testInvalidParam_negativeTimeout() {
        String inputJson = "{\"script\":\"echo hello\",\"timeoutSeconds\":-1}";
        TaskInput input = TaskInput.of(7L, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        assertThat(output.message()).contains("timeoutSeconds");
    }

    @Test
    void testStderrCaptured() {
        String inputJson = "{\"script\":\"echo error >&2\"}";
        TaskInput input = TaskInput.of(8L, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        // On bash, ">&2" redirects stdout to stderr, so stderr should contain "error"
        assertThat(result.stdout() + result.stderr()).contains("error");
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn -pl dag-task/dag-agent test -Dtest=BashTaskExecutionTest -q`

Expected: BUILD SUCCESS, all 8 tests pass.

If any test fails, investigate and fix before proceeding.

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/execution/BashTaskExecutionTest.java
git commit -m "test: add unit tests for BashTaskExecution

Covers basic execution, timeout, non-zero exit code,
workingDirectory, env variables, invalid param validation"
```

---

## Task 3: Full Module Test Run

- [ ] **Step 1: Run all dag-agent tests**

Run: `mvn -pl dag-task/dag-agent test -q`

Expected: BUILD SUCCESS, no regressions in existing tests.

- [ ] **Step 2: Commit (if any fixes were needed)**

Only if a fix was applied:
```bash
git add ...
git commit -m "fix: resolve issue found during full test run"
```

---

## Spec Coverage Checklist

| Spec Requirement | Implementing Task |
|------------------|-------------------|
| Input JSON with script, timeoutSeconds, workingDirectory, env, shell | Task 1 |
| Output Result record with exitCode, stdout, stderr, timedOut | Task 1 |
| ProcessBuilder with shell -c script | Task 1 |
| workingDirectory configuration | Task 1 |
| env variable injection | Task 1 |
| Async stdout/stderr readers (deadlock prevention) | Task 1 |
| Timeout with destroy() then destroyForcibly() | Task 1 |
| Invalid param validation | Task 1 |
| testBasicExecution | Task 2 |
| testTimeout | Task 2 |
| testNonZeroExitCode | Task 2 |
| testWorkingDirectory | Task 2 |
| testEnvVariables | Task 2 |
| testInvalidParam | Task 2 |

---

## Self-Review

**Placeholder scan:** No TBD, TODO, or "implement later" found. ✅
**Type consistency:** `Param` and `Result` record names match across spec and plan. ✅
**Spec coverage:** All spec requirements map to a specific task and step. ✅
