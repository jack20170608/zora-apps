package top.ilovemyhome.dagtask.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.ilovemyhome.dagtask.agent.client.NoOpAgentSchedulerClient;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskExecutionEngineWaitTest {

    @Mock
    private AgentConfiguration config;

    private NoOpAgentSchedulerClient client;

    private ExecutorService executor;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        client = new NoOpAgentSchedulerClient();
        executor = Executors.newFixedThreadPool(2);
        objectMapper = new ObjectMapper();

        lenient().when(config.getAgentId()).thenReturn("test-agent");
        lenient().when(config.getMaxPendingTasks()).thenReturn(100);
        lenient().when(config.getMaxConcurrentTasks()).thenReturn(2);
        lenient().when(config.getTaskLogDir()).thenReturn(null);
        lenient().when(config.getSupportedExecutionKeys()).thenReturn(List.of("echo", "long-running"));
        lenient().when(config.getDeadLetterPersistencePath()).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void shouldReturnResultImmediately_WhenSubmissionFails() throws Exception {
        // Given
        TaskExecutionEngine engine = new TaskExecutionEngine(config, client, executor, objectMapper);
        engine.start();

        // When
        TaskExecuteResult result = engine.submitAndWait(1L, "nonexistent.ExecutionClass", "{}", false, 1000);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.taskId()).isEqualTo(1L);
        assertThat(result.success()).isFalse();
        assertThat(result.output()).contains("Submission rejected");

        engine.stop();
    }

    @Test
    void shouldExecuteSuccessfully_WhenUsingEchoExecution() throws Exception {
        // Given
        TaskExecutionEngine engine = new TaskExecutionEngine(config, client, executor, objectMapper);
        engine.start();
        String input = "{\"message\": \"Hello World\"}";

        // When
        TaskExecuteResult result = engine.submitAndWait(1L, "top.ilovemyhome.dagtask.agent.execution.EchoExecution", input, false, 1000);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.taskId()).isEqualTo(1L);
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Hello World");

        engine.stop();
    }

    @Test
    void shouldTimeout_WhenTaskTakesTooLong() throws Exception {
        // Given
        TaskExecutionEngine engine = new TaskExecutionEngine(config, client, executor, objectMapper);
        engine.start();
        String input = "{\"durationSeconds\": 10}";

        // When & Then
        assertThatThrownBy(() ->
                engine.submitAndWait(1L, "top.ilovemyhome.dagtask.agent.execution.LongRunningExecution", input, false, 100)
        ).isInstanceOf(TimeoutException.class)
                .hasMessageContaining("timed out");

        engine.stop();
    }
}