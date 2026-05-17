package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.util.Objects;

public class LongRunningExecution implements TaskExecution {

    private static final String MDC_TASK_ID = "taskId";
    private static final String MDC_TASK_NAME = "taskName";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public TaskOutput execute(TaskInput input) {
        Long taskId = input.taskId();
        String taskName = input.name();
        try {
            if (taskId != null) {
                MDC.put(MDC_TASK_ID, taskId.toString());
            }
            if (taskName != null) {
                MDC.put(MDC_TASK_NAME, taskName);
            }
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
        } finally {
            MDC.remove(MDC_TASK_ID);
            MDC.remove(MDC_TASK_NAME);
        }
    }

    /**
     * Input parameter DTO for LongRunningExecution.
     */
    public record Param(long durationSeconds, String description) {
    }
}
