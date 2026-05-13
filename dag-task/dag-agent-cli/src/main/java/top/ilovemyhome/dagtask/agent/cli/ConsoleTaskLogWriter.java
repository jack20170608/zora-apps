package top.ilovemyhome.dagtask.agent.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskLogWriter;

/**
 * {@link TaskLogWriter} implementation that writes task execution logs
 * to the console (via SLF4J logger) for CLI usage.
 */
public class ConsoleTaskLogWriter implements TaskLogWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleTaskLogWriter.class);

    @Override
    public void info(String message) {
        LOGGER.info("[TASK] {}", message);
    }

    @Override
    public void warn(String message) {
        LOGGER.warn("[TASK] {}", message);
    }

    @Override
    public void error(String message) {
        LOGGER.error("[TASK] {}", message);
    }

    @Override
    public void stdout(String message) {
        LOGGER.info("[STDOUT] {}", message);
    }

    @Override
    public void stderr(String message) {
        LOGGER.error("[STDERR] {}", message);
    }

    @Override
    public void close() {
        // No resources to release for console output
    }
}