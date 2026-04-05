package top.ilovemyhome.dagtask.si;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public interface TaskFactory {

    Logger LOGGER = LoggerFactory.getLogger(TaskFactory.class);

    default TaskExecution createTaskForExecution(String executionKey){
        TaskExecution result = null;
        try {
            Class executionClass = Class.forName(executionKey);
            result = (TaskExecution)executionClass.newInstance();
        } catch (Throwable t){
            LOGGER.error(t.getMessage(), t);
        }
        return result;
    }
}
