package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.si.TaskOrder;

import java.util.Map;
import java.util.Optional;

/**
 * Instantiate a concrete DAG run from a stored template definition.
 * <p>
 * Replaces {@code DagManageService.instantiateFromTemplate} (both overloads).
 * </p>
 */
public interface InstantiateDagTemplateUseCase {

    /**
     * Instantiate from the currently active version of the named template.
     *
     * @param templateKey template identifier
     * @param orderKey new order key for the run
     * @param orderName human-readable order name
     * @param parameters substitution parameters merged with the template defaults
     * @return the new {@link TaskOrder} if successful
     */
    Optional<TaskOrder> instantiateFromTemplate(
        String templateKey, String orderKey, String orderName,
        Map<String, String> parameters);

    /**
     * Instantiate from a specific template version.
     *
     * @param templateKey template identifier
     * @param version explicit version string ({@code null} selects active)
     * @param orderKey new order key for the run
     * @param orderName human-readable order name
     * @param parameters substitution parameters merged with the template defaults
     * @return the new {@link TaskOrder} if successful
     */
    Optional<TaskOrder> instantiateFromTemplate(
        String templateKey, String version, String orderKey, String orderName,
        Map<String, String> parameters);
}
