package top.ilovemyhome.dagtask.si;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Immutable record that tracks the dispatch information for a task.
 * <p>
 * This table maintains a record of where each task was dispatched (which agent),
 * when it was dispatched, and any additional parameters that were used during dispatch.
 * This information is needed for subsequent management operations like:
 * <ul>
 *     <li>{@code forceOk} - Force mark a task as successful</li>
 *     <li>{@code kill} - Terminate a running task</li>
 *     <li>{@code hold} - Pause a waiting task</li>
 *     <li>Debugging and auditing - Track where tasks were dispatched</li>
 *     <li>Statistics - Analyze dispatch distribution across agents</li>
 * </ul>
 * </p>
 */
public final class TaskDispatchRecord {

    private Long id;
    private Long taskId;
    private String agentId;
    private String agentUrl;
    private LocalDateTime dispatchTime;
    private LocalDateTime lastUpdateTime;
    private DispatchStatus status;
    private Map<String, String> parameters;

    /**
     * Dispatch status enum.
     */
    public enum DispatchStatus {
        /**
         * Task has been dispatched to the agent but not yet accepted.
         */
        DISPATCHED,

        /**
         * Agent has accepted the task and it's queued/running.
         */
        ACCEPTED,

        /**
         * Agent rejected the task (e.g., queue full).
         */
        REJECTED,

        /**
         * Dispatch failed due to connection/network error.
         */
        FAILED,

        /**
         * Task has completed execution (success or failure).
         */
        COMPLETED
    }

    /**
     * Field definitions for database mapping.
     */
    public enum Field {
        id("id", true),
        taskId("task_id"),
        agentId("agent_id"),
        agentUrl("agent_url"),
        dispatchTime("dispatch_time"),
        lastUpdateTime("last_update_time"),
        status("status"),
        parameters("parameters");

        private final String dbColumn;
        private final boolean isId;

        Field(String dbColumn) {
            this.dbColumn = dbColumn;
            this.isId = false;
        }

        Field(String dbColumn, boolean isId) {
            this.dbColumn = dbColumn;
            this.isId = isId;
        }

        public String getDbColumn() {
            return dbColumn;
        }

        public boolean isId() {
            return isId;
        }
    }

    public static final Map<String, String> FIELD_COLUMN_MAP
        = Collections.unmodifiableMap(Stream.of(Field.values())
        .collect(Collectors.toMap(Field::name, Field::getDbColumn)));

    public static final String ID_FIELD = Field.id.name();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentUrl() {
        return agentUrl;
    }

    public void setAgentUrl(String agentUrl) {
        this.agentUrl = agentUrl;
    }

    public LocalDateTime getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(LocalDateTime dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public DispatchStatus getStatus() {
        return status;
    }

    public void setStatus(DispatchStatus status) {
        this.status = status;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * Creates a new builder initialized with default values.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder initialized from an existing record.
     *
     * @param record the existing record to copy from
     * @return a new builder initialized with the record's values
     */
    public static Builder builder(TaskDispatchRecord record) {
        return new Builder()
            .withId(record.getId())
            .withTaskId(record.getTaskId())
            .withAgentId(record.getAgentId())
            .withAgentUrl(record.getAgentUrl())
            .withDispatchTime(record.getDispatchTime())
            .withLastUpdateTime(record.getLastUpdateTime())
            .withStatus(record.getStatus())
            .withParameters(record.getParameters());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskDispatchRecord that = (TaskDispatchRecord) o;
        return Objects.equals(id, that.id)
            && Objects.equals(taskId, that.taskId)
            && Objects.equals(agentId, that.agentId)
            && Objects.equals(agentUrl, that.agentUrl)
            && Objects.equals(dispatchTime, that.dispatchTime)
            && Objects.equals(lastUpdateTime, that.lastUpdateTime)
            && status == that.status
            && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, taskId, agentId, agentUrl, dispatchTime, lastUpdateTime, status, parameters);
    }

    @Override
    public String toString() {
        return "TaskDispatchRecord{" +
            "id=" + id +
            ", taskId=" + taskId +
            ", agentId='" + agentId + '\'' +
            ", agentUrl='" + agentUrl + '\'' +
            ", dispatchTime=" + dispatchTime +
            ", status=" + status +
            '}';
    }

    /**
     * Builder for TaskDispatchRecord.
     */
    public static class Builder {
        private Long id;
        private Long taskId;
        private String agentId;
        private String agentUrl;
        private LocalDateTime dispatchTime;
        private LocalDateTime lastUpdateTime;
        private DispatchStatus status;
        private Map<String, String> parameters;

        private Builder() {
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withTaskId(Long taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder withAgentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder withAgentUrl(String agentUrl) {
            this.agentUrl = agentUrl;
            return this;
        }

        public Builder withDispatchTime(LocalDateTime dispatchTime) {
            this.dispatchTime = dispatchTime;
            return this;
        }

        public Builder withLastUpdateTime(LocalDateTime lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
            return this;
        }

        public Builder withStatus(DispatchStatus status) {
            this.status = status;
            return this;
        }

        public Builder withParameters(Map<String, String> parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Builds a new TaskDispatchRecord with the current builder values.
         * Automatically sets dispatchTime and lastUpdateTime to now if not provided.
         *
         * @return the built TaskDispatchRecord
         */
        public TaskDispatchRecord build() {
            LocalDateTime now = LocalDateTime.now();
            TaskDispatchRecord record = new TaskDispatchRecord();
            record.setId(id);
            record.setTaskId(taskId);
            record.setAgentId(agentId);
            record.setAgentUrl(agentUrl);
            record.setDispatchTime(dispatchTime == null ? now : dispatchTime);
            record.setLastUpdateTime(lastUpdateTime == null ? now : lastUpdateTime);
            record.setStatus(status == null ? DispatchStatus.DISPATCHED : status);
            record.setParameters(parameters);
            return record;
        }
    }
}
