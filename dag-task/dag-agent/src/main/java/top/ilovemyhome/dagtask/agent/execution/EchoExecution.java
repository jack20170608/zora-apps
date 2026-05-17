package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

public class EchoExecution implements TaskExecution {

    private static final String MDC_TASK_ID = "taskId";
    private static final String MDC_TASK_NAME = "taskName";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public TaskOutput execute(TaskInput input) {
        Long taskId = input.taskId();
        String taskName = input.name();
        try {
            if (taskId != null) {
                MDC.put(MDC_TASK_ID, taskId.toString());
            }
            if (taskName != null) {
                MDC.put(MDC_TASK_NAME, taskName);
            }
            logger.info("executing echo task with input={}.", input);
            return TaskOutput.success(input.taskId(), input.input());
        } finally {
            MDC.remove(MDC_TASK_ID);
            MDC.remove(MDC_TASK_NAME);
        }
    }
}
