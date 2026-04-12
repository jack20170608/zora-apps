package top.ilovemyhome.dagtask.si.agent;

import top.ilovemyhome.dagtask.si.TaskExecution;

public class TaskFactory {

    public static TaskExecution createTaskForExecution(String executionKey){
        TaskExecution result = null;
        try {
            Class executionClass = Class.forName(executionKey);
            result = (TaskExecution)executionClass.newInstance();
        } catch (Throwable t){
            throw new IllegalArgumentException("Invalid execution key: " + executionKey);
        }
        return result;
    }
}
