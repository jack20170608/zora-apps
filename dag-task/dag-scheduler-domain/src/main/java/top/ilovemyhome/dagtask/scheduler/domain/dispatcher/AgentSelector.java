package top.ilovemyhome.dagtask.scheduler.domain.dispatcher;

import top.ilovemyhome.dagtask.si.agent.AgentStatus;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Pure-domain helper: selects a candidate agent from a list of active agents
 * by filtering for execution-key support and capacity, then delegating to a
 * {@link LoadBalanceStrategy}.
 * <p>
 * Used by both {@code ScheduleDagRunService} and {@code DispatchTaskService}
 * to avoid application-layer cross-calls.
 * </p>
 */
public final class AgentSelector {

    private AgentSelector() {
        // utility class
    }

    /**
     * Select the best available agent for the given execution key.
     *
     * @param activeAgents     all currently active agents
     * @param executionKey     the execution key the task requires
     * @param loadBalanceStrategy the load balancing strategy to apply
     * @return the selected agent, or null if no suitable agent found
     */
    public static AgentStatus select(List<AgentStatus> activeAgents,
                                     String executionKey,
                                     LoadBalanceStrategy loadBalanceStrategy) {
        Objects.requireNonNull(activeAgents, "activeAgents must not be null");
        Objects.requireNonNull(loadBalanceStrategy, "loadBalanceStrategy must not be null");

        if (activeAgents.isEmpty()) {
            return null;
        }

        // Step 1: filter by execution key support
        List<AgentStatus> candidates = filterByExecutionKey(activeAgents, executionKey);
        if (candidates.isEmpty()) {
            return null;
        }

        // Step 2: filter by capacity (exclude agents at max)
        candidates = filterByCapacity(candidates);
        if (candidates.isEmpty()) {
            return null;
        }

        // Step 3: apply load balancing strategy
        return loadBalanceStrategy.select(candidates);
    }

    /**
     * Filter agents that support the given execution key.
     * An agent with blank/empty supportedExecutionKeys supports all keys.
     */
    public static List<AgentStatus> filterByExecutionKey(List<AgentStatus> agents, String executionKey) {
        return agents.stream()
            .filter(agent -> {
                String keys = agent.getSupportedExecutionKeys();
                if (keys == null || keys.isBlank()) {
                    return true; // supports all
                }
                return List.of(keys.split(",")).contains(executionKey);
            })
            .collect(Collectors.toList());
    }

    /**
     * Filter out agents that have already reached their maximum concurrent task limit.
     * An agent is available if runningTasks &lt; maxConcurrentTasks.
     */
    public static List<AgentStatus> filterByCapacity(List<AgentStatus> agents) {
        return agents.stream()
            .filter(agent -> agent.getRunningTasks() < agent.getMaxConcurrentTasks())
            .collect(Collectors.toList());
    }
}
