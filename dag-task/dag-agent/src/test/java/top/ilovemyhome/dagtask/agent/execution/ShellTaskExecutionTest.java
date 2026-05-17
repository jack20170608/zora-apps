package top.ilovemyhome.dagtask.agent.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import top.ilovemyhome.dagtask.agent.utils.ShellDetector;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ShellTaskExecution with cross-platform support.
 *
 * Tests automatically skip based on OS when necessary to ensure compatibility.
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
        assertThat(output.output()).isInstanceOf(String.class);
        assertThat((String) output.output()).contains("hello");
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
            {"command":"ping -n 5 127.0.0.1","timeoutSeconds": 1}
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
        if (new java.io.File("/tmp").exists()) {
            assertThat((String) output.output()).contains("/tmp");
        }
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
        assertThat((String) output.output()).contains("hello");
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
        assertThat((String) output.output()).contains("hello");
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
     * Test stderr is included in failure message
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testStderrInFailureMessage() {
        String inputJson = """
            {"command":"echo error >&2; exit 1"}
            """;
        TaskInput input = TaskInput.of(8L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        assertThat(output.message()).contains("error");
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
        assertThat((String) output.output()).contains("test");
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
        assertThat((String) output.output()).contains("hello").contains("world");
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
        assertThat((String) output.output()).contains("auto-detect-test");
    }

    /**
     * UTF-8 encoding diagnostic: echo a CJK character and verify it is not mangled.
     */
    @Test
    void testUtf8ChineseOutput() {
        // 中=中  文=文 — Unicode escapes guarantee correctness regardless of source-file encoding.
        String inputJson = """
            {"command":"echo 中文"}
            """;
        TaskInput input = TaskInput.of(99L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        String stdout = (String) output.output();
        assertThat(output.isSuccess()).isTrue();
        assertThat(stdout).contains("中文");
    }
}
