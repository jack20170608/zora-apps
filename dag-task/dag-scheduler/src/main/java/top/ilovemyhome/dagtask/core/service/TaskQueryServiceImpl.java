package top.ilovemyhome.dagtask.core.service;

import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.service.TaskQueryService;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.List;
import java.util.Objects;

public class TaskQueryServiceImpl implements TaskQueryService {

    private final TaskRecordDao taskRecordDao;

    public TaskQueryServiceImpl(TaskRecordDao taskRecordDao) {
        this.taskRecordDao = taskRecordDao;
    }

    @Override
    public List<TaskRecord> findByOrderKey(String orderKey) {
        Objects.requireNonNull(orderKey);
        return taskRecordDao.findByOrderKey(orderKey);
    }

    @Override
    public List<TaskRecord> findByStatus(TaskStatus status) {
        Objects.requireNonNull(status);
        return taskRecordDao.findByStatus(status);
    }

    @Override
    public Page<TaskRecord> search(TaskRecordSearchCriteria criteria, Pageable pageable) {
        Objects.requireNonNull(criteria);
        Objects.requireNonNull(pageable);
        return taskRecordDao.search(criteria, pageable);
    }
}
