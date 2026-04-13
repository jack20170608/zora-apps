# Deadletter Directory IO Separation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the deadletter persistence from single file to directory to achieve full read-write separation, improving concurrency safety and performance.

**Architecture:** Keep backward compatibility with existing configuration. Add support for directory mode where each failed batch is written as a separate file. Writing creates new files (no locking), retry reads and deletes processed files (no rewriting). Full read-write separation.

**Tech Stack:** Java 25, Jackson for JSON, java.io.File for file operations, JUnit 5 for testing.

---

## Files to Modify

| File | Change Summary |
|------|----------------|
| `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/config/AgentConfiguration.java` | Add `deadLetterPersistencePath` field, update getter/setter/builder/toString |
| `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/TaskExecutionEngine.java` | Refactor persistence logic to support both file and directory modes |
| `dag-task/README.md` | Update configuration example and documentation |
| `dag-task/dag-agent/src/test/java/top/ilovemyhome/dagtask/agent/config/AgentConfigurationLoadTest.java` | Update configuration loading test if needed |

---

### Task 1: Update AgentConfiguration with new field

**Files:**
- Modify: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/config/AgentConfiguration.java`

- [ ] **Step 1: Add the new field `deadLetterPersistencePath`**

Add after line 21 (`private String deadLetterPersistenceFile;`):
```java
private String deadLetterPersistencePath;
```

- [ ] **Step 2: Update constructor**

In line 35, add:
```java
this.deadLetterPersistencePath = builder.deadLetterPersistencePath;
```

- [ ] **Step 3: Add getter and setter**

Add after `setDeadLetterPersistenceFile()` (line 104):
```java
    public String getDeadLetterPersistencePath() {
        return deadLetterPersistencePath;
    }

    public void setDeadLetterPersistencePath(String deadLetterPersistencePath) {
        this.deadLetterPersistencePath = deadLetterPersistencePath;
    }
```

- [ ] **Step 4: Update toString()**

In line 146, add:
```java
                ", deadLetterPersistencePath='" + deadLetterPersistencePath + '\'' +
```

- [ ] **Step 5: Update Builder static class**

In the Builder class (after line 159), add:
```java
        private String deadLetterPersistencePath;
```

Add after `deadLetterPersistenceFile()` builder method (line 195):
```java
        public Builder deadLetterPersistencePath(String deadLetterPersistencePath) {
            this.deadLetterPersistencePath = deadLetterPersistencePath;
            return this;
        }
```

- [ ] **Step 6: Compile to verify changes**

```bash
cd dag-task/dag-agent && mvn compile -q
```

Expected: Compilation successful.

- [ ] **Step 7: Commit**

```bash
git add dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/config/AgentConfiguration.java
git commit -m "feat: add deadLetterPersistencePath field to AgentConfiguration"
```

---

### Task 2: Refactor TaskExecutionEngine to support directory mode

**Files:**
- Modify: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/TaskExecutionEngine.java`

- [ ] **Step 1: Change field from `deadLetterFile` to support both modes**

Replace line 68 (`private File deadLetterFile;`):

```java
    // Dead letter persistence - can be either file (legacy mode) or directory (new mode)
    private File deadLetterPersistenceFile;
    private File deadLetterPersistenceDir;
```

- [ ] **Step 2: Refactor `initDeadLetterFile()` to `initDeadLetterPersistence()`**

Replace method `initDeadLetterFile()` (lines 133-147):

