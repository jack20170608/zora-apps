package top.ilovemyhome.dagtask.agent.execution;

import org.apache.commons.lang3.ThreadUtils;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.time.Duration;

public class LongRunningExecution implements TaskExecution {

    @Override
    public TaskOutput execute(TaskInput input) {
        Long taskId = input.taskId();
        ThreadUtils.sleepQuietly(Duration.ofSeconds(10));
        return TaskOutput.success(taskId, "OK");
    }
}
