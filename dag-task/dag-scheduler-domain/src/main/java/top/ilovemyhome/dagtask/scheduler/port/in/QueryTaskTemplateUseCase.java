package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.List;

/**
 * Read-side queries against the task template catalog.
 * <p>
 * Replaces {@code TaskTemplateService.findAll/find}.
 * </p>
 * <p>
 * Temporary compromise: still references {@code si.dto.TaskTemplateSearchCriteria}
 * pending later cleanup tasks.
 * </p>
 */
public interface QueryTaskTemplateUseCase {

    /** Return all templates matching the criteria (no pagination). */
    List<TaskTemplate> findAll(TaskTemplateSearchCriteria searchCriteria);

    // TODO(step-3): replace Pageable/Page with domain-owned page record; temporary leak from zora-jdbi.
    /** Return templates matching the criteria with pagination. */
    Page<TaskTemplate> find(TaskTemplateSearchCriteria searchCriteria, Pageable page);
}
