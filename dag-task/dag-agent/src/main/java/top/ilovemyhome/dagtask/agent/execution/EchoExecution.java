package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

public class EchoExecution implements TaskExecution {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public TaskOutput doExecute(TaskInput input) {
        logger.info("executing echo task with input={}.", input);
        return TaskOutput.success(input.taskId(), input.input());
    }
}
