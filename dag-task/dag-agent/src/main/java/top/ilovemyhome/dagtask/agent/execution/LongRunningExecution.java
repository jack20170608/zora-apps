package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskLogWriter;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.util.Objects;

public class LongRunningExecution implements TaskExecution {

    private static final Logger logger = LoggerFactory.getLogger(LongRunningExecution.class);

    @Override
    public TaskOutput execute(TaskInput input, TaskLogWriter logWriter) {
        Long taskId = input.taskId();
        try {
            Param param = input.getInputAs(Param.class);
            logger.info("Start execute taskId={}, param={}", taskId, param);
            if (Objects.isNull(param) || param.durationSeconds() <= 0) {
                throw new IllegalArgumentException("Input param is not correct! Required: durationSeconds > 0");
            }
            Thread.sleep(param.durationSeconds() * 1000L);
            logger.info("TaskId={} executed successfully!", taskId);
            return TaskOutput.success(taskId, "OK");
        } catch (Throwable t) {
            logger.info("TaskId={} executed failure!", taskId);
            logger.warn(t.getMessage(), t);
            return TaskOutput.fail(taskId, null, t.getMessage());
        }
    }

    /**
     * Input parameter DTO for LongRunningExecution.
     */
    public record Param(long durationSeconds, String description) {
    }
}
