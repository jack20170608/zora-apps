package top.ilovemyhome.dagtask.agent.execution;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import static org.assertj.core.api.Assertions.assertThat;

class BashTaskExecutionTest {

    private final BashTaskExecution execution = new BashTaskExecution();

    @Test
    void testBasicExecution() {
        String inputJson = "{\"script\":\"echo hello\"}";
        TaskInput input = TaskInput.of(1L, inputJson, null);

        TaskOutput output = execution.execute(input, null);

        assertThat(output.isSuccess()).isTrue();
        assertThat(output.output()).isInstanceOf(BashTaskExecution.Result.class);
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("hello");
        assertThat(result.timedOut()).isFalse();
    }

    @Test
    void testTimeout() {
        String inputJson = "{\"script\":\"sleep 10\",\"timeoutSeconds\":1}";
        TaskInput input = TaskInput.of(2L, inputJson, null);

        TaskOutput output = execution.execute(input, null);

        assertThat(output.isSuccess()).isFalse();
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        assertThat(result.timedOut()).isTrue();
    }

    @Test
    void testNonZeroExitCode() {
        String inputJson = "{\"script\":\"exit 1\"}";
        TaskInput input = TaskInput.of(3L, inputJson, null);

        TaskOutput output = execution.execute(input, null);

        assertThat(output.isSuccess()).isFalse();
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.timedOut()).isFalse();
    }

    @Test
    void testWorkingDirectory() {
        String inputJson = "{\"script\":\"pwd\",\"workingDirectory\":\"/tmp\"}";
        TaskInput input = TaskInput.of(4L, inputJson, null);

        TaskOutput output = execution.execute(input, null);

        assertThat(output.isSuccess()).isTrue();
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        if (new java.io.File("/tmp").exists()) {
            assertThat(result.stdout()).contains("/tmp");
        }
    }

    @Test
    void testEnvVariables() {
        org.junit.jupiter.api.Assumptions.assumeFalse(
            System.getProperty("os.name").toLowerCase().contains("win"),
            "Environment variable propagation to bash is unreliable on Windows"
        );
        String inputJson = "{\"script\":\"echo $MY_VAR\",\"env\":{\"MY_VAR\":\"hello\"}}";
        TaskInput input = TaskInput.of(5L, inputJson, null);

        TaskOutput output = execution.execute(input, null);

        assertThat(output.isSuccess()).isTrue();
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("hello");
    }

    @Test
    void testInvalidParamNullScript() {
        String inputJson = "{\"script\":\"\"}";
        TaskInput input = TaskInput.of(6L, inputJson, null);

        TaskOutput output = execution.execute(input, null);

        assertThat(output.isSuccess()).isFalse();
        assertThat(output.message()).contains("script");
    }

    @Test
    void testInvalidParamNegativeTimeout() {
        String inputJson = "{\"script\":\"echo hello\",\"timeoutSeconds\":-1}";
        TaskInput input = TaskInput.of(7L, inputJson, null);

        TaskOutput output = execution.execute(input, null);

        assertThat(output.isSuccess()).isFalse();
        assertThat(output.message()).contains("timeoutSeconds");
    }

    @Test
    void testStderrCaptured() {
        String inputJson = "{\"script\":\"echo error >&2\"}";
        TaskInput input = TaskInput.of(8L, inputJson, null);

        TaskOutput output = execution.execute(input, null);

        assertThat(output.isSuccess()).isTrue();
        BashTaskExecution.Result result = (BashTaskExecution.Result) output.output();
        assertThat(result.stdout() + result.stderr()).contains("error");
    }
}
