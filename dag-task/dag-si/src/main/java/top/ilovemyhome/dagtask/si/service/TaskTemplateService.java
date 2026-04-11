package top.ilovemyhome.dagtask.si.service;

import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing DAG task templates.
 * <p>
 * Provides business logic for template management including versioning,
 * activation/deactivation, and instantiation from templates to create concrete
 * {@link TaskOrder} instances.
 * </p>
 */
public interface TaskTemplateService {

    /**
     * Create a new version of a template.
     * <p>
     * If the template key doesn't exist, creates a new template with the first version.
     * If the template key already exists with this version, creation fails.
     * After creation, optionally deactivates other versions.
     * </p>
     *
     * @param template the template version to create
     * @param setActive whether to set this as the only active version
     * @return true if creation succeeded, false if version already exists
     */
    boolean createTemplate(TaskTemplate template, boolean setActive);

    /**
     * Update an existing template version.
     *
     * @param template the updated template
     * @return true if update succeeded, false if not found
     */
    boolean updateTemplate(TaskTemplate template);

    /**
     * Deactivate a specific template version.
     * Deactivated versions cannot be used for instantiation.
     *
     * @param templateKey the template business key
     * @param version the version to deactivate
     * @return true if deactivation succeeded, false if not found
     */
    boolean deactivateVersion(String templateKey, String version);

    /**
     * Delete a specific template version.
     *
     * @param templateKey the template business key
     * @param version the version to delete
     * @return true if deletion succeeded, false if not found
     */
    boolean deleteVersion(String templateKey, String version);

    List<TaskTemplate> findAll(TaskTemplateSearchCriteria searchCriteria);

    Page<TaskTemplate> find(TaskTemplateSearchCriteria searchCriteria, Pageable page);
}
