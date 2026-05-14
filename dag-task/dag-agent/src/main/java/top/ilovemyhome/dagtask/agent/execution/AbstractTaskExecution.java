package top.ilovemyhome.dagtask.agent.execution;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Abstract base class for task executions that provides per-task isolated logging.
 *
 * <p>For each task execution, a dedicated {@link Logger} bound to a task-specific
 * log file is dynamically created. The file path is derived from {@code taskLogDir}
 * passed via {@link TaskInput#attributes()} under the key {@code "taskLogDir"}.
 *
 * <p>Subclasses should implement {@link #doExecute(TaskInput)} and use the
 * inherited {@code logger} field for all logging. Bash stdout/stderr should be
 * routed through {@code logger.info("[STDOUT] {}", line)} and
 * {@code logger.error("[STDERR] {}", line)} respectively.
 *
 * <p>The logger and its file appender are always cleaned up in {@code finally},
 * regardless of success, failure, or exception.
 */
public abstract class AbstractTaskExecution implements TaskExecution {

    private static final String ATTR_TASK_LOG_DIR = "taskLogDir";
    private static final String ATTR_TASK_LOG_PATTERN = "taskLogPattern";
    private static final String DEFAULT_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%level] %msg%n";

    /** Dynamic task logger; re-bound for each execution. */
    protected Logger logger;

    @Override
    public final TaskOutput execute(TaskInput input) {
        Long taskId = input.taskId();
        setupLogger(taskId, input);
        try {
            return doExecute(input);
        } finally {
            teardownLogger();
        }
    }

    /**
     * Execute the task with the given input.
     *
     * @param input the task input containing parameters and attributes
     * @return the task execution output
     */
    protected abstract TaskOutput doExecute(TaskInput input);

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private void setupLogger(Long taskId, TaskInput input) {
        String taskLogDir = getTaskLogDir(input);
        if (taskLogDir == null || taskLogDir.isBlank()) {
            this.logger = LoggerFactory.getLogger(getClass());
            return;
        }

        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        String loggerName = "task." + taskId;
        ch.qos.logback.classic.Logger taskLogger = ctx.getLogger(loggerName);

        taskLogger.detachAndStopAllAppenders();
        taskLogger.setAdditive(false);
        taskLogger.setLevel(Level.INFO);

        String pattern = getLogPattern(input);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(ctx);
        encoder.setPattern(pattern);
        encoder.start();

        String timestamp = LocalDateTime.now().format(TS_FMT);
        String namePrefix = input.name() != null && !input.name().isBlank() ? input.name() + "-" : "";
        String logFileName = namePrefix + taskId + "-" + timestamp + ".log";

        FileAppender<ILoggingEvent> appender = new FileAppender<>();
        appender.setContext(ctx);
        appender.setName("task-file-" + taskId);
        appender.setFile(Path.of(taskLogDir, logFileName).toString());
        appender.setAppend(false);
        appender.setEncoder(encoder);
        appender.start();

        taskLogger.addAppender(appender);
        this.logger = taskLogger;
    }

    private void teardownLogger() {
        if (this.logger instanceof ch.qos.logback.classic.Logger taskLogger) {
            taskLogger.detachAndStopAllAppenders();
        }
        this.logger = null;
    }

    private String getTaskLogDir(TaskInput input) {
        if (input.attributes() == null) {
            return null;
        }
        return input.attributes().get(ATTR_TASK_LOG_DIR);
    }

    private String getLogPattern(TaskInput input) {
        if (input.attributes() == null) {
            return DEFAULT_PATTERN;
        }
        String pattern = input.attributes().get(ATTR_TASK_LOG_PATTERN);
        return pattern != null ? pattern : DEFAULT_PATTERN;
    }
}
