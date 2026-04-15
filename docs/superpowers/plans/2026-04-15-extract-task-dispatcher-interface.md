# Extract TaskDispatcher Interface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract an interface from the existing `TaskDispatcher` class to enable different implementation strategies and better testability.

**Architecture:** Create a new interface `TaskDispatcher` in the same package, move all public method signatures to the interface, rename the existing implementation class to `DefaultTaskDispatcher`, and update any references. This maintains backward compatibility while enabling dependency injection and mocking.

**Tech Stack:** Java 25, Maven, JUnit 5

---

### Task 1: Create the TaskDispatcher interface

**Files:**
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dispatcher/TaskDispatcher.java`

- [ ] **Step 1: Create the interface with all public method signatures**

```java
package top.ilovemyhome.dagtask.core.dispatcher;

import top.ilovemyhome.dagtask.si.DispatchResult;
import top.ilovemyhome.dagtask.si.TaskDispatchRecord;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.AgentRegistryItem;

import java.util.List;
import java.util.Optional;

/**
 * Dispatcher interface that selects an appropriate agent and dispatches ready tasks
 * for execution on that agent.
 */
public interface TaskDispatcher {

    /**
     * Dispatches a ready task to an appropriate agent for execution.
     *
     * @param task the ready task to dispatch
     * @return the dispatch result indicating success or failure with details
     */
    DispatchResult dispatch(TaskRecord task);

    /**
     * Requests a task to be killed on its executing agent.
     * This overload automatically looks up the agent from the dispatch tracking table.
     *
     * @param taskId the task ID to kill
     * @param dealer the user who performed the operation
     * @param reason the reason for killing the task
     * @return true if the kill request was accepted by the agent
     */
    boolean killTask(Long taskId, String dealer, String reason);

    /**
     * Requests a task to be killed on its executing agent.
     *
     * @param dispatchItem the dispatch record containing task and agent information
     * @param dealer the user who performed the operation
     * @param reason the reason for killing the task
     * @return true if the kill request was accepted by the agent
     */
    boolean killTask(TaskDispatchRecord dispatchItem, String dealer, String reason);

    /**
     * Requests a task to be forcefully marked as successful on its executing agent.
     * This overload automatically looks up the agent from the dispatch tracking table.
     *
     * @param taskId the task ID to force ok
     * @param dealer the user who performed the operation
     * @param reason the reason for forcefully marking the task as ok
     * @return true if the force-ok request was accepted by the agent
     */
    boolean forceOkTask(Long taskId, String dealer, String reason);

    /**
     * Requests a task to be forcefully marked as successful on its executing agent.
     *
     * @param dispatchItem the dispatch record containing task and agent information
     * @param dealer the user who performed the operation
     * @param reason the reason for forcefully marking the task as ok
     * @return true if the force-ok request was accepted by the agent
     */
    boolean forceOkTask(TaskDispatchRecord dispatchItem, String dealer, String reason);

    /**
     * Gets the current number of active agents that support a given execution key.
     * Useful for monitoring and diagnostics.
     *
     * @param executionKey the execution key to count
     * @return number of available agents for this execution key
     */
    int countAvailableAgents(String executionKey);

    /**
     * Gets all available candidate agents for a given execution key.
     *
     * @param executionKey the execution key
     * @return list of available candidate agents (active + supports execution key + has capacity)
     */
    List<AgentRegistryItem> getAvailableCandidates(String executionKey);

    /**
     * Finds all currently active (running) agents from the registry.
     * Active agents are those marked as running in the database.
     *
     * @return list of all active agents
     */
    List<AgentRegistryItem> findAllActiveAgents();

    /**
     * Finds an agent by its unique agent ID.
     *
     * @param agentId the unique agent identifier to search for
     * @return an Optional containing the agent if found, empty otherwise
     */
    Optional<AgentRegistryItem> findAgentByAgentId(String agentId);
}
```

- [ ] **Step 2: Commit**

```bash
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dispatcher/TaskDispatcher.java
git commit -m "feat: create TaskDispatcher interface"
```

---

### Task 2: Rename existing TaskDispatcher to DefaultTaskDispatcher

**Files:**
- Rename: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dispatcher/TaskDispatcher.java` → `DefaultTaskDispatcher.java`
- Modify: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dispatcher/DefaultTaskDispatcher.java`

- [ ] **Step 1: Rename the file**

```bash
git mv dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dispatcher/TaskDispatcher.java \
      dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dispatcher/DefaultTaskDispatcher.java
```

- [ ] **Step 2: Update class declaration to implement the interface**

Change from:
```java
public class TaskDispatcher {
```
To:
```java
public class DefaultTaskDispatcher implements TaskDispatcher {
```

Update constructor signatures:
```java
public DefaultTaskDispatcher(AgentRegistryDao agentRegistryDao,
                      TaskDispatchDao taskDispatchDao,
                      LoadBalanceStrategy loadBalanceStrategy,
                      ObjectMapper objectMapper) {
    this(agentRegistryDao, taskDispatchDao, loadBalanceStrategy, objectMapper,
        HttpClient.newHttpClient());
}

public DefaultTaskDispatcher(AgentRegistryDao agentRegistryDao,
                      TaskDispatchDao taskDispatchDao,
                      LoadBalanceStrategy loadBalanceStrategy,
                      ObjectMapper objectMapper,
                      HttpClient httpClient) {
    // ... existing body remains unchanged
}
```

- [ ] **Step 3: Verify all public methods implement the interface**

All method signatures already match, no changes needed to method bodies.

- [ ] **Step 4: Run maven compile to check for errors**

```bash
cd dag-task/dag-scheduler && mvn compile
```

Expected: Compilation succeeds.

- [ ] **Step 5: Commit**

```bash
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/dispatcher/DefaultTaskDispatcher.java
git commit -m "refactor: rename TaskDispatcher to DefaultTaskDispatcher implements TaskDispatcher"
```

---

### Task 3: Run existing tests to verify nothing is broken

**Files:**
- Test: any existing tests for TaskDispatcher

- [ ] **Step 1: Find and run existing tests**

```bash
cd dag-task/dag-scheduler && mvn test
```

Expected: All tests pass.

- [ ] **Step 2: Commit if any test fixes needed (should not be needed)**

---

## Self-Review

1. **Spec coverage:** Complete - all public methods are on the interface, existing implementation renamed.
2. **Placeholder scan:** No placeholders, all code is provided.
3. **Type consistency:** All method signatures match the existing implementation exactly.

---

Plan complete and saved to `docs/superpowers/plans/2026-04-15-extract-task-dispatcher-interface.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
