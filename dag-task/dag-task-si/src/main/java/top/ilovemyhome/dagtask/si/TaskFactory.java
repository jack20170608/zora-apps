package top.ilovemyhome.dagtask.si;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface TaskFactory {

    Logger LOGGER = LoggerFactory.getLogger(TaskFactory.class);

    default <I,O> TaskExecution<I,O> createTaskForExecution(String executionKey){
        TaskExecution<I, O> result = null;
        try {
            Class executionClass = Class.forName(executionKey);
            result = (TaskExecution<I, O>)executionClass.newInstance();
        } catch (Throwable t){
            LOGGER.error(t.getMessage(), t);
        }
        return result;
    }
}
