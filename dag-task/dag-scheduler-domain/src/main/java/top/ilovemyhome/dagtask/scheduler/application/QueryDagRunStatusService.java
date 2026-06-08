package top.ilovemyhome.dagtask.scheduler.application;

import top.ilovemyhome.dagtask.scheduler.port.out.TaskRecordRepository;

import java.util.Objects;

/**
 * Application service for querying DAG run completion status.
 * <p>
 * Replaces the legacy {@code TaskDagService.isSuccess}.
 * </p>
 */
public class QueryDagRunStatusService implements top.ilovemyhome.dagtask.scheduler.port.in.QueryDagRunStatusUseCase {

    private final TaskRecordRepository taskRecordRepository;

    public QueryDagRunStatusService(TaskRecordRepository taskRecordRepository) {
        this.taskRecordRepository = Objects.requireNonNull(taskRecordRepository, "taskRecordRepository must not be null");
    }

    @Override
    public boolean isSuccess(String orderKey) {
        Objects.requireNonNull(orderKey, "orderKey must not be null");
        boolean ordered = taskRecordRepository.isOrdered(orderKey);
        if (!ordered) {
            return false;
        }
        return taskRecordRepository.isSuccess(orderKey);
    }
}
