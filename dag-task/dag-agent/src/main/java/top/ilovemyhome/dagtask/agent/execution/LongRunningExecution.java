package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.util.Objects;

public class LongRunningExecution implements TaskExecution {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        Param param = input.getInputAs(Param.class);
        logger.info("Start execute taskId={}, param={}", taskId, param);
        if (Objects.isNull(param) || param.durationSeconds() <= 0) {
            throw new IllegalArgumentException("Input param is not correct! Required: durationSeconds > 0");
        }
        try {
            Thread.sleep(param.durationSeconds() * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("TaskId={} interrupted!", taskId);
            return TaskOutput.fail(taskId, null, "Interrupted");
        }
        logger.info("TaskId={} executed successfully!", taskId);
        return TaskOutput.success(taskId, "OK");
    }

    /**
     * Input parameter DTO for LongRunningExecution.
     */
    public record Param(long durationSeconds, String description) {
    }
}
