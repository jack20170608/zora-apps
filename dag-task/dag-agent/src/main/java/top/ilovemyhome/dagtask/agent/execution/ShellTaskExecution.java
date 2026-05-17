package top.ilovemyhome.dagtask.agent.execution;

import org.apache.commons.lang3.StringUtils;
import top.ilovemyhome.dagtask.agent.utils.ShellDetector;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.enums.ShellType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
public class ShellTaskExecution extends AbstractTaskExecution {

    /**
     * Default timeout in seconds if not specified in input.
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    /**
     * Grace period in seconds after destroy() before destroyForcibly().
     */
    private static final int DESTROY_GRACE_PERIOD_SECONDS = 5;

    @Override
    protected TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        var name = input.name();
        try {
            Param param = input.getInputAs(Param.class);
            logger.info("Starting shell execution for taskId={}, name={}, OS={}",
                taskId, name, ShellDetector.getOsName());
            assert param != null;
            logger.info("Command: {}", param.command());
            return doExecute(taskId, param);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input: {}", e.getMessage());
            return TaskOutput.fail(taskId, null, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during shell execution for taskId={}", taskId, e);
            return TaskOutput.createErrorOutput(taskId, e);
        }
    }

    private TaskOutput doExecute(Long taskId, Param param) throws IOException, InterruptedException {
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

        if (param.env() != null && !param.env().isEmpty()) {
            Map<String, String> env = pb.environment();
            env.putAll(param.env());
        }

        Process process = pb.start();

        StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream(), true);
        StreamGobbler stderrGobbler = new StreamGobbler(process.getErrorStream(), false);
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
        }

        stdoutThread.join(5000);
        stderrThread.join(5000);

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
