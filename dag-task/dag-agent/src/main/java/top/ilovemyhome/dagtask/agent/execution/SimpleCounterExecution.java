package top.ilovemyhome.dagtask.agent.execution;

import org.apache.commons.lang3.IntegerRange;
import org.apache.commons.lang3.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.time.Duration;
import java.util.Objects;


public class SimpleCounterExecution implements TaskExecution {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        String name = input.name();
        Param param = input.getInputAs(Param.class);
        logger.info("Start execute taskId={}, name={}, param={}", taskId, name, param);
        if (Objects.isNull(param) || param.from() >= param.to || param.intervalMillisecond < 1L) {
            throw new IllegalArgumentException("Input param is not correct! Required: from <= to and intervalMillisecond > 1");
        }
        IntegerRange.of(param.from, param.to).toIntStream().boxed().forEach(i -> {
            logger.info("Counting={}/{}.", i, param.to);
            ThreadUtils.sleepQuietly(Duration.ofMillis(param.intervalMillisecond));
        });
        logger.info("TaskId={} executed successfully!", taskId);
        return TaskOutput.success(taskId, "OK");
    }

    public record Param(int from, int to, long intervalMillisecond) {
    }
}
