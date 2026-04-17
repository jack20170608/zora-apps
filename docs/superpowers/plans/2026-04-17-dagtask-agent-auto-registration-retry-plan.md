# DAG Task Agent Auto-Registration Retry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add automatic retries with simulated annealing (exponential backoff) to DAG Task Agent auto-registration on startup, starting at 10ms and capped at 5 minutes maximum delay.

**Architecture:** The retry happens in an asynchronous daemon background thread so it doesn't block the main startup sequence. Exponential backoff with ±10% jitter prevents thundering herd. The retry continues indefinitely until successful or until the agent is stopped.

**Tech Stack:** Java 25, JUnit 5, Mockito, SLF4J logging

---

## File Changes Overview
- Modify: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgent.java` - add retry fields, modify `start()` and `stop()`, add retry task
- Test: `dag-task/dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgentTest.java` - new unit tests (create new file)

---

### Task 1: Add fields and constants to DagTaskAgent

**Files:**
- Modify: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgent.java`

- [ ] **Step 1: Add the constants after the LOGGER declaration**

```java
    // Auto-registration retry constants (simulated annealing / exponential backoff)
    static final long INITIAL_DELAY_MS = 10L;
    static final long MAX_DELAY_MS = 5 * 60 * 1000L; // 5 minutes
    private static final double JITTER_FACTOR = 0.1;

    // Fields for registration retry tracking
    private volatile boolean registered;
    private volatile Thread registrationRetryThread;
```

- [ ] **Step 2: Run Maven compile to check for errors**

```bash
cd dag-task/dag-agent && mvn compile
```
Expected: Compiles successfully with no errors

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgent.java
git commit -m "feat: add constants and fields for auto-registration retry"
```

---

### Task 2: Modify start() method to launch background retry thread

**Files:**
- Modify: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgent.java:92-106` (lines 92-106 where auto-registration happens)

- [ ] **Step 1: Replace existing auto-registration code with new logic**

Replace lines 92-106:

```java
        // Auto-register with server
        if (config.isAutoRegister()) {
            var registration = new AgentRegisterRequest(
                    config.getAgentId(),
                    config.getBaseUrl(),
                    config.getMaxConcurrentTasks(),
                    config.getMaxPendingTasks(),
                    config.getSupportedExecutionKeys()
            );
            var response = agentSchedulerClient.register(registration);
            boolean registeredSuccess = response.getStatus() >= 200 && response.getStatus() < 300;
            if (registeredSuccess) {
                registered = true;
                int taskCount = registration.supportedExecutionKeys().size();
                LOGGER.info("Agent {} successfully registered with DAG server at {}, supported {} execution keys",
                        registration.agentId(), config.getDagServerUrl(), taskCount);
            } else {
                LOGGER.warn("Initial auto-registration failed with status {}, starting background retry thread",
                        response.getStatus());
                registered = false;
                RegistrationRetryTask retryTask = new RegistrationRetryTask(registration);
                registrationRetryThread = new Thread(retryTask);
                registrationRetryThread.setDaemon(true);
                registrationRetryThread.setName("dag-agent-registration-retry");
                registrationRetryThread.start();
            }
        } else {
            registered = false;
        }
```

- [ ] **Step 2: Run Maven compile**

