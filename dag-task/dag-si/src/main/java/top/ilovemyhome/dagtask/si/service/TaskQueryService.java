package top.ilovemyhome.dagtask.si.service;

import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Task Query Service - Provides query capabilities for task management.
 * Primarily used by management interfaces and monitoring.
 */
public interface TaskQueryService {


    Optional<TaskRecord> getTask(Long taskId);


    List<TaskRecord> findAll(TaskRecordSearchCriteria criteria);


    /**
     * Search tasks by dynamic criteria with pagination.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @return page of matching tasks
     */
    Page<TaskRecord> find(TaskRecordSearchCriteria criteria, Pageable pageable);
}
