package top.ilovemyhome.dagtask.si;

/**
 * Writer for per-task execution logs.
 * Implementations write task execution messages and subprocess output
 * to a destination (e.g. file, memory buffer).
 */
public interface TaskLogWriter {

    /**
     * Write an informational message.
     */
    void info(String message);

    /**
     * Write a warning message.
     */
    void warn(String message);

    /**
     * Write an error message.
     */
    void error(String message);

    /**
     * Write a line of subprocess stdout output.
     */
    void stdout(String message);

    /**
     * Write a line of subprocess stderr output.
     */
    void stderr(String message);

    /**
     * Flush any buffered output and release resources.
     */
    void close();
}