```bash
cd dag-task/dag-agent && mvn compile
```
Expected: Compiles successfully (will have error because RegistrationRetryTask doesn't exist yet - expected)

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgent.java
git commit -m "feat: modify start() to launch background retry thread"
```

---

### Task 3: Implement RegistrationRetryTask inner class

**Files:**
- Modify: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgent.java` (add inner class before the end of the class)

- [ ] **Step 1: Add the RegistrationRetryTask inner class before LOGGER**

```java
    /**
     * Background task that retries registration with exponential backoff (simulated annealing).
     * Retries until successful or until the agent is stopped.
     */
    private class RegistrationRetryTask implements Runnable {
        private final AgentRegisterRequest registration;
        private long currentDelayMs;

        /**
         * Creates a new retry task for the given registration.
         * @param registration the registration request to retry
         */
        public RegistrationRetryTask(AgentRegisterRequest registration) {
            this.registration = registration;
            this.currentDelayMs = INITIAL_DELAY_MS;
        }

        @Override
        public void run() {
            while (!registered && isRunning()) {
                try {
                    long jitteredDelay = applyJitter(currentDelayMs);
                    Thread.sleep(jitteredDelay);

                    var response = agentSchedulerClient.register(registration);
                    boolean success = response.getStatus() >= 200 && response.getStatus() < 300;

                    if (success) {
                        int taskCount = registration.supportedExecutionKeys().size();
                        LOGGER.info("Agent {} registered successfully after retry, supported {} execution keys",
                                registration.agentId(), taskCount);
                        registered = true;
                        return;
                    }

                    LOGGER.warn("Registration retry failed with status {}, will retry in {}ms",
                            response.getStatus(), currentDelayMs);

                    // Exponential backoff, capped at MAX_DELAY_MS
                    currentDelayMs = Math.min(currentDelayMs * 2, MAX_DELAY_MS);
                } catch (InterruptedException e) {
                    LOGGER.info("Registration retry thread interrupted, stopping retry");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Applies random jitter to the delay to avoid thundering herd problem.
     * Adds ±JITTER_FACTOR random variation.
     * @param delay the base delay in milliseconds
     * @return jittered delay
     */
    long applyJitter(long delay) { // package-private for testing
        double random = (Math.random() * 2 - 1) * JITTER_FACTOR; // -0.1 to +0.1
        double jittered = delay * (1 + random);
        return Math.round(jittered);
    }
```

- [ ] **Step 2: Run Maven compile**

```bash
cd dag-task/dag-agent && mvn compile
```
Expected: Compiles successfully

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgent.java
git commit -m "feat: implement RegistrationRetryTask with jitter"
```

---

### Task 4: Modify stop() method to interrupt retry thread

**Files:**
- Modify: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgent.java:121-134`

- [ ] **Step 1: Add thread interruption after setting running = false**

Update the stop() method to look like this:

```java
    /**
     * Stop the agent and optionally unregister from the DAG server.
     *
     * @param unregister whether to unregister from the DAG server before stopping
     */
    public void stop(boolean unregister) {
        running = false;
        executionEngine.stop();
        // Interrupt registration retry thread if it's still running
        if (registrationRetryThread != null && registrationRetryThread.isAlive()) {
            registrationRetryThread.interrupt();
        }
        if (unregister) {
            var unregistration = new AgentUnregistration(config.getAgentId());
            var response = agentSchedulerClient.unregister(unregistration);
            boolean success = response.getStatus() >= 200 && response.getStatus() < 300;
            if (!success) {
                LOGGER.warn("Failed to unregister agent from DAG server during shutdown");
            }
        }
        taskExecutor.shutdown();
        LOGGER.info("DAG Task Agent stopped{}", unregister ? " (unregistered)" : "");
    }
```

- [ ] **Step 2: Run Maven compile**

```bash
cd dag-task/dag-agent && mvn compile
```
Expected: Compiles successfully

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgent.java
git commit -m "feat: interrupt retry thread on agent stop"
```

---

### Task 5: Create unit test for DagTaskAgent with retry

**Files:**
- Create: `dag-task/dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgentTest.java`

- [ ] **Step 1: Write the unit test**

```java
package top.ilovemyhome.dagtask.agent.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DagTaskAgentTest {

    @Mock
    private AgentConfiguration config;

    @Mock
    private AgentSchedulerClient client;

    @Mock
    private ExecutorService executor;

    @Test
    void shouldMarkRegistered_WhenFirstRegistrationSucceeds() {
        // Given
        when(config.isAutoRegister()).thenReturn(true);
        when(config.getAgentId()).thenReturn("test-agent");
        when(config.getBaseUrl()).thenReturn("http://localhost:8080");
        when(config.getMaxConcurrentTasks()).thenReturn(10);
        when(config.getMaxPendingTasks()).thenReturn(100);
        when(config.getSupportedExecutionKeys()).thenReturn(List.of("echo"));
        when(client.register(any(AgentRegisterRequest.class)))
                .thenReturn(Response.ok().build());

        // When
        DagTaskAgent agent = new DagTaskAgent(config, client, executor);
        agent.start();

        // Then
        assertThat(agent.isRegistered()).isTrue();
        verify(client, times(1)).register(any());
    }

    @Test
    void shouldStartBackgroundThread_WhenFirstRegistrationFails() throws InterruptedException {
        // Given
        when(config.isAutoRegister()).thenReturn(true);
        when(config.getAgentId()).thenReturn("test-agent");
        when(config.getBaseUrl()).thenReturn("http://localhost:8080");
        when(config.getMaxConcurrentTasks()).thenReturn(10);
        when(config.getMaxPendingTasks()).thenReturn(100);
        when(config.getSupportedExecutionKeys()).thenReturn(List.of("echo"));
        when(config.getDagServerUrl()).thenReturn("http://server:8080");
        when(client.register(any(AgentRegisterRequest.class)))
                .thenReturn(Response.serverError().build());

        // When
        DagTaskAgent agent = new DagTaskAgent(config, client, executor);
        agent.start();

        // Then
        assertThat(agent.isRegistered()).isFalse();
        assertThat(agent.getRegistrationRetryThread()).isNotNull();
        assertThat(agent.getRegistrationRetryThread().isAlive()).isTrue();

        // Cleanup - interrupt the thread
        agent.stop(false);
        assertThat(agent.getRegistrationRetryThread().isInterrupted()).isTrue();
    }

    @Test
    void shouldApplyJitterWithinExpectedRange() {
        // Given
        DagTaskAgent agent = new DagTaskAgent(config, client, executor);
        long delay = 100L;

        // When - test multiple times to check range
        for (int i = 0; i < 100; i++) {
            long jittered = agent.applyJitter(delay);
            // Expected range: 90 - 110 (±10%)
            assertThat(jittered).isBetween(90L, 110L);
        }
    }

    @Test
    void shouldVerifyConstants() {
        // Verify the requirements are met
        assertThat(DagTaskAgent.INITIAL_DELAY_MS).isEqualTo(10L);
        assertThat(DagTaskAgent.MAX_DELAY_MS).isEqualTo(5 * 60 * 1000L); // 5 minutes
    }
}
```

Note: Need to add getter for testing: add these getter methods at the end of `DagTaskAgent.java`:

Add to DagTaskAgent.java (after getResource()):

```java
    /**
     * Check if the agent is successfully registered.
     * Package-private for testing.
     */
    boolean isRegistered() {
        return registered;
    }

    /**
     * Get the registration retry thread.
     * Package-private for testing.
     */
    Thread getRegistrationRetryThread() {
        return registrationRetryThread;
    }
```

- [ ] **Step 2: Add getters to DagTaskAgent.java**

Add the above getters after `getResource()` method.

- [ ] **Step 3: Run the tests**

```bash
cd dag-task/dag-agent && mvn test -Dtest=DagTaskAgentTest
```
Expected: All tests pass

- [ ] **Step 4: Run all tests to ensure nothing broke**

```bash
cd dag-task/dag-agent && mvn test
```
Expected: All existing tests still pass

- [ ] **Step 5: Commit**

```bash
git add dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgent.java
git add dag-task/dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/core/DagTaskAgentTest.java
git commit -m "test: add unit tests for auto-registration retry"
```

---

## Self-Review Check

1. **Spec coverage:** All requirements covered:
   - ✅ 10ms initial delay
   - ✅ 5 minutes maximum delay cap
   - ✅ Simulated annealing (exponential backoff)
   - ✅ Jitter added
   - ✅ Asynchronous non-blocking
   - ✅ Clean interruption on stop
   - ✅ Unit tests

2. **Placeholder scan:** No placeholders, all steps have exact code and commands

3. **Type consistency:** All names are consistent throughout the plan

---

