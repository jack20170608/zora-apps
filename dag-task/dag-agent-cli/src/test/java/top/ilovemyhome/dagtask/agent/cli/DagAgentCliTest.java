package top.ilovemyhome.dagtask.agent.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import top.ilovemyhome.dagtask.agent.execution.EchoExecution;
import top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution;
import top.ilovemyhome.dagtask.agent.execution.SimpleCounterExecution;

import static org.assertj.core.api.Assertions.assertThat;

class DagAgentCliTest {

    @Test
    void shouldExecuteSuccessfully_WithLocalMode() {
        CliArguments args = CliArguments.builder()
            .withId(1000L)
            .withName("EchoTask")
            .withAgentId("test-cli-agent")
            .withInputJson("""
                {
                    "message": "hello"
                }
                """)
            .withExecutionClass(EchoExecution.class.getCanonicalName())
            .withTimeoutMs(30 * 1000L)
            .build();
        int exitCode = DagAgentCli.execute(args);
        assertThat(exitCode).isEqualTo(0);
    }


    @Test
    void shouldExecuteSuccessfullyForSimpleCounterExecution() {
        CliArguments args = CliArguments.builder()
            .withId(1000L)
            .withName("CounterTask")
            .withInputJson("""
                {
                "from" : 1,
                "to": 100,
                "intervalMillisecond" : 10
                }
                """)
            .withExecutionClass(SimpleCounterExecution.class.getCanonicalName())
            .withTimeoutMs(30 * 1000L)
            .build();
        int exitCode = DagAgentCli.execute(args);
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void shouldExecuteFailureForSimpleCounterExecution() {
        CliArguments args = CliArguments.builder()
            .withId(1000L)
            .withName("CounterTask")
            .withInputJson("""
                {
                "from" : 200,
                "to": 100,
                "intervalMillisecond" : 10
                }
                """)
            .withExecutionClass(SimpleCounterExecution.class.getCanonicalName())
            .withTimeoutMs(30 * 1000L)
            .build();
        int exitCode = DagAgentCli.execute(args);
        assertThat(exitCode).isEqualTo(1);
    }


    @Test
    @EnabledOnOs({OS.WINDOWS})
    void testLongRunningTask(){
        CliArguments args = CliArguments.builder()
            .withId(1000L)
            .withName("WindowsBashTask")
            .withInputJson("""
                {
                "shell": "cmd",
                "command" : "dir && echo $p1 $p2 ",
                "timeoutSeconds": 10,
                "env": {
                    "p1": "v1",
                    "p2": 100
                    }
                }
                """)
            .withExecutionClass(ShellTaskExecution.class.getCanonicalName())
            .withTimeoutMs(30 * 1000L)
            .build();
        int exitCode = DagAgentCli.execute(args);
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void shouldReturnNonZero_WhenExecutionFails() {
        CliArguments args = CliArguments.builder()
            .withAgentId("test-cli-agent")
            .withInputJson("""
                {}
                """)
            .withExecutionClass("NotExistsClass.class")
            .withTimeoutMs(30 * 1000L)
            .build();
        int exitCode = DagAgentCli.execute(args);
        assertThat(exitCode).isEqualTo(1);
    }

}
