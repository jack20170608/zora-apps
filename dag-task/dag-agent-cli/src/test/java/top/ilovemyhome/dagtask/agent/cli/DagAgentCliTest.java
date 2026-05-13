package top.ilovemyhome.dagtask.agent.cli;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.agent.execution.EchoExecution;

import static org.assertj.core.api.Assertions.assertThat;

class DagAgentCliTest {

    @Test
    void shouldExecuteSuccessfully_WithLocalMode() {
        CliArguments args = new CliArguments();
        setField(args, "executionClass", EchoExecution.class.getName());
        setField(args, "inputJson", "{\"message\":\"hello\"}");
        setField(args, "agentId", "test-cli-agent");
        setField(args, "timeoutMs", 30000L);

        int exitCode = DagAgentCli.execute(args);

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void shouldReturnNonZero_WhenExecutionFails() {
        CliArguments args = new CliArguments();
        setField(args, "executionClass", "nonexistent.Class");
        setField(args, "inputJson", "{}");
        setField(args, "agentId", "test-cli-agent");
        setField(args, "timeoutMs", 30000L);

        int exitCode = DagAgentCli.execute(args);

        assertThat(exitCode).isEqualTo(1);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = CliArguments.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}