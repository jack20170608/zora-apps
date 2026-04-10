package top.ilovemyhome.dagtask.si.service;

import top.ilovemyhome.dagtask.si.TaskRecord;

import java.util.List;

/**
 * DAG Manager - Manages DAG definition and task creation.
 * Responsible for:
 * <ul>
 *     <li>Creating and importing DAG task definitions</li>
 *     <li>DAG validation (cycle detection)</li>
 *     <li>Querying DAG structure and status</li>
 * </ul>
 */
public interface DagManager {

    /**
     * Get next N task IDs for batch creation.
     *
     * @param count number of IDs to get
     * @return list of next available task IDs
     */
    List<Long> getNextTaskIds(int count);

    /**
     * Create multiple tasks as a single DAG.
     * Validates:
     * <ul>
     *     <li>All tasks have the same order key</li>
     *     <li>No duplicate task IDs</li>
     *     <li>No cycles in the DAG after adding new tasks</li>
     * </ul>
     *
     * @param records list of task records to create
     * @return list of created task IDs
     */
    List<Long> createTasks(List<TaskRecord> records);

    /**
     * Find all tasks belonging to a specific order key.
     *
     * @param orderKey the order key to search
     * @return list of all tasks for this order
     */
    List<TaskRecord> findTasksByOrderKey(String orderKey);

    /**
     * Check if an order with given key exists.
     *
     * @param orderKey the order key to check
     * @return true if order exists
     */
    boolean exists(String orderKey);

    /**
     * Check if all tasks in the DAG are completed successfully.
     *
     * @param orderKey the order key to check
     * @return true if all tasks are SUCCESS
     */
    boolean isAllSuccess(String orderKey);

    /**
     * Count tasks by status for a specific order.
     *
     * @param orderKey the order key
     * @return count of tasks per status
     */
    java.util.Map<top.ilovemyhome.dagtask.si.enums.TaskStatus, Long> countByStatus(String orderKey);

    /**
     * Create all TaskRecords from a parsed template DAG definition.
     * Parses the JSON DAG definition, applies parameter substitution,
     * and creates all task records in the database with proper dependency relationships.
     *
     * @param dagDefinitionJson the JSON DAG definition from the template
     * @param orderKey the parent order key that all tasks belong to
     * @param parameters resolved parameters for substitution
     * @throws com.fasterxml.jackson.core.JsonProcessingException if the JSON is invalid
     */
    void createTasksFromDagDefinition(String dagDefinitionJson, String orderKey,
                                       java.util.Map<String, String> parameters)
            throws com.fasterxml.jackson.core.JsonProcessingException;

    /**
     * Instantiate a concrete {@link top.ilovemyhome.dagtask.si.TaskOrder} from an active template.
     * Uses the template's DAG definition and applies the provided parameter
     * values to create a concrete task order ready for execution.
     *
     * @param templateKey the template business key (uses active version)
     * @param orderKey the business key for the new task order
     * @param orderName the name for the new task order
     * @param parameters parameter values to inject into the template
     * @return the instantiated task order, empty if template not found
     */
    java.util.Optional<top.ilovemyhome.dagtask.si.TaskOrder> instantiateFromTemplate(
            String templateKey, String orderKey, String orderName,
            java.util.Map<String, String> parameters);

    /**
     * Instantiate a concrete {@link top.ilovemyhome.dagtask.si.TaskOrder} from a specific template version.
     *
     * @param templateKey the template business key
     * @param version the specific template version to use
     * @param orderKey the business key for the new task order
     * @param orderName the name for the new task order
     * @param parameters parameter values to inject into the template
     * @return the instantiated task order, empty if template not found
     */
    java.util.Optional<top.ilovemyhome.dagtask.si.TaskOrder> instantiateFromTemplate(
            String templateKey, String version, String orderKey, String orderName,
            java.util.Map<String, String> parameters);
}
