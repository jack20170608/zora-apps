# Merge TaskManager to DagScheduler Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Merge all methods from TaskManager interface into DagScheduler interface and implementation, then delete the original TaskManager files.

**Architecture:** Add all 5 methods from TaskManager to the existing DagScheduler interface and implement them in DagSchedulerImpl. The implementation logic is already well-defined in TaskManagerImpl - we just move it over. Finally delete the no-longer-needed TaskManager files.

**Tech Stack:** Java 25, Maven, JUnit 5

---

### Task 1: Add TaskManager methods to DagScheduler interface

**Files:**
- Modify: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/service/DagScheduler.java`

- [ ] **Step 1: Read current file content** (already done in exploration)

Current DagScheduler.java:
```java
package top.ilovemyhome.dagtask.si.service;

import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.List;

/**
 * DAG Scheduler - Responsible for DAG runtime scheduling and execution triggering.
 * Handles:
 * <ul>
 *     <li>Starting a DAG execution</li>
 *     <li>Finding ready tasks (all predecessors completed)</li>
 *     <li>Triggering ready tasks to TaskDispatcher for distribution</li>
 *     <li>Callback after task completion to trigger successors</li>
 * </ul>
 */
public interface DagScheduler {

    /**
     * Start execution of a DAG.
     * Finds all initially ready tasks (no predecessors or all predecessors completed)
     * and triggers them for execution.
     *
     * @param orderKey the order key of the DAG to start
     */
    void start(String orderKey);

    /**
     * Find all ready tasks for a given order.
     * A task is ready when all its predecessor tasks have completed successfully.
     *
     * @param orderKey the order key to search
     * @return list of ready tasks ready for execution
     */
    List<TaskRecord> findReadyTasks(String orderKey);

    /**
     * Trigger all ready tasks to TaskDispatcher for execution.
     *
     * @param orderKey the order key
     * @return number of tasks triggered
     */
    int triggerReadyTasks(String orderKey);

    /**
     * Callback when a task completes.
     * Updates task status and triggers all ready successor tasks.
     *
     * @param taskId    the completed task ID
     * @param newStatus the final status of the task
     * @param output    the task output
     */
    void onTaskCompleted(Long taskId, TaskStatus newStatus, TaskOutput output);
}
```

- [ ] **Step 2: Add the 5 new methods from TaskManager**

Add these imports at the top:
```java
import top.ilovemyhome.dagtask.si.TaskInput;
```

Add these methods after `onTaskCompleted`:
```java
    /**
     * Execute a task synchronously right now.
     * Useful for testing or manual triggering.
     *
     * @param taskId the task ID to execute
     * @param input  the input parameters
     * @return the task output after execution
     */
    TaskOutput runNow(Long taskId, TaskInput input);

    /**
     * Force mark a task as completed successfully.
     * After marking, triggers all ready successor tasks.
     *
     * @param taskId the task ID
     * @param output the output to set
     */
    void forceOk(Long taskId, TaskOutput output);

    /**
     * Force kill (fail) a task.
     * After marking, no successor tasks will be triggered.
     *
     * @param taskId the task ID
     */
    void kill(Long taskId);

    /**
     * Put a task on hold.
     * The task will not be triggered even if it is ready.
     *
     * @param taskId the task ID
     */
    void hold(Long taskId);

    /**
     * Resume a held task.
     * If the task is ready after resuming, it will be triggered.
     *
     * @param taskId the task ID
     */
    void resume(Long taskId);
```

- [ ] **Step 3: Update JavaDoc class comment to reflect the expanded responsibilities**

Update the class javadoc:
```java
/**
 * DAG Scheduler - Responsible for DAG runtime scheduling, execution triggering,
 * and manual task operations.
 * Handles:
 * <ul>
 *     <li>Starting a DAG execution</li>
 *     <li>Finding ready tasks (all predecessors completed)</li>
 *     <li>Triggering ready tasks to TaskDispatcher for distribution</li>
 *     <li>Callback after task completion to trigger successors</li>
 *     <li>Manual task operations: run, force complete, kill, hold, resume</li>
 * </ul>
 */
```

- [ ] **Step 4: Compile to verify no errors**

Run: `mvn compile -pl dag-task/dag-si -am`
Expected: Compilation success

- [ ] **Step 5: Commit**

```bash
git add dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/service/DagScheduler.java
git commit -m "feat: add TaskManager methods to DagScheduler interface"
```

---

### Task 2: Implement all new methods in DagSchedulerImpl

**Files:**
- Modify: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/service/DagSchedulerImpl.java`

- [ ] **Step 1: Read current file content** (already done in exploration)