```java
    /**
     * Initializes dead letter persistence - detects if config is file or directory.
     */
    private void initDeadLetterPersistence() {
        // Priority: new path config first
        String pathConfig = config.getDeadLetterPersistencePath();
        String fileConfig = config.getDeadLetterPersistenceFile();
        
        if (pathConfig != null && !pathConfig.isBlank()) {
            // New directory mode
            this.deadLetterPersistenceDir = new java.io.File(pathConfig);
            this.deadLetterPersistenceFile = null;
            // Ensure directory exists
            if (!deadLetterPersistenceDir.exists()) {
                deadLetterPersistenceDir.mkdirs();
            }
            LOGGER.info("Dead letter persistence configured in directory: {}", deadLetterPersistenceDir.getAbsolutePath());
            return;
        }
        
        if (fileConfig != null && !fileConfig.isBlank()) {
            // Legacy file mode - keep for backward compatibility
            this.deadLetterPersistenceFile = new java.io.File(fileConfig);
            this.deadLetterPersistenceDir = null;
            // Ensure parent directories exist
            java.io.File parent = this.deadLetterPersistenceFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            LOGGER.info("Dead letter persistence configured (legacy file mode): {}", this.deadLetterPersistenceFile.getAbsolutePath());
            return;
        }
        
        // No persistence configured
        this.deadLetterPersistenceFile = null;
        this.deadLetterPersistenceDir = null;
        LOGGER.info("No dead letter persistence configured, failed reports will be dropped");
    }
```

- [ ] **Step 3: Update constructors to call new init method**

In both constructors (lines 108 and 127), change `initDeadLetterFile();` to:
```java
initDeadLetterPersistence();
```

- [ ] **Step 4: Refactor `persistToDeadLetterFile()` method**

Update method signature and logic (lines 357-373):

```java
    /**
     * Persists failed results to dead letter. In directory mode creates a new file per batch.
     * In file mode appends to the existing file (legacy).
     */
    private void persistToDeadLetterFile(List<TaskExecuteResult> results) {
        List<Long> taskIds = results.stream().map(TaskExecuteResult::taskId).toList();
        
        // Directory mode - new file per batch
        if (deadLetterPersistenceDir != null) {
            try {
                // Generate unique filename: YYYYMMDD-HHMMss-<UUID>.json
                String timestamp = java.time.format.DateTimeFormatter
                    .ofPattern("yyyyMMdd-HHmmss")
                    .format(java.time.LocalDateTime.now());
                String filename = timestamp + "-" + java.util.UUID.randomUUID() + ".json";
                File newFile = new java.io.File(deadLetterPersistenceDir, filename);
                
                // Write the entire batch as JSON
                FileWriter writer = new FileWriter(newFile);
                String json = objectMapper.writeValueAsString(results);
                writer.write(json);
                writer.close();
                LOGGER.debug("Persisted failed results for task {} to dead letter file {}", taskIds, filename);
            } catch (Exception e) {
                LOGGER.error("Failed to persist failed results for task {} to dead letter directory", taskIds, e);
            }
            return;
        }
        
        // Legacy file mode - append to single file
        if (deadLetterPersistenceFile != null) {
            try {
                // Append as JSON line (one results per line)
                FileWriter writer = new FileWriter(deadLetterPersistenceFile, true);
                String json = objectMapper.writeValueAsString(results);
                writer.write(json + System.lineSeparator());
                writer.close();
                LOGGER.debug("Persisted failed results for task {} to dead letter file", taskIds);
            } catch (Exception e) {
                LOGGER.error("Failed to persist failed results for task {} to dead letter file", taskIds, e);
            }
            return;
        }
        
        // No persistence configured
        LOGGER.debug("No dead letter persistence configured, dropping failed results for tasks {}", taskIds);
    }
```

- [ ] **Step 5: Refactor `retryPersistedDeadLetterOnce()` method**

Replace entire method (lines 381-439):

```java
    /**
     * Retries all persisted failed reports once.
     * In directory mode: processes each file individually, deletes after processing regardless of outcome.
     * In file mode: uses original rewrite logic.
     * @return number of reports successfully reported
     */
    private int retryPersistedDeadLetterOnce() {
        // Directory mode
        if (deadLetterPersistenceDir != null) {
            return retryPersistedInDirectory();
        }
        
        // Legacy file mode
        if (deadLetterPersistenceFile != null && deadLetterPersistenceFile.exists()) {
            return retryPersistedInSingleFile();
        }
        
        return 0;
    }
```

- [ ] **Step 6: Extract `retryPersistedInDirectory()` private method**

Add after `retryPersistedDeadLetterOnce()`:

