package top.ilovemyhome.dagtask.core.config;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * POJO representing an individual task configuration from YAML.
 */
public class TaskConfig {

    private Integer taskId;
    private String taskName;
    private String description;
    private String executionClass;
    private String executionType = "sync";
    private List<Integer> dependencies = List.of();
    private Integer timeout = 60;
    private String timeoutUnit = "SECONDS";
    private String input;
    private Boolean dummy = false;

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExecutionClass() {
        return executionClass;
    }

    public void setExecutionClass(String executionClass) {
        this.executionClass = executionClass;
    }

    public String getExecutionType() {
        return executionType;
    }

    public void setExecutionType(String executionType) {
        this.executionType = executionType;
    }

    public List<Integer> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Integer> dependencies) {
        this.dependencies = dependencies;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getTimeoutUnit() {
        return timeoutUnit;
    }

    public void setTimeoutUnit(String timeoutUnit) {
        this.timeoutUnit = timeoutUnit;
    }

    public TimeUnit getTimeoutTimeUnit() {
        return TimeUnit.valueOf(timeoutUnit);
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public Boolean isDummy() {
        return dummy;
    }

    public void setDummy(Boolean dummy) {
        this.dummy = dummy;
    }

    public boolean isAsync() {
        return "async".equalsIgnoreCase(executionType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskConfig that = (TaskConfig) o;
        return Objects.equals(taskId, that.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return "TaskConfig{" +
                "taskId=" + taskId +
                ", taskName='" + taskName + '\'' +
                ", executionClass='" + executionClass + '\'' +
                ", executionType='" + executionType + '\'' +
                ", dependencies=" + dependencies +
                '}';
    }
}
