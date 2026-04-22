package top.ilovemyhome.dagtask.si.service;


import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.List;

/**
 * DAG task management service - responsible for DAG task record lifecycle management.
 * <p>
 * This interface focuses on task record CRUD and query operations.
 * Runtime scheduling operations (start, runNow, forceOk, kill, hold) are handled by
 * {@link DagScheduleService}.
 * </p>
 */
public interface TaskDagService {

    // 1.0 query related
    List<TaskRecord> findByOrderKey(String orderKey);

    List<TaskRecord> findByStatus(TaskStatus status);

    // 2.0 task record management
    boolean isSuccess(String orderKey);

    List<TaskRecord> findTaskByOrderKey(String orderKey);

    List<Long> getNextTaskIds(int count);

    List<Long> createTasks(List<TaskRecord> records);

}
