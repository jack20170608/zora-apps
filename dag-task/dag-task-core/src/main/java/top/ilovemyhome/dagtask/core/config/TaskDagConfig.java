package top.ilovemyhome.dagtask.core.config;

import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.OrderType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * POJO representing a complete DAG workflow configuration from YAML.
 */
public class TaskDagConfig {

    private String orderKey;
    private String orderName;
    private String orderType = "Free";
    private Map<String, String> attributes = Map.of();
    private List<TaskConfig> tasks = List.of();

    public String getOrderKey() {
        return orderKey;
    }

    public void setOrderKey(String orderKey) {
        this.orderKey = orderKey;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public OrderType getOrderType() {
        return OrderType.valueOf(orderType);
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public List<TaskConfig> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskConfig> tasks) {
        this.tasks = tasks;
    }

    /**
     * Convert this configuration to a TaskOrder with built TaskRecords.
     *
     * @param recordBuilder function that builds TaskRecords from this config
     * @return the fully built TaskOrder
     */
    public TaskOrder convertToTaskOrder(Function<TaskDagConfig, List<TaskRecord>> recordBuilder) {
        List<TaskRecord> records = recordBuilder.apply(this);
        return TaskOrder.builder()
                .withKey(orderKey)
                .withName(orderName)
                .withOrderType(getOrderType())
                .withAttributes(attributes)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskDagConfig that = (TaskDagConfig) o;
        return Objects.equals(orderKey, that.orderKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderKey);
    }

    @Override
    public String toString() {
        return "TaskDagConfig{" +
                "orderKey='" + orderKey + '\'' +
                ", orderName='" + orderName + '\'' +
                ", orderType='" + orderType + '\'' +
                ", tasksCount=" + (tasks != null ? tasks.size() : 0) +
                '}';
    }
}
