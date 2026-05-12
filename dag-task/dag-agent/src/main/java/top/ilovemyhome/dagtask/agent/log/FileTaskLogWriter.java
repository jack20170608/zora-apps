package top.ilovemyhome.dagtask.agent.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskLogWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * File-based implementation of {@link TaskLogWriter}.
 * Each task gets its own log file under a configured directory.
 */
public class FileTaskLogWriter implements TaskLogWriter {

    private static final Logger logger = LoggerFactory.getLogger(FileTaskLogWriter.class);

    private final Long taskId;
    private final BufferedWriter writer;

    public FileTaskLogWriter(Long taskId, String logDir) {
        this.taskId = taskId;
        try {
            Path dir = Path.of(logDir);
            Files.createDirectories(dir);
            Path logFile = dir.resolve(taskId + ".log");
            this.writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create task log file for taskId=" + taskId + " in dir=" + logDir, e);
        }
    }

    @Override
    public void info(String message) {
        writeLine("[INFO] " + message);
    }

    @Override
    public void warn(String message) {
        writeLine("[WARN] " + message);
    }

    @Override
    public void error(String message) {
        writeLine("[ERROR] " + message);
    }

    @Override
    public void stdout(String message) {
        writeLine("[STDOUT] " + message);
    }

    @Override
    public void stderr(String message) {
        writeLine("[STDERR] " + message);
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            logger.warn("Failed to close task log writer for taskId={}", taskId, e);
        }
    }

    private synchronized void writeLine(String line) {
        try {
            writer.write(Instant.now().toString());
            writer.write(' ');
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            logger.warn("Failed to write to task log for taskId={}: {}", taskId, line, e);
        }
    }
}
