package top.ilovemyhome.dagtask.agent.execution;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import top.ilovemyhome.dagtask.agent.utils.ShellDetector;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.enums.ShellType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TaskExecution implementation that executes shell scripts across multiple platforms.
 *
 * <p>
 * This executor supports bash, sh, cmd.exe, and PowerShell, automatically selecting
 * the appropriate shell based on the operating system if not explicitly specified.
 * </p>
 *
 * <p>
 * Input is provided as JSON via {@link TaskInput#input()} with the following structure:
 * <pre>
 * {
 *   "command": "echo hello",
 *   "timeoutSeconds": 300,
 *   "workingDirectory": "/tmp",
 *   "env": {"VAR": "value"},
 *   "shell": "bash"           // Optional: auto-selected if not specified
 * }
 * </pre>
 *
 * <p>Shell selection priority:
 * <ul>
 *   <li>If "shell" is explicitly provided in input, it will be used</li>
 *   <li>Otherwise, OS-appropriate shell is auto-selected: bash/sh on Linux/Mac, cmd.exe on Windows</li>
 * </ul>
 *
 * <p>Cross-platform compatibility:
 * <ul>
 *   <li>Windows: cmd.exe or PowerShell</li>
 *   <li>Linux/Mac: bash or sh</li>
 *   <li>Script syntax should match the target shell</li>
 * </ul>
 */
public class ShellTaskExecution implements TaskExecution {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Default timeout in seconds if not specified in input.
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    /**
     * Grace period in seconds after destroy() before destroyForcibly().
     */
    private static final int DESTROY_GRACE_PERIOD_SECONDS = 5;

    @Override
    public TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        String taskName = input.name();
        try {
            Param param = input.getInputAs(Param.class);
            logger.info("Starting shell execution for taskId={}, name={}, OS={}",
                taskId, taskName, ShellDetector.getOsName());
            assert param != null;
            logger.info("Command: {}", param.command());
            return executeShell(taskId, taskName, param);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input: {}", e.getMessage());
            return TaskOutput.fail(taskId, null, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during shell execution for taskId={}", taskId, e);
            return TaskOutput.createErrorOutput(taskId, e);
        }
    }

    private TaskOutput executeShell(Long taskId, String taskName, Param param) throws IOException, InterruptedException {
        validate(param);

        // Determine shell: explicit input or auto-detect based on OS
        String shell = StringUtils.isNotBlank(param.shell()) ? param.shell() : ShellDetector.getDefaultShell();
        if (StringUtils.isNotBlank(param.shell()) && !isShellAvailable(shell)) {
            return TaskOutput.fail(taskId, null, "Shell not available on this system: " + shell);
        }
        logger.info("Using shell: {}", shell);

        // Build command array appropriate for the shell
        String[] commandArray = ShellDetector.buildCommandArray(shell, param.command());
        ProcessBuilder pb = new ProcessBuilder(commandArray);

        if (param.workingDirectory() != null && !param.workingDirectory().isBlank()) {
            pb.directory(new File(param.workingDirectory()));
        }

        Map<String, String> env = pb.environment();
        env.putIfAbsent("LC_ALL", "C.UTF-8");
        env.putIfAbsent("LANG", "C.UTF-8");
        if (param.env() != null && !param.env().isEmpty()) {
            env.putAll(param.env());
        }

        Process process = pb.start();

        String currentTaskId = taskId != null ? taskId.toString() : null;
        StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream(), true, currentTaskId, taskName);
        StreamGobbler stderrGobbler = new StreamGobbler(process.getErrorStream(), false, currentTaskId, taskName);
        String shellBase = new File(shell).getName();
        Thread stdoutThread = new Thread(stdoutGobbler, shellBase + "-stdout-" + taskId);
        Thread stderrThread = new Thread(stderrGobbler, shellBase + "-stderr-" + taskId);
        stdoutThread.setDaemon(true);
        stderrThread.setDaemon(true);
        stdoutThread.start();
        stderrThread.start();

        int timeoutSeconds = param.timeoutSeconds() != null ? param.timeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;
        boolean finishedInTime = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        boolean timedOut = false;
        if (!finishedInTime) {
            logger.warn("TaskId={} timed out after {} seconds, attempting graceful termination", taskId, timeoutSeconds);
            timedOut = true;
            process.destroy();
            boolean destroyed = process.waitFor(DESTROY_GRACE_PERIOD_SECONDS, TimeUnit.SECONDS);
            if (!destroyed) {
                logger.warn("TaskId={} did not terminate gracefully, forcing termination", taskId);
                process.destroyForcibly();
                process.waitFor();
            }
            // Forcibly close streams to unblock StreamGobbler threads.
            // On some platforms destroy() does not immediately close the pipes,
            // leaving readLine() blocked indefinitely.
            closeQuietly(process.getInputStream());
            closeQuietly(process.getErrorStream());
        }

        awaitStreamConsumer(stdoutThread, "stdout", taskId, process.getInputStream());
        awaitStreamConsumer(stderrThread, "stderr", taskId, process.getErrorStream());

        String stdout = stdoutGobbler.getOutput();
        String stderr = stderrGobbler.getOutput();
        int exitCode = process.exitValue();

        if (timedOut || exitCode != 0) {
            String message = buildFailureMessage(timedOut, exitCode, timeoutSeconds, stderr);
            return TaskOutput.fail(taskId, stdout, message);
        }

        logger.info("Task completed successfully, exitCode={}", exitCode);
        return TaskOutput.success(taskId, stdout);
    }

    /**
     * Checks whether the specified shell executable is available on the current system.
     *
     * @param shell the shell identifier to test
     * @return true if the shell can be started and executes a trivial command successfully
     */
    private boolean isShellAvailable(String shell) {
        ShellType shellType = ShellType.fromString(shell);
        if (shellType == null) {
            return false;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                shellType.getExecutable(),
                shellType.getFlag(),
                "echo ok"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Waits for a stream consumer thread to finish.
     *
     * <p>For normal process exit the pipe closes naturally and the thread exits
     * quickly after reading EOF. For timeout exit the caller has already closed
     * the stream, so readLine() is unblocked by IOException.</p>
     *
     * <p>A short timeout is sufficient because the stream is either naturally
     * closed or forcibly closed. If the thread is still stuck (extremely rare),
     * we close the stream again and interrupt as a last resort.</p>
     */
    private void awaitStreamConsumer(Thread thread, String name, Long taskId, java.io.InputStream stream) {
        try {
            thread.join(10000);
            if (thread.isAlive()) {
                logger.warn("TaskId={}: {} consumer still alive after 10s, closing stream to force exit", taskId, name);
                closeQuietly(stream);
                thread.join(5000);
                if (thread.isAlive()) {
                    logger.warn("TaskId={}: {} consumer still alive after stream close, interrupting", taskId, name);
                    thread.interrupt();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("TaskId={}: Interrupted while waiting for {} consumer", taskId, name);
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ignored) {
        }
    }

    private static String buildFailureMessage(boolean timedOut, int exitCode, int timeoutSeconds, String stderr) {
        StringBuilder sb = new StringBuilder();
        if (timedOut) {
            sb.append("Task timed out after ").append(timeoutSeconds).append(" seconds");
        } else {
            sb.append("Task exited with code ").append(exitCode);
        }
        if (StringUtils.isNotBlank(stderr)) {
            sb.append(". stderr: ").append(stderr);
        }
        return sb.toString();
    }

    private void validate(Param param) {
        if (param == null) {
            throw new IllegalArgumentException("Input param is required");
        }
        if (param.command() == null || param.command().isBlank()) {
            throw new IllegalArgumentException("command is required and must not be blank");
        }
        if (param.timeoutSeconds() != null && param.timeoutSeconds() <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be > 0");
        }
        if (StringUtils.isNotBlank(param.shell())){
            var shellType = ShellType.fromString(param.shell());
            if (shellType == null) {
                throw new IllegalArgumentException("Invalid shell type");
            }
        }
    }

    /**
     * Input parameter DTO for BashTaskExecution.
     */
    public record Param(
        String shell,
        String command,
        Integer timeoutSeconds,
        String workingDirectory,
        Map<String, String> env
    ) {
    }

    /**
     * Consumes an InputStream in a separate thread to prevent pipe buffer deadlock.
     * Reads line-by-line using the OS native encoding and prints each line to the logger
     * in real time, while also buffering the full output for the final result.
     */
    private class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final boolean isStdout;
        private final String mdcTaskId;
        private final String mdcTaskName;
        private final StringBuilder output = new StringBuilder();

        StreamGobbler(InputStream inputStream, boolean isStdout,
                      String mdcTaskId, String mdcTaskName) {
            this.inputStream = inputStream;
            this.isStdout = isStdout;
            this.mdcTaskId = mdcTaskId;
            this.mdcTaskName = mdcTaskName;
        }

        @Override
        public void run() {
            if (mdcTaskId != null) {
                MDC.put(MDC_TASK_ID, mdcTaskId);
            }
            if (mdcTaskName != null) {
                MDC.put(MDC_TASK_NAME, mdcTaskName);
            }
            try {
                Charset charset = resolveStreamCharset();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append(System.lineSeparator());
                        if (isStdout) {
                            logger.info("[STDOUT] {}", line);
                        } else {
                            logger.error("[STDERR] {}", line);
                        }
                    }
                } catch (IOException e) {
                    logger.error("Failed to read stream: {}", e.getMessage());
                }
            } finally {
                if (mdcTaskId != null) {
                    MDC.remove(MDC_TASK_ID);
                }
                if (mdcTaskName != null) {
                    MDC.remove(MDC_TASK_NAME);
                }
            }
        }

        String getOutput() {
            return output.toString();
        }
    }

    private static Charset resolveStreamCharset() {
        String nativeEncoding = System.getProperty("sun.jnu.encoding");
        if (nativeEncoding != null) {
            try {
                return Charset.forName(nativeEncoding);
            } catch (Exception ignored) {
            }
        }
        return StandardCharsets.UTF_8;
    }
}
