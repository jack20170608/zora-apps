package top.ilovemyhome.dagtask.core.dispatcher;

import top.ilovemyhome.dagtask.si.agent.AgentStatus;

import java.util.List;

/**
 * Strategy interface for selecting an agent from candidate list for task dispatch.
 * <p>
 * Implementations of this interface define different algorithms for load balancing
 * across multiple available agents that can execute a given task type.
 * </p>
 */
public interface LoadBalanceStrategy {

    /**
     * Selects an agent from the list of candidate agents that can execute a task.
     *
     * @param candidates list of candidate agents (already filtered by execution key
     *                   and all are active/running)
     * @return the selected agent, or null if no suitable agent available
     */
    AgentStatus select(List<AgentStatus> candidates);
}
