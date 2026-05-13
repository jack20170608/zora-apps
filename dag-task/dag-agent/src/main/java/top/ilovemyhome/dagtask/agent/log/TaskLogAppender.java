package top.ilovemyhome.dagtask.agent.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import top.ilovemyhome.dagtask.si.TaskLogWriter;

/**
 * Logback appender that forwards log events to the {@link TaskLogWriter}
 * bound to the current thread via {@link TaskLogContext}.
 *
 * <p>Only events at INFO level or higher are written; DEBUG and TRACE are
 * ignored to keep per-task logs concise. If no writer is bound to the current
 * thread the event is silently dropped.
 */
public class TaskLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent event) {
        TaskLogWriter writer = TaskLogContext.get();
        if (writer == null) {
            return;
        }

        int levelInt = event.getLevel().toInt();
        if (levelInt < Level.INFO_INT) {
            // DEBUG / TRACE — skip to avoid noise in per-task logs
            return;
        }

        String message = formatMessage(event);
        switch (levelInt) {
            case Level.INFO_INT:
                writer.info(message);
                break;
            case Level.WARN_INT:
                writer.warn(message);
                break;
            case Level.ERROR_INT:
                writer.error(message);
                break;
            default:
                // Unknown level — treat as info
                writer.info(message);
        }
    }

    private String formatMessage(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(event.getLoggerName()).append("] ")
            .append(event.getFormattedMessage());

        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            sb.append('\n');
            sb.append(tp.getClassName());
            sb.append(": ").append(tp.getMessage()).append('\n');
            appendStackTrace(sb, tp.getStackTraceElementProxyArray());
            while (tp.getCause() != null) {
                tp = tp.getCause();
                sb.append("Caused by: ");
                sb.append(tp.getClassName());
                sb.append(": ").append(tp.getMessage()).append('\n');
                appendStackTrace(sb, tp.getStackTraceElementProxyArray());
            }
        }

        return sb.toString();
    }

    private void appendStackTrace(StringBuilder sb, ch.qos.logback.classic.spi.StackTraceElementProxy[] stackTrace) {
        if (stackTrace != null) {
            for (ch.qos.logback.classic.spi.StackTraceElementProxy step : stackTrace) {
                sb.append("\tat ").append(step.getStackTraceElement().toString()).append('\n');
            }
        }
    }
}
