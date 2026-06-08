package top.ilovemyhome.dagtask.scheduler.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.scheduler.domain.dispatcher.AgentSelector;
import top.ilovemyhome.dagtask.scheduler.domain.dispatcher.LoadBalanceStrategy;
import top.ilovemyhome.dagtask.scheduler.port.in.ScheduleDagRunUseCase;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentDispatcher;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentStatusRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentUnreachableException;
import top.ilovemyhome.dagtask.scheduler.port.out.Clock;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskDispatchRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskRecordRepository;
import top.ilovemyhome.dagtask.si.TaskDispatchRecord;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.dagtask.si.enums.DispatchStatus;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service driving the scheduler runtime loop.
 * <p>
 * Replaces {@code DagScheduleService.start/findReadyTasks/triggerReadyTasks/onTaskCompleted}.
 * Agent selection is delegated to the pure-domain helper
 * {@link AgentSelector}; delivery to the agent is via the {@link AgentDispatcher}
 * outbound port.
 * </p>
 */
public class ScheduleDagRunService implements ScheduleDagRunUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleDagRunService.class);

    private final TaskRecordRepository taskRecordRepository;
    private final AgentDispatcher agentDispatcher;
    private final AgentStatusRepository agentStatusRepository;
    private final TaskDispatchRepository taskDispatchRepository;
    private final Clock clock;
    private final LoadBalanceStrategy loadBalanceStrategy;

    public ScheduleDagRunService(TaskRecordRepository taskRecordRepository,
                                 AgentDispatcher agentDispatcher,
                                 AgentStatusRepository agentStatusRepository,
                                 TaskDispatchRepository taskDispatchRepository,
                                 Clock clock,
                                 LoadBalanceStrategy loadBalanceStrategy) {
        this.taskRecordRepository = Objects.requireNonNull(taskRecordRepository, "taskRecordRepository must not be null");
        this.agentDispatcher = Objects.requireNonNull(agentDispatcher, "agentDispatcher must not be null");
        this.agentStatusRepository = Objects.requireNonNull(agentStatusRepository, "agentStatusRepository must not be null");
        this.taskDispatchRepository = Objects.requireNonNull(taskDispatchRepository, "taskDispatchRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.loadBalanceStrategy = Objects.requireNonNull(loadBalanceStrategy, "loadBalanceStrategy must not be null");
    }

    @Override
    public void start(String orderKey) {
        Objects.requireNonNull(orderKey);
        logger.info("Starting DAG execution for order key: {}", orderKey);
        int triggered = triggerReadyTasks(orderKey);
        logger.info("Triggered {} ready tasks for order {}", triggered, orderKey);
    }

    @Override
    public List<TaskRecord> findReadyTasks(String orderKey) {
        return taskRecordRepository.findReadyTasksForOrder(orderKey);
    }

    @Override
    public int triggerReadyTasks(String orderKey) {
        List<TaskRecord> readyTasks = findReadyTasks(orderKey);
        if (readyTasks.isEmpty()) {
            return 0;
        }

        int triggeredCount = 0;
        for (TaskRecord task : readyTasks) {
            if (task.getStatus() != TaskStatus.INIT) {
                continue;
            }
            if (dispatchOne(task)) {
                taskRecordRepository.updateStatus(task.getId(), TaskStatus.DISPATCHED);
                triggeredCount++;
            }
        }
        return triggeredCount;
    }

    @Override
    public void onTaskCompleted(Long taskId, TaskStatus newStatus, TaskOutput output) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(newStatus);

        Optional<TaskRecord> taskOpt = taskRecordRepository.loadTaskById(taskId);
        if (taskOpt.isEmpty()) {
            logger.warn("Task {} not found when completing", taskId);
            return;
        }

        TaskRecord task = taskOpt.get();
        String orderKey = task.getOrderKey();

        LocalDateTime now = LocalDateTime.ofInstant(clock.now(), ZoneId.systemDefault());
        taskRecordRepository.stop(taskId, newStatus, output, now);
        logger.info("Task {} completed with status: {}", taskId, newStatus);

        if (newStatus == TaskStatus.SUCCESS) {
            List<TaskRecord> readySuccessors = taskRecordRepository.findReadySuccessors(taskId);
            int triggered = 0;
            for (TaskRecord successor : readySuccessors) {
                if (successor.getStatus() == TaskStatus.INIT && taskRecordRepository.isReady(successor.getId())) {
                    if (dispatchOne(successor)) {
                        taskRecordRepository.updateStatus(successor.getId(), TaskStatus.DISPATCHED);
                        triggered++;
                    }
                }
            }
            if (triggered > 0) {
                logger.info("Triggered {} successor tasks after task {} completed", triggered, taskId);
            }
        }

        if (taskRecordRepository.isSuccess(orderKey)) {
            logger.info("All tasks in order {} completed successfully", orderKey);
        }
    }

    /**
     * Internal dispatch helper: select an agent, record dispatch, deliver via port.
     * Returns true if dispatched (acknowledged by agent), false otherwise.
     */
    private boolean dispatchOne(TaskRecord task) {
        List<AgentStatus> activeAgents = agentStatusRepository.findAllActive();
        AgentStatus selected = AgentSelector.select(activeAgents, task.getExecutionKey(), loadBalanceStrategy);
        if (selected == null) {
            logger.warn("No available agent for task {} (executionKey={})", task.getId(), task.getExecutionKey());
            return false;
        }

        TaskDispatchRecord dispatchRecord = TaskDispatchRecord.builder()
            .withTaskId(task.getId())
            .withAgentId(selected.getAgentId())
            .withAgentUrl(selected.getAgentUrl())
            .withStatus(DispatchStatus.DISPATCHED)
            .build();
        taskDispatchRepository.create(dispatchRecord);

        try {
            AgentDispatcher.DispatchAck ack = agentDispatcher.dispatch(selected, task);
            if (ack.accepted()) {
                taskDispatchRepository.updateStatus(task.getId(), DispatchStatus.ACCEPTED);
                logger.info("Task {} dispatched successfully to agent {}", task.getId(), selected.getAgentId());
                return true;
            } else {
                taskDispatchRepository.updateStatus(task.getId(), DispatchStatus.REJECTED);
                logger.warn("Agent {} rejected task {}: {}", selected.getAgentId(), task.getId(), ack.message());
                return false;
            }
        } catch (AgentUnreachableException e) {
            taskDispatchRepository.updateStatus(task.getId(), DispatchStatus.FAILED);
            logger.error("Failed to dispatch task {} to agent {}", task.getId(), selected.getAgentId(), e);
            return false;
        }
    }
}
