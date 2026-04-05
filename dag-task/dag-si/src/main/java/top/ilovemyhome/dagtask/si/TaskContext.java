package top.ilovemyhome.dagtask.si;

import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;

import java.util.concurrent.ExecutorService;

public interface TaskContext {

    TaskFactory getTaskFactory();
    TaskOrderDao getTaskOrderDao();
    void setTaskOrderDao(TaskOrderDao taskOrderDao);
    TaskRecordDao getTaskRecordDao();
    void setTaskRecordDao(TaskRecordDao taskRecordDao);

    ExecutorService getThreadPool();
    TaskDagService getTaskDagService();
    void setTaskDagService(TaskDagService taskDagService);
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
