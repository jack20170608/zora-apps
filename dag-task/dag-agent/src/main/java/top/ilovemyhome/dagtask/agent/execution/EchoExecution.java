package top.ilovemyhome.dagtask.agent.execution;

import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

public class EchoExecution extends AbstractTaskExecution {

    @Override
    protected TaskOutput doExecute(TaskInput input) {
        logger.info("executing echo task with input={}.", input);
        return TaskOutput.success(input.taskId(), input.input());
    }
}
