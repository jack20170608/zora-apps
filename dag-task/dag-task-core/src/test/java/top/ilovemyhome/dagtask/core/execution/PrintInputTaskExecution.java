package top.ilovemyhome.dagtask.core.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

public class PrintInputTaskExecution implements TaskExecution<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(PrintInputTaskExecution.class);

    @Override
    public TaskOutput<String> execute(TaskInput<String> input) {
        logger.info("Input is [{}].", input);
        String in = input.input();
        return TaskOutput.success(input.taskId(), in + "->" + getClass().getSimpleName());
    }


}
