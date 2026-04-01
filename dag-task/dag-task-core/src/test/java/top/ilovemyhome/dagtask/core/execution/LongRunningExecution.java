package top.ilovemyhome.dagtask.core.execution;

import org.apache.commons.lang3.ThreadUtils;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.time.Duration;

public class LongRunningExecution implements TaskExecution<String, String> {

    @Override
    public TaskOutput<String> execute(TaskInput<String> input) {
        String in = input.input();
        Long taskId = input.taskId();
        ThreadUtils.sleepQuietly(Duration.ofSeconds(2));
        return TaskOutput.success(taskId, in + "->" + getClass().getSimpleName());
    }
}
