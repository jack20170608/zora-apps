package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.scheduler.domain.dag.DagDefinition;
import top.ilovemyhome.dagtask.si.TaskRecord;

import java.util.List;
import java.util.Map;

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
}
