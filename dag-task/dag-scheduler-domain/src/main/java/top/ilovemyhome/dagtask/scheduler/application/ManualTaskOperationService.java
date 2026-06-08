package top.ilovemyhome.dagtask.scheduler.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.scheduler.domain.dispatcher.AgentSelector;
import top.ilovemyhome.dagtask.scheduler.domain.dispatcher.LoadBalanceStrategy;
import top.ilovemyhome.dagtask.scheduler.port.in.ManualTaskOperationUseCase;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentDispatcher;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentStatusRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentUnreachableException;
import top.ilovemyhome.dagtask.scheduler.port.out.Clock;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskDispatchRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskRecordRepository;
import top.ilovemyhome.dagtask.si.DispatchResult;
import top.ilovemyhome.dagtask.si.TaskDispatchRecord;
import top.ilovemyhome.dagtask.si.TaskInput;
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
 * Application service for operator-initiated interventions on individual tasks.
 * <p>
 * Replaces {@code DagScheduleService.runNow/forceOk/kill/hold/resume}. Shares
 * agent-selection and dispatch logic with {@code ScheduleDagRunService} by using
 * the pure-domain {@link AgentSelector} helper and the {@link AgentDispatcher}
 * outbound port. {@code forceOk} / {@code kill} replicate the completion logic
 * inline rather than calling {@code ScheduleDagRunService.onTaskCompleted} (which
 * is an inbound port — ArchUnit forbids application services from depending on
 * each other's inbound port).
 * </p>
 */
public class ManualTaskOperationService implements ManualTaskOperationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ManualTaskOperationService.class);

    private final TaskRecordRepository taskRecordRepository;
    private final AgentDispatcher agentDispatcher;
    private final AgentStatusRepository agentStatusRepository;
    private final TaskDispatchRepository taskDispatchRepository;
    private final Clock clock;
    private final LoadBalanceStrategy loadBalanceStrategy;

    public ManualTaskOperationService(TaskRecordRepository taskRecordRepository,
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
    public DispatchResult runNow(Long taskId, TaskInput input) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(input);

        Optional<TaskRecord> taskOpt = taskRecordRepository.loadTaskById(taskId);
        if (taskOpt.isEmpty()) {
            throw new TaskNotFoundException("Task not found: " + taskId);
        }
        TaskRecord task = taskOpt.get();
        logger.info("Running task {} manually", taskId);

        DispatchResult result = dispatchInternal(task);
        if (!result.success()) {
            throw new RuntimeException("Failed to dispatch task: " + result.message());
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.now(), ZoneId.systemDefault());
        taskRecordRepository.start(taskId, input, now);
        return result;
    }

    @Override
    public void forceOk(Long taskId, TaskOutput output) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(output);
        logger.info("Force marking task {} as successful", taskId);
        completeTask(taskId, TaskStatus.SUCCESS, output);
    }

    @Override
    public void kill(Long taskId) {
        Objects.requireNonNull(taskId);
        logger.info("Force killing (marking as failed) task {}", taskId);
        TaskOutput output = TaskOutput.fail(taskId, null, "Task was manually killed by operator");
        completeTask(taskId, TaskStatus.ERROR, output);
    }

    @Override
    public void hold(Long taskId) {
        Objects.requireNonNull(taskId);
        Optional<TaskRecord> taskOpt = taskRecordRepository.loadTaskById(taskId);
        if (taskOpt.isEmpty()) {
            throw new TaskNotFoundException("Task not found: " + taskId);
        }
        logger.info("Putting task {} on hold", taskId);
        taskRecordRepository.updateStatus(taskId, TaskStatus.HOLD);
    }

    @Override
    public void resume(Long taskId) {
        Objects.requireNonNull(taskId);
        Optional<TaskRecord> taskOpt = taskRecordRepository.loadTaskById(taskId);
        if (taskOpt.isEmpty()) {
            throw new TaskNotFoundException("Task not found: " + taskId);
        }
        logger.info("Resuming held task {}", taskId);
        taskRecordRepository.updateStatus(taskId, TaskStatus.INIT);

        if (taskRecordRepository.isReady(taskId)) {
            // Trigger the task immediately if it's ready
            TaskRecord task = taskOpt.get();
            DispatchResult result = dispatchInternal(task);
            if (result.success()) {
                taskRecordRepository.updateStatus(taskId, TaskStatus.DISPATCHED);
            }
        }
    }

    /**
     * Completion logic shared by forceOk/kill. Mirrors
     * {@link ScheduleDagRunService#onTaskCompleted(Long, TaskStatus, TaskOutput)} —
     * inlined here to avoid application-layer cross-call.
     */
    private void completeTask(Long taskId, TaskStatus newStatus, TaskOutput output) {
        Optional<TaskRecord> taskOpt = taskRecordRepository.loadTaskById(taskId);
        if (taskOpt.isEmpty()) {
            logger.warn("Task {} not found when completing", taskId);
            return;
        }
        TaskRecord task = taskOpt.get();
        String orderKey = task.getOrderKey();

        LocalDateTime now = LocalDateTime.ofInstant(clock.now(), ZoneId.systemDefault());
        taskRecordRepository.stop(taskId, newStatus, output, now);

        if (newStatus == TaskStatus.SUCCESS) {
            List<TaskRecord> readySuccessors = taskRecordRepository.findReadySuccessors(taskId);
            for (TaskRecord successor : readySuccessors) {
                if (successor.getStatus() == TaskStatus.INIT && taskRecordRepository.isReady(successor.getId())) {
                    DispatchResult r = dispatchInternal(successor);
                    if (r.success()) {
                        taskRecordRepository.updateStatus(successor.getId(), TaskStatus.DISPATCHED);
                    }
                }
            }
        }
        if (taskRecordRepository.isSuccess(orderKey)) {
            logger.info("All tasks in order {} completed successfully", orderKey);
        }
    }

    /**
     * Internal dispatch helper mirroring {@code ScheduleDagRunService.dispatchOne}.
     */
    private DispatchResult dispatchInternal(TaskRecord task) {
        List<AgentStatus> activeAgents = agentStatusRepository.findAllActive();
        if (activeAgents.isEmpty()) {
            return DispatchResult.noAvailableAgent("No active agents registered in the system");
        }
        AgentStatus selected = AgentSelector.select(activeAgents, task.getExecutionKey(), loadBalanceStrategy);
        if (selected == null) {
            List<AgentStatus> withKey = AgentSelector.filterByExecutionKey(activeAgents, task.getExecutionKey());
            if (withKey.isEmpty()) {
                return DispatchResult.noCandidateForExecutionKey(task.getExecutionKey());
            }
            List<AgentStatus> withCapacity = AgentSelector.filterByCapacity(withKey);
            if (withCapacity.isEmpty()) {
                return DispatchResult.allCandidatesAtCapacity(task.getExecutionKey());
            }
            return DispatchResult.selectionFailed();
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
                return DispatchResult.success(selected, task.getId());
            } else {
                taskDispatchRepository.updateStatus(task.getId(), DispatchStatus.REJECTED);
                return DispatchResult.agentQueueFull(selected);
            }
        } catch (AgentUnreachableException e) {
            taskDispatchRepository.updateStatus(task.getId(), DispatchStatus.FAILED);
            return DispatchResult.connectionFailed(selected, e.getMessage());
        }
    }
}
