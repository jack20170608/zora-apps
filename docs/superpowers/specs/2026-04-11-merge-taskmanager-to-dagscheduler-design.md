# Merge TaskManager to DagScheduler Design

## Overview

Merge all interface methods from `TaskManager` into `DagScheduler` to simplify code structure. Currently TaskManager handles manual task operations while DagScheduler handles DAG runtime scheduling. Merging them into a single interface makes the API more cohesive since all operations are related to DAG execution control.

## Background

- `TaskManager` interface provides manual intervention capabilities: `runNow`, `forceOk`, `kill`, `hold`, `resume`
- `DagScheduler` interface handles DAG scheduling: `start`, `findReadyTasks`, `triggerReadyTasks`, `onTaskCompleted`
- `TaskManagerImpl` already depends on `DagScheduler` for many operations
- No other components in the codebase depend on `TaskManager` interface

## Design Decision

**Approach: Full merge, delete TaskManager**

- Add all 5 methods from TaskManager to DagScheduler interface
- Implement all methods in DagSchedulerImpl
- Delete `TaskManager.java` interface file
- Delete `TaskManagerImpl.java` implementation file

## Interface Changes

### After merge, DagScheduler will have the following methods:

From original DagScheduler:
- `void start(String orderKey)` - Start DAG execution
- `List<TaskRecord> findReadyTasks(String orderKey)` - Find all ready tasks for an order
- `int triggerReadyTasks(String orderKey)` - Trigger all ready tasks
- `void onTaskCompleted(Long taskId, TaskStatus newStatus, TaskOutput output)` - Callback when task completes

From TaskManager (newly added):
- `TaskOutput runNow(Long taskId, TaskInput input)` - Execute a task synchronously right now
- `void forceOk(Long taskId, TaskOutput output)` - Force mark a task as completed successfully
- `void kill(Long taskId)` - Force kill (fail) a task
- `void hold(Long taskId)` - Put a task on hold
- `void resume(Long taskId)` - Resume a held task

## Implementation Details

### DagSchedulerImpl changes:
- Add `TaskDispatcher` as a field (already has `TaskRecordDao`)
- Implement all 5 new methods
- Reuse the same implementation logic from `TaskManagerImpl`
- Constructor will change to accept `TaskDispatcher` (already accepts it, so no change needed - actually it already has it)

Looking at existing code:
- `DagSchedulerImpl` already has `TaskRecordDao` and `TaskDispatcher` as fields
- Constructor already accepts both dependencies
- No constructor signature changes needed

### Dependencies:
- No other dependencies on `TaskManager` found in codebase
- No API breaking changes for external consumers unless they were using `TaskManager` directly
- Based on current codebase scan, no direct usage found

## Deletions

- `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/service/TaskManager.java` - delete
- `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/service/TaskManagerImpl.java` - delete

## Testing

- No tests need to be created specifically for this refactoring
- Existing tests should continue to pass
- Verify compilation after changes

## Success Criteria

- Code compiles successfully
- All existing tests pass
- All functionality that was available through TaskManager is now available through DagScheduler
- No compilation errors from deleted files
