# dag-agent-cli Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create `dag-agent-cli` module that triggers a single task execution from command line, reusing `TaskExecutionEngine` with optional scheduler server reporting.

**Architecture:** Extend `TaskExecutionEngine` with `submitAndWait()` using `CompletableFuture`. Add `NoOpAgentSchedulerClient` for local mode. New `dag-agent-cli` module uses picocli for argument parsing and runs one task synchronously.

**Tech Stack:** JDK 25, Maven, JUnit 5, Mockito, picocli (from zora-bom), SLF4J

---

## File Structure

### Modified (dag-agent)
- `dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/TaskExecutionEngine.java` — add `submitAndWait()` and wait-future notification

### New (dag-agent)
- `dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/client/NoOpAgentSchedulerClient.java` — no-op client for local mode
- `dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/core/TaskExecutionEngineWaitTest.java` — tests for submitAndWait

### New (dag-agent-cli)
- `dag-agent-cli/pom.xml` — module pom
- `dag-agent-cli/metadata/metadata.json` — module metadata
- `dag-agent-cli/src/main/java/top/ilovemyhome/dagtask/agent/cli/DagAgentCli.java` — main entry
- `dag-agent-cli/src/main/java/top/ilovemyhome/dagtask/agent/cli/CliArguments.java` — picocli arguments
- `dag-agent-cli/src/main/java/top/ilovemyhome/dagtask/agent/cli/ConsoleTaskLogWriter.java` — console log writer
- `dag-agent-cli/src/test/java/top/ilovemyhome/dagtask/agent/cli/DagAgentCliTest.java` — CLI tests
- `dag-agent-cli/README.md` — module documentation

### Modified (dag-task parent)
- `dag-task/pom.xml` — add `dag-agent-cli` to modules list and dependencyManagement

---

### Task 1: Extend TaskExecutionEngine with submitAndWait

**Files:**
- Modify: `dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/TaskExecutionEngine.java`

- [ ] **Step 1: Add waitFutures field and submitAndWait method**

Add at the top of `TaskExecutionEngine` class, after existing fields:

```java
private final ConcurrentHashMap<Long, CompletableFuture<TaskExecuteResult>> waitFutures = new ConcurrentHashMap<>();
```

Add the `submitAndWait` method after the existing `submit` method (after line ~480):

```java
/**
 * Submits a task for execution and blocks until it completes or times out.
 * Reuses the existing submit() and queue processing pipeline.
 *
 * @param taskId         the task ID
 * @param executionClass the execution class name to instantiate
 * @param inputJson      the input JSON (can be null)
 * @param reportResult   whether to report the result to the scheduler server
 * @param timeoutMs      maximum time to wait in milliseconds
 * @return the task execution result
 * @throws TimeoutException     if the task does not complete within the timeout
 * @throws InterruptedException if the wait is interrupted
 */
public TaskExecuteResult submitAndWait(Long taskId, String executionClass, String inputJson,
                                       boolean reportResult, long timeoutMs)
        throws TimeoutException, InterruptedException {
    CompletableFuture<TaskExecuteResult> future = new CompletableFuture<>();
    waitFutures.put(taskId, future);
    try {
        SubmissionResult submissionResult = submit(taskId, executionClass, inputJson, reportResult);
        if (!submissionResult.accepted()) {
            TaskExecuteResult failedResult = new TaskExecuteResult(
                config.getAgentId(), taskId, false,
                "Submission rejected: " + submissionResult.message(), Instant.now()
            );
            future.complete(failedResult);
            return future.get();
        }
        return future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    } catch (java.util.concurrent.ExecutionException e) {
        throw new RuntimeException("Unexpected error during task execution wait", e.getCause());
    } catch (java.util.concurrent.TimeoutException e) {
        kill(taskId);
        throw new TimeoutException("Task " + taskId + " timed out after " + timeoutMs + "ms");
    } finally {
        waitFutures.remove(taskId);
    }
}
```

Add import if missing:
```java
import java.util.concurrent.CompletableFuture;
import java.time.Instant;
```

- [ ] **Step 2: Add notifyWaitFuture helper method**

Add a private helper method near `finishTask`:

```java
private void notifyWaitFuture(Long taskId, TaskExecuteResult result) {
    CompletableFuture<TaskExecuteResult> future = waitFutures.remove(taskId);
    if (future != null && !future.isDone()) {
        future.complete(result);
    }
}
```

