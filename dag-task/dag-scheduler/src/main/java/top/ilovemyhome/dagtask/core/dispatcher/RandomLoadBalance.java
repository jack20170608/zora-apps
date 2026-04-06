package top.ilovemyhome.dagtask.core.dispatcher;

import top.ilovemyhome.dagtask.si.agent.AgentInfo;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@link LoadBalanceStrategy} implementation that selects a candidate <b>randomly</b>.
 * <p>
 * Simple random selection provides good statistical distribution without any
 * state tracking. Works well for homogeneous environments where all agents
 * have similar capacity.
 * </p>
 */
public class RandomLoadBalance implements LoadBalanceStrategy {

    @Override
    public AgentInfo select(List<AgentInfo> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        int size = candidates.size();
        if (size == 1) {
            return candidates.get(0);
        }
        Random random = ThreadLocalRandom.current();
        int index = random.nextInt(size);
        return candidates.get(index);
    }
}
