package top.ilovemyhome.dagtask.si.service;

import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.List;

/**
 * Task Query Service - Provides query capabilities for task management.
 * Primarily used by management interfaces and monitoring.
 */
public interface TaskQueryService {

    /**
     * Find all tasks by order key.
     *
     * @param orderKey the order key
     * @return list of matching tasks
     */
    List<TaskRecord> findByOrderKey(String orderKey);

    /**
     * Find all tasks with a specific status.
     *
     * @param status the task status
     * @return list of matching tasks
     */
    List<TaskRecord> findByStatus(TaskStatus status);

    /**
     * Search tasks by dynamic criteria with pagination.
     *
     * @param criteria the search criteria
     * @param pageable pagination information
     * @return page of matching tasks
     */
    Page<TaskRecord> search(TaskRecordSearchCriteria criteria, Pageable pageable);
}