- [ ] **Step 3: Modify executeTask to notify wait futures**

Refactor `executeTask` method to construct `TaskExecuteResult` in both success and failure paths, then notify the wait future in `finally`:

Replace the entire `executeTask` method body with:

```java
private void executeTask(PendingTask pendingTask) {
    Long taskId = pendingTask.taskId();
    TaskExecution execution = pendingTask.execution();
    TaskInput input = pendingTask.input();
    boolean reportResult = pendingTask.reportResult();
    long startTime = System.currentTimeMillis();

    TaskLogWriter logWriter = null;
    String taskLogDir = config.getTaskLogDir();
    if (taskLogDir != null && !taskLogDir.isBlank()) {
        try {
            logWriter = new FileTaskLogWriter(taskId, taskLogDir);
        } catch (Exception e) {
            logger.warn("Failed to create task log writer for taskId={}, taskLogDir={}, continuing without per-task log", taskId, taskLogDir, e);
        }
    }

    TaskExecuteResult result = null;
    try {
        logger.info("Starting execution of task {}", taskId);
        TaskOutput output = execution.execute(input, logWriter);
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Completed execution of task {} in {}ms", taskId, duration);
        result = new TaskExecuteResult(
            config.getAgentId(), taskId, output.isSuccess(), serializeOutput(output), null
        );
        if (reportResult) {
            if (!resultReportQueue.offer(result)) {
                persistToDeadLetterFile(List.of(result));
            }
        }
        finishTask(taskId, true, output.isSuccess(), duration);
    } catch (Exception e) {
        long duration = System.currentTimeMillis() - startTime;
        logger.error("Task {} execution failed after {}ms", taskId, duration, e);
        result = new TaskExecuteResult(config.getAgentId(), taskId, false, e.getMessage());
        if (reportResult) {
            if (!resultReportQueue.offer(result)) {
                persistToDeadLetterFile(List.of(result));
            }
        }
        finishTask(taskId, false, false, duration);
    } finally {
        if (logWriter != null) {
            logWriter.close();
        }
        notifyWaitFuture(taskId, result);
    }
}
```

Note: the `report` variable is replaced with `result` directly, since `TaskExecuteResult` is the same type needed for both reporting and wait future notification.

- [ ] **Step 4: Verify dag-agent compiles**

Run:
```bash
mvn compile -pl dag-agent -am
```

Expected: BUILD SUCCESS

---

### Task 2: Create NoOpAgentSchedulerClient

**Files:**
- Create: `dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/client/NoOpAgentSchedulerClient.java`

- [ ] **Step 1: Write the NoOpAgentSchedulerClient class**

```java
package top.ilovemyhome.dagtask.agent.client;

import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;

import java.util.List;

/**
 * No-op implementation of {@link AgentSchedulerClient} for local CLI execution mode.
 * Logs operations but does not communicate with any real scheduling server.
 */
public class NoOpAgentSchedulerClient implements AgentSchedulerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpAgentSchedulerClient.class);

    @Override
    public Response register(AgentRegisterRequest registration) {
        LOGGER.info("NoOp register for agent {}", registration.agentId());
        return Response.ok().build();
    }

    @Override
    public Response unregister(AgentUnregistration unregistration) {
        LOGGER.info("NoOp unregister for agent {}", unregistration.agentId());
        return Response.ok().build();
    }

    @Override
    public Response reportTaskResult(List<TaskExecuteResult> results) {
        LOGGER.debug("NoOp reportTaskResult for {} tasks", results.size());
        return Response.ok().build();
    }
}
```

- [ ] **Step 2: Verify dag-agent compiles**

Run:
```bash
mvn compile -pl dag-agent -am
```

Expected: BUILD SUCCESS

---

### Task 3: Write unit tests for submitAndWait

**Files:**
- Create: `dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/core/TaskExecutionEngineWaitTest.java`

- [ ] **Step 1: Write the test class**

