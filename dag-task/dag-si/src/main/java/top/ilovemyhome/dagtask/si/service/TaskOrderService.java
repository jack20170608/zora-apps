package top.ilovemyhome.dagtask.si.service;


import top.ilovemyhome.dagtask.si.TaskOrder;

public interface TaskOrderService {

    Long createOrder(TaskOrder order);

    boolean isOrdered(String orderKey);

    int updateOrderByKey(String orderKey, TaskOrder taskOrder);

    int deleteOrderByKey(String orderKey);


}