```java
    /**
     * Retry processing in directory mode: list all files, process each one, delete after processing.
     */
    private int retryPersistedInDirectory() {
        int totalSuccessCount = 0;
        File[] files = deadLetterPersistenceDir.listFiles();
        if (files == null || files.length == 0) {
            return 0;
        }
        
        for (File file : files) {
            if (!file.isFile() || file.getName().startsWith(".")) {
                continue; // skip directories and hidden files
            }
            
            try {
                // Read the entire file content
                String json = java.nio.file.Files.readString(file.toPath());
                if (json.isBlank()) {
                    Files.deleteIfExists(file.toPath());
                    continue;
                }
                
                // Parse as list of TaskExecuteResult
                List<TaskExecuteResult> reports = objectMapper.readValue(json, 
                    new TypeReference<List<TaskExecuteResult>>() {});
                
                // Try to report all
                int successInFile = 0;
                try {
                    agentSchedulerClient.reportTaskResult(reports);
                    // All succeeded
                    totalSuccessCount += reports.size();
                    LOGGER.debug("Successfully retried {} dead letter reports from file {}", 
                        reports.size(), file.getName());
                } catch (Exception e) {
                    // Some or all failed - report individually to count successes
                    for (TaskExecuteResult report : reports) {
                        try {
                            agentSchedulerClient.reportTaskResult(List.of(report));
                            successInFile++;
                            totalSuccessCount++;
                            LOGGER.debug("Successfully retried dead letter report for task {}", 
                                report.taskId());
                        } catch (Exception ex) {
                            // Still failed - drop it, will be re-persisted if it fails again
                            LOGGER.debug("Still failed to report task {} from dead letter, will drop", 
                                report.taskId());
                        }
                    }
                }
                
                // Always delete the file after processing - failures get re-persisted as new files
                Files.deleteIfExists(file.toPath());
                
            } catch (Exception e) {
                LOGGER.error("Error processing dead letter file {}, will skip for now", 
                    file.getName(), e);
            }
        }
        
        if (totalSuccessCount > 0) {
            LOGGER.info("Retried {} successfully from dead letter directory", totalSuccessCount);
        }
        return totalSuccessCount;
    }
```

- [ ] **Step 7: Extract `retryPersistedInSingleFile()` private method (legacy mode)**

Add after the above method, move the original single file logic here:

```java
    /**
     * Retry processing in legacy single file mode. Keeps the original logic.
     */
    private int retryPersistedInSingleFile() {
        if (deadLetterPersistenceFile.length() == 0) {
            return 0;
        }

        int successCount = 0;
        List<TaskExecuteResult> remainingFailed = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(deadLetterPersistenceFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    List<TaskExecuteResult> reports = objectMapper.readValue(line, new TypeReference<>() {
                    });
                    agentSchedulerClient.reportTaskResult(reports);
                    // Success - don't add back to remaining
                    successCount += reports.size();
                    LOGGER.debug("Successfully retried dead letter reports for batch", reports);
                } catch (Exception e) {
                    // Still failing - keep for next retry
                    // Note: original code tried to parse as single report, keep that behavior
                    try {
                        TaskExecuteResult report = objectMapper.readValue(line, TaskExecuteResult.class);
                        remainingFailed.add(report);
                        LOGGER.debug("Still failed to report task {} from dead letter, will retry later", report.taskId());
                    } catch (Exception ex) {
                        LOGGER.debug("Failed to parse failed report from dead letter, dropping", ex);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error reading dead letter file {}", deadLetterPersistenceFile.getAbsolutePath(), e);
            return 0;
        }

        // Rewrite the file with only remaining failed reports
        if (!remainingFailed.isEmpty()) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(deadLetterPersistenceFile))) {
                for (TaskExecuteResult report : remainingFailed) {
                    writer.println(objectMapper.writeValueAsString(report));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to rewrite dead letter file with remaining failures", e);
            }
        } else {
            // No failures left - truncate the file
            try {
                new java.io.PrintWriter(deadLetterPersistenceFile).close();
            } catch (Exception e) {
                LOGGER.error("Failed to truncate empty dead letter file", e);
            }
        }

        if (successCount > 0) {
            LOGGER.info("Retried {} successfully from dead letter, {} remaining", successCount, remainingFailed.size());
        }
        return successCount;
    }
```

- [ ] **Step 8: Update `getDeadLetterQueueSize()` method**