```java
package top.ilovemyhome.dagtask.agent.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.ilovemyhome.dagtask.agent.client.NoOpAgentSchedulerClient;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.agent.execution.EchoExecution;
import top.ilovemyhome.dagtask.agent.execution.LongRunningExecution;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskExecutionEngineWaitTest {

    @Mock
    private AgentConfiguration config;

    private ExecutorService executorService;
    private TaskExecutionEngine engine;
    private AgentSchedulerClient client;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        client = new NoOpAgentSchedulerClient();

        when(config.getAgentId()).thenReturn("test-agent");
        when(config.getMaxPendingTasks()).thenReturn(10);
        when(config.getMaxConcurrentTasks()).thenReturn(2);
        when(config.getTaskLogDir()).thenReturn("");
        when(config.getSupportedExecutionKeys()).thenReturn(List.of("echo", "long-running"));
        when(config.getDeadLetterPersistencePath()).thenReturn("");

        engine = new TaskExecutionEngine(config, client, executorService, new com.fasterxml.jackson.databind.ObjectMapper());
        engine.start();
    }

    @AfterEach
    void tearDown() {
        engine.stop();
        executorService.shutdown();
    }

    @Test
    void shouldReturnResult_WhenTaskExecutesSuccessfully() throws Exception {
        // Given
        String executionClass = EchoExecution.class.getName();
        String inputJson = "{\"message\":\"hello\"}";

        // When
        TaskExecuteResult result = engine.submitAndWait(1L, executionClass, inputJson, false, 30000);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.taskId()).isEqualTo(1L);
        assertThat(result.agentId()).isEqualTo("test-agent");
    }

    @Test
    void shouldThrowTimeout_WhenTaskExceedsTimeout() {
        // Given
        String executionClass = LongRunningExecution.class.getName();
        String inputJson = "{\"durationSeconds\":10}";

        // When / Then
        assertThatThrownBy(() ->
            engine.submitAndWait(2L, executionClass, inputJson, false, 100)
        ).isInstanceOf(TimeoutException.class)
         .hasMessageContaining("timed out");
    }

    @Test
    void shouldReturnFailedResult_WhenExecutionClassNotFound() throws Exception {
        // Given
        String executionClass = "nonexistent.ClassName";

        // When
        TaskExecuteResult result = engine.submitAndWait(3L, executionClass, "{}", false, 30000);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isFalse();
        assertThat(result.output()).contains("rejected");
    }
}
```

- [ ] **Step 2: Run the tests**

Run:
```bash
mvn test -pl dag-agent -Dtest=TaskExecutionEngineWaitTest
```

Expected: 3 tests PASS

---

### Task 4: Create dag-agent-cli module structure

**Files:**
- Create: `dag-agent-cli/pom.xml`
- Create: `dag-agent-cli/metadata/metadata.json`
- Create directories: `dag-agent-cli/src/main/java/top/ilovemyhome/dagtask/agent/cli/`, `dag-agent-cli/src/test/java/top/ilovemyhome/dagtask/agent/cli/`, `dag-agent-cli/src/main/resources/`, `dag-agent-cli/src/test/resources/`

- [ ] **Step 1: Write dag-agent-cli/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>top.ilovemyhome.dagtask</groupId>
        <artifactId>dag-task</artifactId>
        <version>${revision}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <modelVersion>4.0.0</modelVersion>
    <artifactId>dag-agent-cli</artifactId>
    <name>dag-agent-cli - DAG Task Agent CLI</name>
    <description>Command-line tool for triggering single task execution to quickly verify TaskExecution implementations</description>

    <dependencies>
        <!-- DAG Agent Core -->
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-agent</artifactId>
        </dependency>
        <!-- DAG SI -->
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-si</artifactId>
        </dependency>

        <!-- picocli for CLI argument parsing -->
        <dependency>
            <groupId>info.picocli</groupId>
            <artifactId>picocli</artifactId>
        </dependency>

        <!-- zora common -->
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-common</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-json</artifactId>
        </dependency>

        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- SLF4J -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>metadata</directory>
                <filtering>true</filtering>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
            </resource>
        </resources>
        <testResources>
            <testResource>
                <directory>src/test/resources</directory>
            </testResource>
        </testResources>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Write metadata/metadata.json**

```json
{
  "groupId": "@project.groupId@",
  "artifactId": "@project.artifactId@",
  "description": "@project.description@",
  "version": "@project.version@",
  "scmUrl": "@project.scmUrl@"
}
```

- [ ] **Step 3: Create directories**

```bash
mkdir -p dag-agent-cli/src/main/java/top/ilovemyhome/dagtask/agent/cli/
mkdir -p dag-agent-cli/src/test/java/top/ilovemyhome/dagtask/agent/cli/
mkdir -p dag-agent-cli/src/main/resources/
mkdir -p dag-agent-cli/src/test/resources/
```

