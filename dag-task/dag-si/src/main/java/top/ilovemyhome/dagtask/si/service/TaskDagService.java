package top.ilovemyhome.dagtask.si.service;


import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.List;

public interface TaskDagService {

    //2.0 task record management
    boolean isSuccess(String orderKey);
    List<TaskRecord> findTaskByOrderKey(String orderKey);
    List<Long> getNextTaskIds(int count);
    List<Long> createTasks(List<TaskRecord> records);

    //3.0 runtime related
    void start(String orderKey);

    void receiveTaskEvent(Long taskId, TaskStatus newStatus, TaskOutput output);

    TaskOutput runNow(Long taskId, TaskInput input);

    //Force terminite the task and mark the task success
    void forceOk(Long taskId, TaskOutput output);

    //Force terminate the task and mark the task failure
    void kill(Long taskId);

    //Hold the task and don't trigger even condition is ready
    void hold(Long taskId);

}