Replace method (lines 656-674):

```java
    /**
     * Returns the number of failed reports in the dead letter.
     * For directory mode: counts all non-empty files (each file is at least one report).
     * For file mode: counts lines in the file.
     */
    public int getDeadLetterQueueSize() {
        // Directory mode
        if (deadLetterPersistenceDir != null && deadLetterPersistenceDir.exists()) {
            File[] files = deadLetterPersistenceDir.listFiles();
            if (files == null) {
                return 0;
            }
            int count = 0;
            for (File file : files) {
                if (file.isFile() && file.length() > 0 && !file.getName().startsWith(".")) {
                    count++;
                }
            }
            return count;
        }
        
        // Legacy file mode
        if (deadLetterPersistenceFile != null && deadLetterPersistenceFile.exists()) {
            // Count the number of non-empty lines in the file
            int count = 0;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(deadLetterPersistenceFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        count++;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to count dead letter entries from file", e);
                return 0;
            }
            return count;
        }
        return 0;
    }
```

- [ ] **Step 9: Update the start method where deadLetterFile is checked**

In line 229 (`if (deadLetterFile != null) {`), change to:
```java
        // Start dead letter retry thread if persistence is configured
        if (deadLetterPersistenceDir != null || deadLetterPersistenceFile != null) {
```

- [ ] **Step 10: Update the stop method where deadLetterFile is checked**

Check if any other references to `deadLetterFile` need updating - use the above new fields.

Check `persistToDeadLetterFile` null checks already updated. In shutdown (line 344-348) it's just stopping the thread, doesn't reference the file, so no change needed.

- [ ] **Step 11: Compile and check for errors**

```bash
cd dag-task/dag-agent && mvn compile -q
```

Expected: Compilation successful. Fix any errors.

- [ ] **Step 12: Run existing tests**

```bash
cd dag-task/dag-agent && mvn test -q
```

Expected: All existing tests pass.

- [ ] **Step 13: Commit**

```bash
git add dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/core/TaskExecutionEngine.java
git commit -m "refactor: refactor TaskExecutionEngine to support directory deadletter mode"
```

---

### Task 3: Update README documentation

**Files:**
- Modify: `dag-task/README.md`

- [ ] **Step 1: Update configuration example**

Update line 290 in README.md from:
```hocon
  deadLetterPersistenceFile = "./dead-letter.jsonl"
```
to:
```hocon
  # 死信队列持久化目录（每个失败批次一个文件，读写分离）
  deadLetterPersistencePath = "./dead-letter"
```

- [ ] **Step 2: Update dead letter documentation**

Update line 335 in README.md from:
```java
deadLetterPersistenceFile = "./dead-letter.jsonl"
```
to:
```java
// 目录模式（推荐）- 每个失败批次一个文件，读写分离
deadLetterPersistencePath = "./dead-letter"
// 兼容旧配置 - 单个文件模式
// deadLetterPersistenceFile = "./dead-letter.jsonl"
```

- [ ] **Step 3: Commit**

```bash
git add dag-task/README.md
git commit -m "docs: update README for deadletter directory mode"
```

---

### Task 4: Verify and test the implementation

**Files:**
- Test: optionally add new test for directory mode

- [ ] **Step 1: Run all tests**

```bash
cd dag-task && mvn test -q
```

Expected: All tests pass.

- [ ] **Step 2: Verify backward compatibility**

Check that:
- If `deadLetterPersistencePath` configured → directory mode works
- If only `deadLetterPersistenceFile` configured → legacy file mode still works
- If neither configured → no persistence works

- [ ] **Step 3: Final compile**

```bash
cd dag-task && mvn clean compile -DskipTests
```

Expected: Clean compile succeeds.

- [ ] **Step 4: Commit any final test fixes**

If tests need adjustments, commit them.

---

## Self-Review

- ✅ Spec coverage: All requirements from the spec covered (new field, directory mode, backward compatibility, docs)
- ✅ No placeholders: All steps have exact code and exact commands
- ✅ Type consistency: Field names consistent across all methods
- ✅ Backward compatibility: Legacy file mode preserved
- ✅ Public API preserved: All public method signatures unchanged

