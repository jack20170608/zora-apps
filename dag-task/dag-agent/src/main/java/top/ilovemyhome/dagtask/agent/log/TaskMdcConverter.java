package top.ilovemyhome.dagtask.agent.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Map;

/**
 * Logback pattern converter that prints task MDC values only when set.
 *
 * <p>Outputs {@code [taskId][taskName]} when present, otherwise empty string.
 * Used in logback pattern to avoid printing empty brackets or extra spaces.</p>
 */
public class TaskMdcConverter extends ClassicConverter {

    private static final String MDC_TASK_ID = "taskId";
    private static final String MDC_TASK_NAME = "taskName";

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        String taskId = mdc.get(MDC_TASK_ID);
        String taskName = mdc.get(MDC_TASK_NAME);

        if (taskId == null && taskName == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        if (taskId != null) {
            sb.append("[").append(taskId).append("]");
        }
        if (taskName != null) {
            sb.append("[").append(taskName).append("]");
        }
        return sb.toString();
    }
}
