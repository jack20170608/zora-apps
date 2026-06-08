package top.ilovemyhome.dagtask.scheduler.port.out;

import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.List;

/**
 * Outbound port for {@link TaskTemplate} persistence. Implementations live in
 * dag-scheduler-adapter-persistence-jdbc (or any other persistence adapter).
 */
public interface TaskTemplateRepository {

    /** Create a new template and return its generated ID. */
    Long create(TaskTemplate template);

    /**
     * Update an existing template by ID.
     *
     * @param id       the template ID to update
     * @param template the updated fields
     * @return number of rows updated (1 if success, 0 if not found)
     */
    int update(Long id, TaskTemplate template);

    /** Find templates matching the given criteria without pagination. */
    List<TaskTemplate> find(TaskTemplateSearchCriteria criteria);

    /**
     * Find templates matching the given criteria with pagination.
     * Page/Pageable types are a temporary leak from zora-jdbi (TD-1);
     * will be replaced with domain-owned types in step 3.
     */
    Page<TaskTemplate> find(TaskTemplateSearchCriteria criteria, Pageable pageable);

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
