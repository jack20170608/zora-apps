package top.ilovemyhome.dagtask.core.service;

import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.service.TaskQueryService;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static top.ilovemyhome.dagtask.si.Constants.MAX_QUERY_SIZE;

public class TaskQueryServiceImpl implements TaskQueryService {

    private final TaskRecordDao taskRecordDao;

    public TaskQueryServiceImpl(TaskRecordDao taskRecordDao) {
        this.taskRecordDao = taskRecordDao;
    }

    @Override
    public Optional<TaskRecord> getTask(Long taskId) {
        return taskRecordDao.loadTaskById(taskId);
    }

    @Override
    public List<TaskRecord> findAll(TaskRecordSearchCriteria criteria) {
        Objects.requireNonNull(criteria);
        int size = taskRecordDao.count(criteria);
        if (size > MAX_QUERY_SIZE){
            throw new IllegalArgumentException("Query result too large: " + size + ". Please refine your search criteria.");
        }
        return taskRecordDao.find(criteria);
    }


    @Override
    public Page<TaskRecord> find(TaskRecordSearchCriteria criteria, Pageable pageable) {
        Objects.requireNonNull(criteria);
        Objects.requireNonNull(pageable);
        return taskRecordDao.find(criteria, pageable);
    }
}
