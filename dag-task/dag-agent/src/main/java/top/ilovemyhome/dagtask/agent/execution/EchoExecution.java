package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskLogWriter;
import top.ilovemyhome.dagtask.si.TaskOutput;

public class EchoExecution implements TaskExecution {

    private static final Logger logger = LoggerFactory.getLogger(EchoExecution.class);

    @Override
    public TaskOutput execute(TaskInput input, TaskLogWriter logWriter) {
        logger.info("executing echo task with input={}.", input);
        return TaskOutput.success(input.taskId(), input.input());
    }
}
