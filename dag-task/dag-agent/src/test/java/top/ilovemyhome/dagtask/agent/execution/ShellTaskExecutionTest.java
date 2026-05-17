package top.ilovemyhome.dagtask.agent.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ShellTaskExecution with cross-platform support.
 *
 * Tests automatically skip based on OS when necessary to ensure compatibility.
 * Note: stdout/stderr are streamed to the logger in real time and are not
 * captured into {@link TaskOutput#output()}. Full output should be checked
 * in the log files.
 */
class ShellTaskExecutionTest {

    private final ShellTaskExecution execution = new ShellTaskExecution();

    /**
     * Test basic echo command - works on all platforms with auto-detected shell
     */
    @Test
    void testBasicExecution() {
        String inputJson = """
            {"command":"echo hello"}
            """;
        TaskInput input = TaskInput.of(1L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
    }

    /**
     * Test timeout on Unix systems
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testTimeoutUnix() {
        String inputJson = """
            {"command":"sleep 10","timeoutSeconds":1}
            """;
        TaskInput input = TaskInput.of(2L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        assertThat(output.message()).contains("timed out");
    }

    /**
     * Test timeout on Windows systems
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testTimeoutWindows() {
        String inputJson = """
            {"command":"ping -n 11 127.0.0.1","timeoutSeconds": 1}
            """;
        TaskInput input = TaskInput.of(2L, "NetworkPingTask", inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        assertThat(output.message()).contains("timed out");
    }

    /**
     * Test non-zero exit code
     */
    @Test
    void testNonZeroExitCode() {
        String inputJson = """
            {"command":"exit 1"}
            """;
        TaskInput input = TaskInput.of(3L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        assertThat(output.message()).contains("exited with code 1");
    }

    /**
     * Test working directory on Unix
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testWorkingDirectoryUnix() {
        String inputJson = """
            {"command":"pwd","workingDirectory":"/tmp"}
            """;
        TaskInput input = TaskInput.of(4L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
    }

    /**
     * Test environment variables on Unix
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testEnvVariablesUnix() {
        String inputJson = """
            {"command":"echo $MY_VAR","env":{"MY_VAR":"hello"}}
            """;
        TaskInput input = TaskInput.of(5L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
    }

    /**
     * Test environment variables on Windows
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testEnvVariablesWindows() {
        String inputJson = """
            {"command":"echo %MY_VAR%","env":{"MY_VAR":"hello"}}
            """;
        TaskInput input = TaskInput.of(5L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
    }

    /**
     * Test invalid command rejection
     */
    @Test
    void testInvalidParamNullScript() {
        String inputJson = """
            {"command":""}
            """;
        TaskInput input = TaskInput.of(6L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        assertThat(output.message()).contains("command");
    }

    /**
     * Test invalid timeout rejection
     */
    @Test
    void testInvalidParamNegativeTimeout() {
        String inputJson = """
            {"command":"echo hello","timeoutSeconds":-1}
            """;
        TaskInput input = TaskInput.of(7L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        assertThat(output.message()).contains("timeoutSeconds");
    }

    /**
     * Test explicit bash shell
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testExplicitBashShell() {
        String inputJson = """
            {"command":"echo $SHELL","shell":"bash"}
            """;
        TaskInput input = TaskInput.of(9L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
    }

    /**
     * Test explicit cmd.exe shell on Windows
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExplicitCmdShell() {
        String inputJson = """
            {"command":"echo test","shell":"cmd.exe"}
            """;
        TaskInput input = TaskInput.of(10L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
    }

    /**
     * Test complex multi-line command on Unix
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testMultiLineScriptUnix() {
        String inputJson = """
            {"command":"VAR1=\\"hello\\"\\nVAR2=\\"world\\"\\necho $VAR1 $VAR2"}
            """;
        TaskInput input = TaskInput.of(11L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
    }

    /**
     * Test auto-detection of appropriate shell
     */
    @Test
    void testAutoDetectShell() {
        String inputJson = """
            {"command":"echo auto-detect-test"}
            """;
        TaskInput input = TaskInput.of(12L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
    }

    /**
     * UTF-8 encoding diagnostic: echo a CJK character and verify execution
     * succeeds. The actual character correctness is validated via the log
     * output rather than TaskOutput (stdout is not captured into output).
     */
    @Test
    void testUtf8ChineseOutput() {
        String inputJson = """
            {"command":"echo 中文"}
            """;
        TaskInput input = TaskInput.of(99L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
    }
}
