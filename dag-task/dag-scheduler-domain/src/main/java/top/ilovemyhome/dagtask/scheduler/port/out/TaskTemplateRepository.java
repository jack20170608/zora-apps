package top.ilovemyhome.dagtask.scheduler.port.out;

import top.ilovemyhome.dagtask.si.TaskTemplate;

/**
 * Outbound port for {@link TaskTemplate} persistence. Implementations live in
 * dag-scheduler-adapter-persistence-jdbc (or any other persistence adapter).
 */
public interface TaskTemplateRepository {

    /**
     * Deactivate a specific template version.
     * This prevents the version from being used for new instantiations.
     *
     * @param templateKey the template business key
     * @param version     the version to deactivate
     * @return number of rows affected
     */
    int deactivateVersion(String templateKey, String version);

    /**
     * Deactivate all versions of a template except the specified one.
     * Used to mark a specific version as the only active one.
     *
     * @param templateKey   the template business key
     * @param activeVersion the version that should remain active
     * @return number of rows affected
     */
    int deactivateOtherVersions(String templateKey, String activeVersion);

    /**
     * Delete a specific template version.
     *
     * @param templateKey the template business key
     * @param version     the version to delete
     * @return number of rows affected
     */
    int deleteByKeyAndVersion(String templateKey, String version);

    /**
     * Check if a template version with the given key and version already exists.
     *
     * @param templateKey the template business key
     * @param version     the version string
     * @return true if exists, false otherwise
     */
    boolean exists(String templateKey, String version);
}