---

### Task 5: Implement ConsoleTaskLogWriter

**Files:**
- Create: `dag-agent-cli/src/main/java/top/ilovemyhome/dagtask/agent/cli/ConsoleTaskLogWriter.java`

- [ ] **Step 1: Write ConsoleTaskLogWriter**

```java
package top.ilovemyhome.dagtask.agent.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskLogWriter;

/**
 * {@link TaskLogWriter} implementation that writes task execution logs
 * to the console (via SLF4J logger) for CLI usage.
 */
public class ConsoleTaskLogWriter implements TaskLogWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleTaskLogWriter.class);

    @Override
    public void info(String message) {
        LOGGER.info("[TASK] {}", message);
    }

    @Override
    public void warn(String message) {
        LOGGER.warn("[TASK] {}", message);
    }

    @Override
    public void error(String message) {
        LOGGER.error("[TASK] {}", message);
    }

    @Override
    public void stdout(String message) {
        LOGGER.info("[STDOUT] {}", message);
    }

    @Override
    public void stderr(String message) {
        LOGGER.error("[STDERR] {}", message);
    }

    @Override
    public void close() {
        // No resources to release for console output
    }
}
```

---

### Task 6: Implement CliArguments (picocli)

**Files:**
- Create: `dag-agent-cli/src/main/java/top/ilovemyhome/dagtask/agent/cli/CliArguments.java`

- [ ] **Step 1: Write CliArguments**

```java
package top.ilovemyhome.dagtask.agent.cli;

import picocli.CommandLine;

/**
 * Command-line arguments for dag-agent-cli using picocli.
 */
@CommandLine.Command(
    name = "dag-agent-cli",
    description = "Trigger a single task execution from command line",
    mixinStandardHelpOptions = true,
    version = "1.0.0"
)
public class CliArguments implements Runnable {

    @CommandLine.Option(
        names = {"-e", "--execution"},
        description = "TaskExecution implementation class full qualified name",
        required = true
    )
    private String executionClass;

    @CommandLine.Option(
        names = {"-i", "--input"},
        description = "Input JSON for the task",
        defaultValue = "{}"
    )
    private String inputJson;

    @CommandLine.Option(
        names = {"-s", "--server-url"},
        description = "Scheduler server URL (if not set, runs in local NoOp mode)"
    )
    private String serverUrl;

    @CommandLine.Option(
        names = {"-a", "--agent-id"},
        description = "Agent ID (auto-generated if not set)"
    )
    private String agentId;

    @CommandLine.Option(
        names = {"-l", "--task-log-dir"},
        description = "Task log directory"
    )
    private String taskLogDir;

    @CommandLine.Option(
        names = {"-t", "--timeout"},
        description = "Timeout in milliseconds",
        defaultValue = "300000"
    )
    private long timeoutMs;

    public String getExecutionClass() {
        return executionClass;
    }

    public String getInputJson() {
        return inputJson;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getTaskLogDir() {
        return taskLogDir;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public void run() {
        // Execution logic is in DagAgentCli.main(), not here
    }
}
```

---

### Task 7: Implement DagAgentCli main entry

**Files:**
- Create: `dag-agent-cli/src/main/java/top/ilovemyhome/dagtask/agent/cli/DagAgentCli.java`

- [ ] **Step 1: Write DagAgentCli**

