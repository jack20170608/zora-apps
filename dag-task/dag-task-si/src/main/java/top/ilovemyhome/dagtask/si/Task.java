package top.ilovemyhome.dagtask.si;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class Task<I, O> implements Runnable {

    // Static
    private final Long id;
    private final String orderKey;
    private final String name;
    private final TaskExecution<I, O> taskExecution;
    private final Long timeout;
    private final TimeUnit timeoutUnit;
    private final LocalDateTime createDt;

    private TaskInput<I> input;
    private Set<Long> successorIds;
    private TaskOutput<O> output;
    private TaskStatus taskStatus;
    private LocalDateTime startDt;
    private LocalDateTime endDt;
    protected transient TaskContext taskContext;
    private LocalDateTime lastUpdateDt;

    protected Task(Long id, TaskContext taskContext, String orderKey, String name, TaskInput<I> input
            , TaskStatus taskStatus, Long timeout, TimeUnit timeoutUnit, TaskExecution<I, O> taskExecution) {
        LocalDateTime now = LocalDateTime.now();
        this.id = id;
        this.taskContext = taskContext;
        this.orderKey = orderKey;
        this.name = name;
        this.input = input;
        this.taskStatus = taskStatus;
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.taskExecution = taskExecution;
        this.createDt = now;
        this.lastUpdateDt = now;
    }

    protected void start() {
        LocalDateTime now = LocalDateTime.now();
        this.startDt = now;
        this.taskStatus = TaskStatus.RUNNING;
        this.lastUpdateDt = now;
        taskContext.getTaskRecordDao().start(id, this.input, now);
    }

    public synchronized void failure(TaskStatus newStatus, TaskOutput<O> output) {
        if (newStatus == TaskStatus.SUCCESS) {
            throw new IllegalArgumentException("The status should not success as your call failure method!!");
        }
        LocalDateTime now = LocalDateTime.now();
        this.taskStatus = newStatus;
        this.output = output;
        this.endDt = now;
        this.lastUpdateDt = now;
        taskContext.getTaskRecordDao().stop(id, newStatus, output, now);
    }

    public boolean isReady() {
        return taskContext.getTaskRecordDao().isReady(getId());
    }

    protected void error(TaskOutput<O> output) {
        failure(TaskStatus.ERROR, output);
    }

    protected void unknown(TaskOutput<O> output) {
        failure(TaskStatus.UNKNOWN, output);
    }

    protected void timeout(TaskOutput<O> output) {
        failure(TaskStatus.TIMEOUT, output);
    }

    public synchronized void success(TaskOutput<O> output) {
        LocalDateTime now = LocalDateTime.now();
        this.output = output;
        this.endDt = now;
        this.lastUpdateDt = now;
        this.taskStatus = TaskStatus.SUCCESS;
        taskContext.getTaskRecordDao().stop(id, TaskStatus.SUCCESS, output, now);
        LOGGER.info("OrderId={}, id={}, name={} execute successfully.", orderKey, id, name);

        // Find all ready successors with a single SQL query and submit them
        Set<Long> successors = getSuccessorIds();
        if (Objects.nonNull(successors) && !successors.isEmpty()) {
            taskContext.getTaskRecordDao().<I, O>findReadySuccessors(getId())
                .forEach(task -> taskContext.getThreadPool().submit(task));
        }
    }

    public void skip(TaskOutput<O> output) {
        LocalDateTime now = LocalDateTime.now();
        this.output = output;
        this.endDt = now;
        this.lastUpdateDt = now;
        this.taskStatus = TaskStatus.SKIPPED;
        taskContext.getTaskRecordDao().stop(id, TaskStatus.SKIPPED, output, now);
        LOGGER.info("OrderId={}, id={}, name={} execute skipped.", orderKey, id, name);

        // Skip all ready successors
        Set<Long> successors = getSuccessorIds();
        if (Objects.nonNull(successors) && !successors.isEmpty()) {
            taskContext.getTaskRecordDao().<I, O>findReadySuccessors(getId())
                .forEach(s -> s.skip(output));
        }
    }

    public Long getId() {
        return id;
    }

    public String getOrderKey() {
        return orderKey;
    }

    public String getName() {
        return name;
    }

    public TaskInput<I> getInput() {
        return input;
    }

    public TaskExecution<I, O> getTaskExecution() {
        return taskExecution;
    }

    public Long getTimeout() {
        return timeout;
    }

    public LocalDateTime getCreateDt() {
        return createDt;
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    public void setSuccessorIds(Set<Long> successorIds) {
        this.successorIds = successorIds;
    }

    public Set<Long> getSuccessorIds() {
        return successorIds;
    }

    public TaskOutput<O> getOutput() {
        return output;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public LocalDateTime getStartDt() {
        return startDt;
    }

    public LocalDateTime getEndDt() {
        return endDt;
    }

    public LocalDateTime getLastUpdateDt() {
        return lastUpdateDt;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", orderKey='" + orderKey + '\'' +
                ", name='" + name + '\'' +
                ", input=" + input +
                ", taskExecution=" + taskExecution +
                ", timeout=" + timeout +
                ", timeoutUnit=" + timeoutUnit +
                ", createDt=" + createDt +
                ", successorIds=" + successorIds +
                ", output=" + output +
                ", taskStatus=" + taskStatus +
                ", startDt=" + startDt +
                ", endDt=" + endDt +
                ", lastUpdateDt=" + lastUpdateDt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task<?, ?> that = (Task<?, ?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);

}
