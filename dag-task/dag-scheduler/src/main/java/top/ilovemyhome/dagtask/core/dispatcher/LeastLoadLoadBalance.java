package top.ilovemyhome.dagtask.core.dispatcher;

import top.ilovemyhome.dagtask.si.agent.AgentStatus;

import java.util.Comparator;
import java.util.List;

/**
 * {@link LoadBalanceStrategy} implementation that selects the agent with the <b>least current load</b>.
 * <p>
 * The load is measured by the number of currently <b>running tasks</b> on the agent.
 * This strategy tends to distribute tasks evenly across available agents, keeping
 * utilization balanced. It prefers agents that are currently less busy.
 * </p>
 * <p>
 * This is generally the recommended strategy for most workloads because it:
 * <ul>
 *     <li>Avoids overloading any single agent</li>
 *     <li>Adapts naturally to different agent capacities</li>
 *     <li>Has very low selection overhead</li>
 * </ul>
 * </p>
 */
public class LeastLoadLoadBalance implements LoadBalanceStrategy {

    /**
     * Comparator that orders agents by current running task count ascending.
     * The agent with the fewest running tasks comes first.
     */
    private static final Comparator<AgentStatus> LEAST_RUNNING_COMPARATOR =
        Comparator.comparingInt(AgentStatus::getRunningTasks);

    @Override
    public AgentStatus select(List<AgentStatus> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        // Select the candidate with the fewest currently running tasks
        return candidates.stream()
            .min(LEAST_RUNNING_COMPARATOR)
            .orElse(null);
    }
}
