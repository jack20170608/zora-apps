package top.ilovemyhome.dagtask.agent.execution;

import org.apache.commons.lang3.IntegerRange;
import org.apache.commons.lang3.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.time.Duration;
import java.util.Objects;


public class SimpleCounterExecution implements TaskExecution {

    private static final String MDC_TASK_ID = "taskId";
    private static final String MDC_TASK_NAME = "taskName";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public TaskOutput execute(TaskInput input) {
        Long taskId = input.taskId();
        String name = input.name();
        try {
            if (taskId != null) {
                MDC.put(MDC_TASK_ID, taskId.toString());
            }
            if (name != null) {
                MDC.put(MDC_TASK_NAME, name);
            }
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
        } catch (Throwable t) {
            logger.info("TaskId={} executed failure!", taskId);
            logger.warn(t.getMessage(), t);
            return TaskOutput.fail(taskId, null, t.getMessage());
        } finally {
            MDC.remove(MDC_TASK_ID);
            MDC.remove(MDC_TASK_NAME);
        }
    }

    public record Param(int from, int to, long intervalMillisecond) {
    }
}
