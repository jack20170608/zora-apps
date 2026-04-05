package top.ilovemyhome.dagtask.si;


public interface TaskOrderService {

    Long createOrder(TaskOrder order);

    boolean isOrdered(String orderKey);

    int updateOrderByKey(String orderKey, TaskOrder taskOrder);

    int deleteOrderByKey(String orderKey);


}
