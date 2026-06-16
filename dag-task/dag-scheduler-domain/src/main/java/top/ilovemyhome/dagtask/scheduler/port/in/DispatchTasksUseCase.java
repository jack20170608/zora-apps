package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.si.DispatchResult;
import top.ilovemyhome.dagtask.si.TaskRecord;

/**
 * Trigger the scheduler-loop dispatch of an individual ready task to an executing agent.
 * <p>
 * Wraps the legacy {@code DefaultTaskDispatcher.dispatch(TaskRecord)} call. Agent
 * selection / load-balancing logic lives behind the {@code AgentDispatcher} outbound
 * port; this use case is the inbound entry-point used by the scheduler loop.
 * </p>
 */
public interface DispatchTasksUseCase {

    /**
     * Dispatch a single ready task. Selects an appropriate agent (via the outbound
     * {@code AgentDispatcher} port) and submits the work item to it.
     *
     * @param task the ready task to dispatch
     * @return the dispatch result indicating success or failure with details
     */
    DispatchResult dispatch(TaskRecord task);
}
