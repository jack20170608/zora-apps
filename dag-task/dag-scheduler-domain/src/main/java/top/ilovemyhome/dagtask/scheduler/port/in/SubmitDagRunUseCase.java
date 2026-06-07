package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.si.TaskRecord;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Submit a new DAG run.
 * <p>
 * Replaces {@code DagManageService.createTasks} / {@code DagManageService.createTasksFromDagDefinition}
 * and {@code TaskDagService.createTasks} (the latter two had identical signatures and are merged here).
 * </p>
 * <p>
 * Note: legacy {@code createTasksFromDagDefinition(String json, ...)} took a raw JSON string and
 * threw {@code JsonProcessingException}; that responsibility now belongs to the inbound web adapter
 * which parses JSON into the {@link DagDefinition} record before invoking
 * {@link #submitFromDefinition(DagDefinition, String, Map)}.
 * </p>
 */
public interface SubmitDagRunUseCase {

    /** Submit pre-built task records (caller has already built the DAG). */
    List<Long> submitTasks(List<TaskRecord> records);

    /**
     * Submit from a parsed DAG definition. JSON parsing is the web adapter's job.
     *
     * @param definition the parsed DAG definition
     * @param orderKey the unique order key for this DAG run
     * @param parameters substitution parameters applied to task fields (placeholder {@code {{name}}})
     * @return list of newly created task record IDs
     */
    List<Long> submitFromDefinition(DagDefinition definition, String orderKey,
                                    Map<String, String> parameters);

    /**
     * Domain-neutral DAG definition. Replaces JSON-string input from legacy API.
     */
    record DagDefinition(String name, List<TaskDefinition> tasks) {}

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
    record TaskDefinition(
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
