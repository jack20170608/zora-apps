package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for {@link TaskTemplate} persistence operations.
 * <p>
 * Provides methods for querying templates by key, version, and activation status.
 * </p>
 */
public interface TaskTemplateDao extends BaseDao<TaskTemplate> {

    /**
     * Find all versions of a template by its template key.
     *
     * @param templateKey the template business key
     * @return list of all versions ordered by version descending
     */
    List<TaskTemplate> findByTemplateKey(String templateKey);

    /**
     * Find a specific version of a template by template key and version string.
     *
     * @param templateKey the template business key
     * @param version the semantic version string
     * @return the template if found, empty otherwise
     */
    Optional<TaskTemplate> findByKeyAndVersion(String templateKey, String version);

    /**
     * Find the currently active (latest/recommended) version of a template.
     *
     * @param templateKey the template business key
     * @return the active template if found, empty otherwise
     */
    Optional<TaskTemplate> findActiveByTemplateKey(String templateKey);

    /**
     * Find all active templates (at least one active version across all templates).
     * Returns only the active version for each distinct template key.
     *
     * @return list of active templates
     */
    List<TaskTemplate> findAllActive();

    /**
     * Find all templates across all versions (including inactive).
     *
     * @return list of all template versions
     */
    List<TaskTemplate> findAll();

    /**
     * Deactivate a specific template version.
     * This prevents the version from being used for new instantiations.
     *
     * @param templateKey the template business key
     * @param version the version to deactivate
     * @return number of rows affected
     */
    int deactivateVersion(String templateKey, String version);

    /**
     * Deactivate all versions of a template except the specified one.
     * Used to mark a specific version as the only active one.
     *
     * @param templateKey the template business key
     * @param activeVersion the version that should remain active
     * @return number of rows affected
     */
    int deactivateOtherVersions(String templateKey, String activeVersion);

    /**
     * Delete a specific template version.
     *
     * @param templateKey the template business key
     * @param version the version to delete
     * @return number of rows affected
     */
    int deleteByKeyAndVersion(String templateKey, String version);

    /**
     * Check if a template version with the given key and version already exists.
     *
     * @param templateKey the template business key
     * @param version the version string
     * @return true if exists, false otherwise
     */
    boolean exists(String templateKey, String version);
}