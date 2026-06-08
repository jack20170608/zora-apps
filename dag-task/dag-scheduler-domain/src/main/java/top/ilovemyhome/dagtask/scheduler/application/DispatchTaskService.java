package top.ilovemyhome.dagtask.scheduler.application;

import top.ilovemyhome.dagtask.scheduler.domain.dispatcher.AgentSelector;
import top.ilovemyhome.dagtask.scheduler.domain.dispatcher.LoadBalanceStrategy;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentDispatcher;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentStatusRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskDispatchRepository;
import top.ilovemyhome.dagtask.si.DispatchResult;
import top.ilovemyhome.dagtask.si.TaskDispatchRecord;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.dagtask.si.enums.DispatchStatus;
import java.util.List;
import java.util.Objects;

/**
 * Application service for dispatching a single ready task to an executing agent.
 * <p>
 * Replaces the "select + submit + track" part of the legacy
 * {@code DefaultTaskDispatcher.dispatch(TaskRecord)}. The HTTP delivery is now
 * behind the {@link AgentDispatcher} outbound port; this service only handles
 * the domain logic: agent selection, capacity filtering, and dispatch record tracking.
 * </p>
 */
public class DispatchTaskService implements top.ilovemyhome.dagtask.scheduler.port.in.DispatchTasksUseCase {

    private final AgentStatusRepository agentStatusRepository;
    private final TaskDispatchRepository taskDispatchRepository;
    private final AgentDispatcher agentDispatcher;
    private final LoadBalanceStrategy loadBalanceStrategy;

    public DispatchTaskService(AgentStatusRepository agentStatusRepository,
                               TaskDispatchRepository taskDispatchRepository,
                               AgentDispatcher agentDispatcher,
                               LoadBalanceStrategy loadBalanceStrategy) {
        this.agentStatusRepository = Objects.requireNonNull(agentStatusRepository, "agentStatusRepository must not be null");
        this.taskDispatchRepository = Objects.requireNonNull(taskDispatchRepository, "taskDispatchRepository must not be null");
        this.agentDispatcher = Objects.requireNonNull(agentDispatcher, "agentDispatcher must not be null");
        this.loadBalanceStrategy = Objects.requireNonNull(loadBalanceStrategy, "loadBalanceStrategy must not be null");
    }

    @Override
    public DispatchResult dispatch(TaskRecord task) {
        Objects.requireNonNull(task, "task must not be null");
        String executionKey = task.getExecutionKey();

        // Step 1: Get all active agents
        List<AgentStatus> activeAgents = agentStatusRepository.findAllActive();
        if (activeAgents.isEmpty()) {
            return DispatchResult.noAvailableAgent("No active agents registered in the system");
        }

        // Step 2: Select agent using domain helper (filter + load-balance)
        AgentStatus selected = AgentSelector.select(activeAgents, executionKey, loadBalanceStrategy);

        if (selected == null) {
            // Differentiate the reason for no selection
            List<AgentStatus> withKey = AgentSelector.filterByExecutionKey(activeAgents, executionKey);
            if (withKey.isEmpty()) {
                return DispatchResult.noCandidateForExecutionKey(executionKey);
            }
            List<AgentStatus> withCapacity = AgentSelector.filterByCapacity(withKey);
            if (withCapacity.isEmpty()) {
                return DispatchResult.allCandidatesAtCapacity(executionKey);
            }
            return DispatchResult.selectionFailed();
        }

        // Step 3: Record the dispatch before sending
        TaskDispatchRecord dispatchRecord = TaskDispatchRecord.builder()
            .withTaskId(task.getId())
            .withAgentId(selected.getAgentId())
            .withAgentUrl(selected.getAgentUrl())
            .withStatus(DispatchStatus.DISPATCHED)
            .build();
        taskDispatchRepository.create(dispatchRecord);

        // Step 4: Deliver via outbound port
        try {
            AgentDispatcher.DispatchAck ack = agentDispatcher.dispatch(selected, task);
            if (ack.accepted()) {
                taskDispatchRepository.updateStatus(task.getId(), DispatchStatus.ACCEPTED);
                return DispatchResult.success(selected, task.getId());
            } else {
                taskDispatchRepository.updateStatus(task.getId(), DispatchStatus.REJECTED);
                return DispatchResult.agentQueueFull(selected);
            }
        } catch (top.ilovemyhome.dagtask.scheduler.port.out.AgentUnreachableException e) {
            taskDispatchRepository.updateStatus(task.getId(), DispatchStatus.FAILED);
            return DispatchResult.connectionFailed(selected, e.getMessage());
        }
    }
}
