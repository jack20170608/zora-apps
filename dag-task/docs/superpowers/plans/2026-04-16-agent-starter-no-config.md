# AgentStarter 不依赖配置文件改造 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修改 `AgentStarter` 使其不依赖 Typesafe Config 从配置文件读取配置，改为直接接受 `AgentConfiguration` 参数。

**Architecture:** 移除 `AgentStarter` 中两个现有的 `start()` 方法，新增一个接受 `AgentConfiguration` 参数的 `start()` 方法。保持其他启动逻辑不变（创建 ObjectMapper、AgentSchedulerClient、Executor，构建并启动 DagTaskAgent，添加 shutdown hook）。

**Tech Stack:** Java 25, JUnit 5, Maven, Typesafe Config (still used by AgentConfiguration.load, but not by AgentStarter)

---

### Task 1: Modify AgentStarter class

**Files:**
- Modify: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/AgentStarter.java`

- [ ] **Step 1: Read current file content** (already read, but re-verify before editing)

- [ ] **Step 2: Replace existing implementation with new approach**

New content after modification:

```java
package top.ilovemyhome.dagtask.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.ilovemyhome.dagtask.agent.client.DefaultAgentSchedulerClient;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgent;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgentBuilder;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;

import java.util.Objects;
import java.util.concurrent.Executors;

public class AgentStarter {

    private AgentStarter() {
        // Utility class
    }

    public static void start(AgentConfiguration agentConfig) {
        Objects.requireNonNull(agentConfig, "agentConfig is required");

        // Create default dependencies
        ObjectMapper objectMapper = new ObjectMapper();
        AgentSchedulerClient agentSchedulerClient = new DefaultAgentSchedulerClient(agentConfig);
        var executor = Executors.newFixedThreadPool(agentConfig.getMaxConcurrentTasks());

        // Create and start agent
        DagTaskAgent agent = new DagTaskAgentBuilder()
                .config(agentConfig)
                .objectMapper(objectMapper)
                .agentSchedulerClient(agentSchedulerClient)
                .taskExecutor(executor)
                .build();
        agent.start();

        // Add shutdown hook
        agent.stop();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            agent.stop();
        }));
    }
}
```

- [ ] **Step 3: Remove unused imports**

  The imports for `com.typesafe.config.Config` and `com.typesafe.config.ConfigFactory` will be removed.

- [ ] **Step 4: Compile to verify syntax**

Run:
```bash
cd dag-task/dag-agent
mvn compile
```
Expected: Compilation succeeds.

- [ ] **Step 5: Commit**

```bash
git add dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/AgentStarter.java
git commit -m "refactor: change AgentStarter to accept AgentConfiguration directly (no config file)"
```

### Task 2: Check for existing usages of AgentStarter

**Files:**
- Check: entire codebase for `AgentStarter.start()` calls

- [ ] **Step 1: Search for usages**

Search: `AgentStarter\.start` across all files.

If usages found: update them to use the new API pattern:
```java
// Before: AgentStarter.start();
// After:
AgentConfiguration config = AgentConfiguration.builder()
        .agentId("your-agent-id")
        .agentUrl("http://localhost:8080")
        .dagServerUrl("http://localhost:8081")
        .build();
AgentStarter.start(config);
```

- [ ] **Step 2: Compile again to verify no broken references**

Run:
```bash
cd dag-task
mvn compile
```
Expected: Compilation succeeds.

- [ ] **Step 3: Commit if changes needed**

```bash
git add <modified-files>
git commit -m "refactor: update usages of AgentStarter to new API"
```

If no usages found, skip to Task 3.

### Task 3: Add/update test for AgentStarter

**Files:**
- Create/Modify: `dag-task/dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/AgentStarterTest.java`

- [ ] **Step 1: Create a simple test that verifies AgentStarter works with builder-built AgentConfiguration**

Test code:

```java
package top.ilovemyhome.dagtask.agent;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;

import static org.junit.jupiter.api.Assertions.*;

class AgentStarterTest {

    @Test
    void testStartWithAgentConfiguration() {
        // This just verifies the method compiles and runs without throwing
        // Full integration test would need a running server
        AgentConfiguration config = AgentConfiguration.builder()
                .agentId("test-agent-1")
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8081")
                .maxConcurrentTasks(2)
                .maxPendingTasks(10)
                .build();

        // Just verify it doesn't throw exception when creating the agent
        // We don't actually start it fully in unit test
        assertDoesNotThrow(() -> {
            // AgentStarter.start(config) would start and block, but for this test
            // we just verify the configuration works - the actual start is tested in integration
            AgentStarter.start(config);
        });
    }
}
```

Wait - actually `start()` starts the agent and registers shutdown hook. For unit test, that's okay - it just won't actually process anything.

- [ ] **Step 2: Run the test**

Run:
```bash
cd dag-task/dag-agent
mvn test -Dtest=AgentStarterTest
```
Expected: Test passes.

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/AgentStarterTest.java
git commit -m "test: add unit test for AgentStarter new API"
```

### Task 4: Full build and test verification

**Files:** All modules

- [ ] **Step 1: Run full maven compile and test**

Run:
```bash
cd dag-task
mvn clean compile test
```
Expected: All tests pass, compilation succeeds.

- [ ] **Step 2: Verify no Typesafe Config import in AgentStarter**

Check that `AgentStarter.java` no longer imports `com.typesafe.config.*`.

