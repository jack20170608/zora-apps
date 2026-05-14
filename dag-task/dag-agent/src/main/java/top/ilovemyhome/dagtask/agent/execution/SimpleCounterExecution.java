package top.ilovemyhome.dagtask.agent.execution;

import org.apache.commons.lang3.IntegerRange;
import org.apache.commons.lang3.ThreadUtils;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.time.Duration;
import java.util.Objects;


public class SimpleCounterExecution extends AbstractTaskExecution {


    @Override
    protected TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        String name = input.name();
        try {
            SimpleCounterExecution.Param param = input.getInputAs(SimpleCounterExecution.Param.class);
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
        } catch (Throwable t) {
            logger.info("TaskId={} executed failure!", taskId);
            logger.warn(t.getMessage(), t);
            return TaskOutput.fail(taskId, null, t.getMessage());
        }
    }

    public record Param(int from, int to, long intervalMillisecond) {
    }
}