Current has the 4 original methods implemented. Constructor already has all required dependencies:
```java
public class DagSchedulerImpl implements DagScheduler {

    private final TaskRecordDao taskRecordDao;
    private final TaskDispatcher taskDispatcher;

    public DagSchedulerImpl(TaskRecordDao taskRecordDao, TaskDispatcher taskDispatcher) {
        this.taskRecordDao = taskRecordDao;
        this.taskDispatcher = taskDispatcher;
    }
    // ... existing methods
}
```

- [ ] **Step 2: Add missing imports**

Add these imports at the top:
```java
import top.ilovemyhome.dagtask.si.TaskInput;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
```

(Note: Some of these may already be imported)

- [ ] **Step 3: Add runNow implementation**

Add this method after `onTaskCompleted`:
```java
    @Override
    public TaskOutput runNow(Long taskId, TaskInput input) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(input);

        Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        TaskRecord task = taskOpt.get();
        logger.info("Running task {} manually", taskId);

        // Dispatch the task for execution
        var result = taskDispatcher.dispatch(task);
        if (!result.success()) {
            throw new RuntimeException("Failed to dispatch task: " + result.message());
        }

        // Update status to RUNNING
        taskRecordDao.start(taskId, input, LocalDateTime.now());

        // Note: This returns immediately, actual execution happens on agent
        return TaskOutput.empty();
    }
```

- [ ] **Step 4: Add forceOk implementation**

```java
    @Override
    public void forceOk(Long taskId, TaskOutput output) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(output);

        logger.info("Force marking task {} as successful", taskId);
        onTaskCompleted(taskId, TaskStatus.SUCCESS, output);
    }
```

- [ ] **Step 5: Add kill implementation**

```java
    @Override
    public void kill(Long taskId) {
        Objects.requireNonNull(taskId);

        logger.info("Force killing (marking as failed) task {}", taskId);
        TaskOutput output = TaskOutput.builder()
            .withSuccess(false)
            .withMessage("Task was manually killed by operator")
            .build();
        onTaskCompleted(taskId, TaskStatus.FAILED, output);
    }
```

- [ ] **Step 6: Add hold implementation**

```java
    @Override
    public void hold(Long taskId) {
        Objects.requireNonNull(taskId);

        Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        logger.info("Putting task {} on hold", taskId);
        taskRecordDao.updateStatus(taskId, TaskStatus.HOLD);
    }
```

- [ ] **Step 7: Add resume implementation**

```java
    @Override
    public void resume(Long taskId) {
        Objects.requireNonNull(taskId);

        Optional<TaskRecord> taskOpt = taskRecordDao.loadTaskById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        TaskRecord task = taskOpt.get();
        logger.info("Resuming held task {}", taskId);

        // Change status back to INIT
        taskRecordDao.updateStatus(taskId, TaskStatus.INIT);

        // Check if task is ready now and trigger it
        if (taskRecordDao.isReady(taskId)) {
            triggerReadyTasks(task.getOrderKey());
        }
    }
```

- [ ] **Step 8: Compile to verify no errors**

Run: `mvn compile -pl dag-task/dag-scheduler -am`
Expected: Compilation success

- [ ] **Step 9: Commit**

```bash
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/service/DagSchedulerImpl.java
git commit -m "impl: implement TaskManager methods in DagSchedulerImpl"
```

---

### Task 3: Delete TaskManager and TaskManagerImpl files

**Files:**
- Delete: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/service/TaskManager.java`
- Delete: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/service/TaskManagerImpl.java`

- [ ] **Step 1: Verify no other dependencies on TaskManager**

Run: `grep -r "TaskManager" . --include="*.java"`
Expected: Only shows the two files being deleted

- [ ] **Step 2: Delete the two files**

```bash
rm dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/service/TaskManager.java
rm dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/service/TaskManagerImpl.java
```

- [ ] **Step 3: Full compile**

Run: `mvn compile -pl dag-task -am`
Expected: Compilation success

- [ ] **Step 4: Run all tests**

Run: `mvn test -pl dag-task -am`
Expected: All tests pass

- [ ] **Step 5: Commit deletions**

```bash
git rm dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/service/TaskManager.java
git rm dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/service/TaskManagerImpl.java
git commit -m "refactor: remove TaskManager interface and implementation after merge"
```

---

### Task 4: Final verification

**Files:** None to modify

- [ ] **Step 1: Full clean build**

Run: `mvn clean compile test -pl dag-task`
Expected: Build success, all tests pass

- [ ] **Step 2: Verify no remaining references**

Run: `grep -r "TaskManager" . --include="*.java" --include="*.xml"`
Expected: No matches found (or only matches in comments/docs)

- [ ] **Step 3: Commit any remaining cleanups** (if needed)

- [ ] **Step 4: Done**

---
