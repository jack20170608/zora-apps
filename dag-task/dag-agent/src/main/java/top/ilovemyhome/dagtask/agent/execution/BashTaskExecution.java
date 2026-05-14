package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TaskExecution implementation that executes bash scripts.
 *
 * <p>
 * Input is provided as JSON via {@link TaskInput#input()} with the following structure:
 * <pre>
 * {
 *   "script": "echo hello",
 *   "timeoutSeconds": 300,
 *   "workingDirectory": "/tmp",
 *   "env": {"VAR": "value"},
 *   "shell": "bash"
 * }
 * </pre>
 */
public class BashTaskExecution extends AbstractTaskExecution {

    /** Default timeout in seconds if not specified in input. */
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    /** Grace period in seconds after destroy() before destroyForcibly(). */
    private static final int DESTROY_GRACE_PERIOD_SECONDS = 5;

    @Override
    protected TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        try {
            Param param = input.getInputAs(Param.class);
            logger.info("Starting bash execution for taskId={}, scriptLength={}", taskId, param.script().length());
            return doExecute(taskId, param);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input: {}", e.getMessage());
            return TaskOutput.fail(taskId, null, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during bash execution for taskId={}", taskId, e);
            return TaskOutput.createErrorOutput(taskId, e);
        }
    }

    private TaskOutput doExecute(Long taskId, Param param) throws IOException, InterruptedException {
        validate(param);

        String shell = param.shell() != null && !param.shell().isBlank() ? param.shell() : "bash";
        ProcessBuilder pb = new ProcessBuilder(shell, "-c", param.script());

        if (param.workingDirectory() != null && !param.workingDirectory().isBlank()) {
            pb.directory(new File(param.workingDirectory()));
        }

        if (param.env() != null && !param.env().isEmpty()) {
            Map<String, String> env = pb.environment();
            env.putAll(param.env());
        }

        Process process = pb.start();

        StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream(), true);
        StreamGobbler stderrGobbler = new StreamGobbler(process.getErrorStream(), false);
        Thread stdoutThread = new Thread(stdoutGobbler, "bash-stdout-" + taskId);
        Thread stderrThread = new Thread(stderrGobbler, "bash-stderr-" + taskId);
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
        }

        stdoutThread.join(5000);
        stderrThread.join(5000);

        String stdout = stdoutGobbler.getOutput();
        String stderr = stderrGobbler.getOutput();
        int exitCode = process.exitValue();

        Result result = new Result(exitCode, stdout, stderr, timedOut);

        if (timedOut || exitCode != 0) {
            String message = timedOut
                ? "Task timed out after " + timeoutSeconds + " seconds"
                : "Task exited with code " + exitCode;
            return TaskOutput.fail(taskId, result, message);
        }

        logger.info("Task completed successfully, exitCode={}", exitCode);
        return TaskOutput.success(taskId, result);
    }

    private void validate(Param param) {
        if (param == null) {
            throw new IllegalArgumentException("Input param is required");
        }
        if (param.script() == null || param.script().isBlank()) {
            throw new IllegalArgumentException("script is required and must not be blank");
        }
        if (param.timeoutSeconds() != null && param.timeoutSeconds() <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be > 0");
        }
    }

    /**
     * Input parameter DTO for BashTaskExecution.
     */
    public record Param(
        String script,
        Integer timeoutSeconds,
        String workingDirectory,
        Map<String, String> env,
        String shell
    ) {
    }

    /**
     * Result record containing the output of the bash script execution.
     */
    public record Result(
        int exitCode,
        String stdout,
        String stderr,
        boolean timedOut
    ) {
    }

    /**
     * Consumes an InputStream in a separate thread to prevent pipe buffer deadlock.
     * Routes each line through the task logger.
     */
    private class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final boolean isStdout;
        private final StringBuilder output = new StringBuilder();

        StreamGobbler(InputStream inputStream, boolean isStdout) {
            this.inputStream = inputStream;
            this.isStdout = isStdout;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
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
                output.append("[READ ERROR: ").append(e.getMessage()).append("]");
                logger.error("Failed to read stream: {}", e.getMessage());
            }
        }

        String getOutput() {
            return output.toString();
        }
    }
}
