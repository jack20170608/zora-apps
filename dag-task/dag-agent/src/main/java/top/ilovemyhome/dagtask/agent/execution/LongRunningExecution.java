package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.util.Map;
import java.util.Objects;

public class LongRunningExecution implements TaskExecution {

    @Override
    public TaskOutput execute(TaskInput input) {
        Long taskId = input.taskId();
        try {
            Map<String, Object> param = (Map<String, Object>)input.input();
            if (Objects.isNull(param) || !param.containsKey("durationSeconds") || param.get("durationSeconds") == null) {
                throw new IllegalArgumentException("Input param is not correct!");
            }
            Thread.sleep(p.durationSeconds);
            return TaskOutput.success(taskId, "OK");
        }catch (Throwable t){
            logger.warn(t.getMessage(), t);
            return TaskOutput.fail(taskId, null,  t.getMessage());
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(LongRunningExecution.class);

    record Param(long durationSeconds, String description){
    }
}
