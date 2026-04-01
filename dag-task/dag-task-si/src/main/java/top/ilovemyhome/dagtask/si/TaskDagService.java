package top.ilovemyhome.dagtask.si;


import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.List;

public interface TaskDagService<I,O> {
    //1.0 task order management
    boolean isOrdered(String orderKey);
    boolean isSuccess(String orderKey);
    List<TaskRecord> findTaskByOrderKey(String orderKey);

    //2.1 task order management
    Long createOrder(TaskOrder order);
    int updateOrderByKey(String orderKey, TaskOrder taskOrder);
    int deleteOrderByKey(String orderKey, boolean caseCade);

    //2.0 task record management
    List<Long> getNextTaskIds(int count);
    List<Long> createTasks(List<TaskRecord> records);

    TaskOutput<O> runTask(Long taskId, TaskInput<I> input);

    //3.0 runtime related
    void start(String orderKey);

    void receiveTaskEvent(Long taskId, TaskStatus newStatus, TaskOutput<O> output);

}
