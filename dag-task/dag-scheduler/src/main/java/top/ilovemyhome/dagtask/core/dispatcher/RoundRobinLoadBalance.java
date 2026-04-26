package top.ilovemyhome.dagtask.core.dispatcher;

import top.ilovemyhome.dagtask.si.agent.AgentStatus;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link LoadBalanceStrategy} implementation that uses <b>round-robin</b> selection.
 * <p>
 * Cycles through the candidate list in order, distributing tasks evenly.
 * This is a simple strategy that works well when all agents have similar capacity
 * and task durations are roughly uniform.
 * </p>
 * <p>
 * Note: The round-robin counter is shared across all selection calls, so tasks
 * are distributed across all agents regardless of execution key filtering.
 * </p>
 */
public class RoundRobinLoadBalance implements LoadBalanceStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public AgentStatus select(List<AgentStatus> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        int size = candidates.size();
        if (size == 1) {
            return candidates.get(0);
        }
        int index = Math.abs(counter.getAndIncrement() % size);
        return candidates.get(index);
    }
}
