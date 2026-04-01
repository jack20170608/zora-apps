package top.ilovemyhome.dagtask.si;

import java.util.concurrent.ExecutorService;

public interface TaskContext<I,O> {

    TaskFactory getTaskFactory();
    TaskOrderDao getTaskOrderDao();
    void setTaskOrderDao(TaskOrderDao taskOrderDao);
    TaskRecordDao getTaskRecordDao();
    void setTaskRecordDao(TaskRecordDao taskRecordDao);

    ExecutorService getThreadPool();
    TaskDagService<I,O> getTaskDagService();
    void setTaskDagService(TaskDagService<I,O> taskDagService);
    void setTaskFactory(TaskFactory taskFactory);

    default String getTaskOrderTableName(){
        return DEFAULT_TASK_ORDER_TABLE_NAME;
    }

    default String getTaskTableName(){
        return DEFAULT_TASK_TABLE_NAME;
    }

    String DEFAULT_TASK_ORDER_TABLE_NAME = "t_task_order";
    String DEFAULT_TASK_TABLE_NAME = "t_task";

}
