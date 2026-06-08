package top.ilovemyhome.dagtask.scheduler.application;

import top.ilovemyhome.dagtask.scheduler.port.out.TaskRecordRepository;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static top.ilovemyhome.dagtask.si.Constants.MAX_QUERY_SIZE;

/**
 * Application service for querying DAG task records.
 * <p>
 * Replaces the legacy {@code DagQueryService.getTask/findAll/find} and the
 * search-related methods of {@code TaskDagService.findByOrderKey/findTaskByOrderKey/findByStatus}.
 * </p>
 */
public class QueryDagTasksService implements top.ilovemyhome.dagtask.scheduler.port.in.QueryDagTasksUseCase {

    private final TaskRecordRepository taskRecordRepository;

    public QueryDagTasksService(TaskRecordRepository taskRecordRepository) {
        this.taskRecordRepository = Objects.requireNonNull(taskRecordRepository, "taskRecordRepository must not be null");
    }

    @Override
    public Optional<TaskRecord> getTask(Long taskId) {
        return taskRecordRepository.loadTaskById(taskId);
    }

    @Override
    public List<TaskRecord> findAll(TaskRecordSearchCriteria criteria) {
        Objects.requireNonNull(criteria);
        int size = taskRecordRepository.count(criteria);
        if (size > MAX_QUERY_SIZE) {
            throw new IllegalArgumentException("Query result too large: " + size + ". Please refine your search criteria.");
        }
        return taskRecordRepository.find(criteria);
    }

    @Override
    public Page<TaskRecord> find(TaskRecordSearchCriteria criteria, Pageable pageable) {
        Objects.requireNonNull(criteria);
        Objects.requireNonNull(pageable);
        return taskRecordRepository.find(criteria, pageable);
    }

    @Override
    public List<TaskRecord> findByOrderKey(String orderKey) {
        Objects.requireNonNull(orderKey);
        TaskRecordSearchCriteria criteria = TaskRecordSearchCriteria.builder()
            .withOrderKey(orderKey)
            .build();
        return taskRecordRepository.find(criteria);
    }

    @Override
    public List<TaskRecord> findTaskByOrderKey(String orderKey) {
        Objects.requireNonNull(orderKey);
        TaskRecordSearchCriteria criteria = TaskRecordSearchCriteria.builder()
            .withOrderKey(orderKey)
            .build();
        // Legacy parity: this overload calls search() (not find()); both are kept on
        // the repository port so DaoJdbiImpl can satisfy them with a single method.
        return taskRecordRepository.search(criteria);
    }

    @Override
    public List<TaskRecord> findByStatus(TaskStatus status) {
        Objects.requireNonNull(status);
        TaskRecordSearchCriteria criteria = TaskRecordSearchCriteria.builder()
            .withStatus(status)
            .build();
        return taskRecordRepository.find(criteria);
    }
}
