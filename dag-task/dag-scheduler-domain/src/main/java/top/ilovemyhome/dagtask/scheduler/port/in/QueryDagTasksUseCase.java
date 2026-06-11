package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.scheduler.domain.query.Page;
import top.ilovemyhome.dagtask.scheduler.domain.query.Pageable;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.List;
import java.util.Optional;

/**
 * Query DAG task records.
 * <p>
 * Replaces {@code DagQueryService.getTask/findAll/find} and
 * {@code TaskDagService.findByOrderKey/findTaskByOrderKey/findByStatus}.
 * </p>
 * <p>
 * Temporary compromise: still references {@code si.dto.TaskRecordSearchCriteria} pending
 * later cleanup tasks.
 * </p>
 */
public interface QueryDagTasksUseCase {

    /** Look up a single task by its primary id. */
    Optional<TaskRecord> getTask(Long taskId);

    /** Find all tasks matching the supplied criteria (no pagination). */
    List<TaskRecord> findAll(TaskRecordSearchCriteria criteria);

    /** Find tasks matching the supplied criteria with pagination. */
    Page<TaskRecord> find(TaskRecordSearchCriteria criteria, Pageable pageable);

    /** Return all tasks for the given order key. */
    List<TaskRecord> findByOrderKey(String orderKey);

    /**
     * Same as {@link #findByOrderKey(String)} — preserved for parity with
     * legacy {@code TaskDagService.findTaskByOrderKey} until callers are migrated.
     */
    List<TaskRecord> findTaskByOrderKey(String orderKey);

    /** Return all tasks currently in the given status across all orders. */
    List<TaskRecord> findByStatus(TaskStatus status);
}