```java
package top.ilovemyhome.dagtask.agent.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import top.ilovemyhome.dagtask.agent.client.DefaultAgentSchedulerClient;
import top.ilovemyhome.dagtask.agent.client.NoOpAgentSchedulerClient;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.agent.core.TaskExecutionEngine;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * Command-line entry point for triggering a single task execution.
 * Supports both local NoOp mode and scheduler server reporting mode.
 */
public class DagAgentCli {

    private static final Logger LOGGER = LoggerFactory.getLogger(DagAgentCli.class);

    public static void main(String[] args) {
        CliArguments arguments = new CliArguments();
        CommandLine cmd = new CommandLine(arguments);
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            LOGGER.error("Execution failed: {}", ex.getMessage(), ex);
            System.err.println("Execution failed: " + ex.getMessage());
            return 1;
        });

        int exitCode = cmd.execute(args);
        if (exitCode != 0 || cmd.isUsageHelpRequested() || cmd.isVersionHelpRequested()) {
            System.exit(exitCode);
        }

        exitCode = execute(arguments);
        System.exit(exitCode);
    }

    static int execute(CliArguments args) {
        String agentId = args.getAgentId() != null ? args.getAgentId() : "cli-" + UUID.randomUUID();
        String serverUrl = args.getServerUrl();
        boolean reportResult = serverUrl != null && !serverUrl.isBlank();

        AgentConfiguration config = buildConfiguration(agentId, serverUrl, args.getTaskLogDir());
        AgentSchedulerClient client = buildClient(config, serverUrl);
        ObjectMapper objectMapper = new ObjectMapper();

        var executor = Executors.newFixedThreadPool(1);
        TaskExecutionEngine engine = new TaskExecutionEngine(config, client, executor, objectMapper);

        long taskId = 1L;
        long startTime = System.currentTimeMillis();

        try {
            engine.start();
            TaskExecuteResult result = engine.submitAndWait(
                taskId,
                args.getExecutionClass(),
                args.getInputJson(),
                reportResult,
                args.getTimeoutMs()
            );
            long duration = System.currentTimeMillis() - startTime;
            printResult(result, duration);
            return result.success() ? 0 : 1;
        } catch (TimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("Task execution timed out after " + duration + "ms");
            return 1;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during task execution", e);
            System.err.println("Task execution failed: " + e.getMessage());
            return 1;
        } finally {
            engine.stop();
            executor.shutdown();
        }
    }

    private static AgentConfiguration buildConfiguration(String agentId, String serverUrl, String taskLogDir) {
        AgentConfiguration.Builder builder = AgentConfiguration.builder()
            .agentId(agentId)
            .agentUrl("http://localhost:0")  // dummy, not used in CLI
            .dagServerUrl(serverUrl != null ? serverUrl : "http://localhost:0")  // dummy in NoOp mode
            .autoRegister(false);

        if (taskLogDir != null && !taskLogDir.isBlank()) {
            builder.taskLogDir(taskLogDir);
        }

        return builder.build();
    }

    private static AgentSchedulerClient buildClient(AgentConfiguration config, String serverUrl) {
        if (serverUrl != null && !serverUrl.isBlank()) {
            return new DefaultAgentSchedulerClient(config, new ObjectMapper());
        }
        return new NoOpAgentSchedulerClient();
    }

    private static void printResult(TaskExecuteResult result, long durationMs) {
        String status = result.success() ? "SUCCESS" : "FAILED";
        System.out.println("Task execution completed:");
        System.out.println("  Task ID: " + result.taskId());
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + result.output());
        System.out.println("  Duration: " + durationMs + "ms");
    }
}
```

---

### Task 8: Write CLI tests

**Files:**
- Create: `dag-agent-cli/src/test/java/top/ilovemyhome/dagtask/agent/cli/DagAgentCliTest.java`

- [ ] **Step 1: Write the test class**

```java
package top.ilovemyhome.dagtask.agent.cli;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.agent.execution.EchoExecution;

import static org.assertj.core.api.Assertions.assertThat;

class DagAgentCliTest {

    @Test
    void shouldExecuteSuccessfully_WithLocalMode() {
        // Given
        CliArguments args = new CliArguments();
        setField(args, "executionClass", EchoExecution.class.getName());
        setField(args, "inputJson", "{\"message\":\"hello\"}");
        setField(args, "agentId", "test-cli-agent");
        setField(args, "timeoutMs", 30000L);

        // When
        int exitCode = DagAgentCli.execute(args);

        // Then
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void shouldReturnNonZero_WhenExecutionFails() {
        // Given - invalid execution class
        CliArguments args = new CliArguments();
        setField(args, "executionClass", "nonexistent.Class");
        setField(args, "inputJson", "{}");
        setField(args, "agentId", "test-cli-agent");
        setField(args, "timeoutMs", 30000L);

        // When
        int exitCode = DagAgentCli.execute(args);

        // Then
        assertThat(exitCode).isEqualTo(1);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = CliArguments.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 2: Run CLI tests**

Run:
```bash
mvn test -pl dag-agent-cli -am
```

Expected: 2 tests PASS

---

### Task 9: Register dag-agent-cli in parent pom

**Files:**
- Modify: `dag-task/pom.xml`

- [ ] **Step 1: Add dag-agent-cli to modules list**

In `<modules>` section, after `dag-agent-muserver` add:
```xml
<module>dag-agent-cli</module>
```

- [ ] **Step 2: Add dag-agent-cli to dependencyManagement**

In `<dependencyManagement>` section, after `dag-agent-muserver` add:
```xml
<dependency>
    <groupId>top.ilovemyhome.dagtask</groupId>
    <artifactId>dag-agent-cli</artifactId>
    <version>${revision}</version>
