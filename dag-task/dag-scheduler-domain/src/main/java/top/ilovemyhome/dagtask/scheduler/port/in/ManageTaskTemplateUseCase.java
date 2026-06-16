package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.scheduler.application.TemplateNotFoundException;
import top.ilovemyhome.dagtask.si.TaskTemplate;

/**
 * Write-side operations on stored task templates.
 * <p>
 * Replaces {@code TaskTemplateService.createTemplate/updateTemplate/deactivateVersion/deleteVersion}.
 * Read-side operations live in {@link QueryTaskTemplateUseCase}.
 * </p>
 */
public interface ManageTaskTemplateUseCase {

    /**
     * Persist a new template version.
     *
     * @param template the template definition
     * @param setActive whether to mark this version as the active one
     * @return {@code true} if created, {@code false} if a template with same
     *         {@code templateKey + version} already exists (caller should retry with a different version)
     */
    boolean createTemplate(TaskTemplate template, boolean setActive);

    /**
     * Update an existing template.
     *
     * @return {@code true} if updated, {@code false} on optimistic-lock conflict
     *         (caller should refresh and retry)
     * @throws TemplateNotFoundException if no template exists with the given key + version
     */
    boolean updateTemplate(TaskTemplate template);

    /**
     * Mark a specific template version as inactive without deleting it.
     *
     * @return {@code true} if the version was deactivated
     * @throws TemplateNotFoundException if no template version matches
     */
    boolean deactivateVersion(String templateKey, String version);

    /**
     * Permanently delete a specific template version.
     * <p>
     * Lenient semantics: no exception is thrown if the version is already absent.
     * </p>
     *
     * @return {@code true} if deleted, {@code false} if no such version existed (no-op)
     */
    boolean deleteVersion(String templateKey, String version);
}
