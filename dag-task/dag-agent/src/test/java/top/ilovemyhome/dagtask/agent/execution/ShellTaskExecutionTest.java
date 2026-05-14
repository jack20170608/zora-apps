package top.ilovemyhome.dagtask.agent.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import top.ilovemyhome.dagtask.agent.utils.ShellDetector;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for BashTaskExecution with cross-platform support.
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
        String inputJson = "{\"command\":\"echo hello\"}";
        TaskInput input = TaskInput.of(1L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        assertThat(output.output()).isInstanceOf(ShellTaskExecution.Result.class);
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("hello");
        assertThat(result.timedOut()).isFalse();
    }

    /**
     * Test timeout on Unix systems
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testTimeoutUnix() {
        String inputJson = "{\"command\":\"sleep 10\",\"timeoutSeconds\":1}";
        TaskInput input = TaskInput.of(2L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.timedOut()).isTrue();
    }

    /**
     * Test timeout on Windows systems
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testTimeoutWindows() {
        String inputJson = "{\"command\":\"timeout /t 10\",\"timeoutSeconds\":1}";
        TaskInput input = TaskInput.of(2L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.timedOut()).isTrue();
    }

    /**
     * Test non-zero exit code
     */
    @Test
    void testNonZeroExitCode() {
        String inputJson = "{\"command\":\"exit 1\"}";
        TaskInput input = TaskInput.of(3L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.timedOut()).isFalse();
    }

    /**
     * Test working directory on Unix
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testWorkingDirectoryUnix() {
        String inputJson = "{\"command\":\"pwd\",\"workingDirectory\":\"/tmp\"}";
        TaskInput input = TaskInput.of(4L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        if (new java.io.File("/tmp").exists()) {
            assertThat(result.stdout()).contains("/tmp");
        }
    }

    /**
     * Test environment variables on Unix
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testEnvVariablesUnix() {
        String inputJson = "{\"command\":\"echo $MY_VAR\",\"env\":{\"MY_VAR\":\"hello\"}}";
        TaskInput input = TaskInput.of(5L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("hello");
    }

    /**
     * Test environment variables on Windows
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testEnvVariablesWindows() {
        String inputJson = "{\"command\":\"echo %MY_VAR%\",\"env\":{\"MY_VAR\":\"hello\"}}";
        TaskInput input = TaskInput.of(5L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("hello");
    }

    /**
     * Test invalid command rejection
     */
    @Test
    void testInvalidParamNullScript() {
        String inputJson = "{\"command\":\"\"}";
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
        String inputJson = "{\"command\":\"echo hello\",\"timeoutSeconds\":-1}";
        TaskInput input = TaskInput.of(7L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isFalse();
        assertThat(output.message()).contains("timeoutSeconds");
    }

    /**
     * Test stderr capture on Unix
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testStderrCapturedUnix() {
        String inputJson = "{\"command\":\"echo error >&2\"}";
        TaskInput input = TaskInput.of(8L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.stdout() + result.stderr()).contains("error");
    }

    /**
     * Test explicit bash shell
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testExplicitBashShell() {
        String inputJson = "{\"command\":\"echo $SHELL\",\"shell\":\"bash\"}";
        TaskInput input = TaskInput.of(9L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
    }

    /**
     * Test explicit cmd.exe shell on Windows
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExplicitCmdShell() {
        String inputJson = "{\"command\":\"echo test\",\"shell\":\"cmd.exe\"}";
        TaskInput input = TaskInput.of(10L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("test");
    }

    /**
     * Test complex multi-line command on Unix
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testMultiLineScriptUnix() {
        String script = "VAR1=\"hello\"\\nVAR2=\"world\"\\necho $VAR1 $VAR2";
        String inputJson = "{\"command\":\"" + script + "\"}";
        TaskInput input = TaskInput.of(11L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("hello").contains("world");
    }

    /**
     * Test auto-detection of appropriate shell
     */
    @Test
    void testAutoDetectShell() {
        // This test verifies that the auto-detected shell works
        String expectedShell = ShellDetector.getDefaultShell();
        String inputJson = "{\"command\":\"echo auto-detect-test\"}";
        TaskInput input = TaskInput.of(12L, null, inputJson, null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        ShellTaskExecution.Result result = (ShellTaskExecution.Result) output.output();
        assertThat(result.stdout()).contains("auto-detect-test");
    }
}
