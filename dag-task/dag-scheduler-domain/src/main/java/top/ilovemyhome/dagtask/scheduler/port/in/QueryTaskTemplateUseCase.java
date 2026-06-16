package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.scheduler.domain.query.Page;
import top.ilovemyhome.dagtask.scheduler.domain.query.Pageable;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;

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

    /** Return templates matching the criteria with pagination. */
    Page<TaskTemplate> find(TaskTemplateSearchCriteria searchCriteria, Pageable page);
}
