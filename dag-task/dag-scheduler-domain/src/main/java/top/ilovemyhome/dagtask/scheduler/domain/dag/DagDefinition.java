package top.ilovemyhome.dagtask.scheduler.domain.dag;

import java.util.List;
import java.util.Set;

/**
 * Domain-neutral DAG definition. Replaces JSON-string input from legacy API.
 */
public record DagDefinition(String name, List<TaskDefinition> tasks) {

    /**
     * A single task within a {@link DagDefinition}. Field shape mirrors the legacy JSON
     * fields parsed by {@code DagManageServiceImpl.buildTaskFromNode}:
     * {@code name}, {@code description}, {@code executionKey}, {@code async}, {@code dummy},
     * {@code input}, {@code timeout}, {@code timeoutUnit}, and {@code dependsOn[]}.
     *
     * @param key task identifier within the DAG (matches legacy {@code name})
     * @param description optional human-readable description
     * @param executionKey routing key for the executor / agent
     * @param async whether the task runs asynchronously
     * @param dummy whether this is a no-op marker task
     * @param input opaque input payload (string, may carry JSON)
     * @param timeout optional timeout duration value
     * @param timeoutUnit name of the {@link java.util.concurrent.TimeUnit} for {@code timeout}
     * @param dependencies keys of tasks that must complete before this one runs
     */
    public record TaskDefinition(
        String key,
        String description,
        String executionKey,
        boolean async,
        boolean dummy,
        String input,
        Integer timeout,
        String timeoutUnit,
        Set<String> dependencies
    ) {}
}