</dependency>
```

- [ ] **Step 3: Verify full project compiles**

Run:
```bash
mvn compile -pl dag-agent-cli -am
```

Expected: BUILD SUCCESS

---

### Task 10: Write README.md

**Files:**
- Create: `dag-agent-cli/README.md`

- [ ] **Step 1: Write README.md**

```markdown
# dag-agent-cli

Command-line tool for triggering a single task execution to quickly verify `TaskExecution` implementations.

## Purpose

This module provides a lightweight CLI that reuses `dag-agent`'s `TaskExecutionEngine` to execute a single task. It supports both local NoOp mode (no scheduler server needed) and scheduler server reporting mode.

## Usage

### Local Mode (NoOp)

Execute a task locally without connecting to a scheduler server:

```bash
java -jar dag-agent-cli.jar \
  -e top.ilovemyhome.dagtask.agent.execution.BashTaskExecution \
  -i '{"script":"echo hello"}'
```

### Scheduler Server Mode

Execute a task and report the result to a scheduler server:

```bash
java -jar dag-agent-cli.jar \
  -e top.ilovemyhome.dagtask.agent.execution.BashTaskExecution \
  -i '{"script":"echo hello"}' \
  -s http://localhost:8080
```

### Command-line Options

| Option | Short | Required | Default | Description |
|--------|-------|----------|---------|-------------|
| `--execution` | `-e` | Yes | - | TaskExecution implementation class full qualified name |
| `--input` | `-i` | No | `{}` | Input JSON for the task |
| `--server-url` | `-s` | No | - | Scheduler server URL |
| `--agent-id` | `-a` | No | auto | Agent ID |
| `--task-log-dir` | `-l` | No | - | Task log directory |
| `--timeout` | `-t` | No | 300000 | Timeout in milliseconds |
| `--help` | `-h` | No | - | Show help |

## Output Format

On success:
```
Task execution completed:
  Task ID: 1
  Status: SUCCESS
  Output: {"exitCode":0,"stdout":"hello\n","stderr":"","timedOut":false}
  Duration: 120ms
```

On failure:
```
Task execution completed:
  Task ID: 1
  Status: FAILED
  Output: script is required and must not be blank
  Duration: 5ms
```

## Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Task executed successfully |
| 1 | Task execution failed or timed out |
```

---

### Task 11: Verify full build

- [ ] **Step 1: Run all tests in dag-agent-cli**

```bash
mvn test -pl dag-agent-cli -am
```

Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 2: Run all tests in dag-agent**

```bash
mvn test -pl dag-agent -am
```

Expected: BUILD SUCCESS, all tests pass (including new `TaskExecutionEngineWaitTest`)

- [ ] **Step 3: Full project build**

```bash
mvn clean compile -pl dag-agent-cli -am
```

Expected: BUILD SUCCESS

---

## Spec Coverage Checklist

| Spec Requirement | Plan Task |
|---|---|
| Extend `TaskExecutionEngine` with `submitAndWait()` | Task 1 |
| `submitAndWait()` blocks until task completes or times out | Task 1 |
| Complete reuse of existing engine pipeline | Task 1 |
| Timeout kills the task | Task 1 |
| `NoOpAgentSchedulerClient` for local mode | Task 2 |
| Optional `--server-url` for real reporting | Task 7 |
| picocli argument parsing | Task 6 |
| Console output format | Task 7 |
| Exit codes (0 success, 1 failure) | Task 7 |
| Module metadata | Task 4 |
| README documentation | Task 10 |
| Unit tests for `submitAndWait` | Task 3 |
| Unit tests for CLI | Task 8 |

---

## Placeholder Scan

- No TBD/TODO/"implement later"/"fill in details" found
- All steps contain complete code
- All file paths are exact
- All commands have expected outputs
