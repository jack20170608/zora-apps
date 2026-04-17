package top.ilovemyhome.dagtask.agent.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DagTaskAgentTest {

    @Mock
    private AgentConfiguration config;

    @Mock
    private AgentSchedulerClient client;

    @Mock
    private ExecutorService executor;

    @Test
    void shouldMarkRegistered_WhenFirstRegistrationSucceeds() {
        // Given
        when(config.isAutoRegister()).thenReturn(true);
        when(config.getAgentId()).thenReturn("test-agent");
        when(config.getBaseUrl()).thenReturn("http://localhost:8080");
        when(config.getMaxConcurrentTasks()).thenReturn(10);
        when(config.getMaxPendingTasks()).thenReturn(100);
        when(config.getSupportedExecutionKeys()).thenReturn(List.of("echo"));
        when(client.register(any(AgentRegisterRequest.class)))
                .thenReturn(Response.ok().build());

        // When
        DagTaskAgent agent = new DagTaskAgent(config, client, executor);
        agent.start();

        // Then
        assertThat(agent.isRegistered()).isTrue();
        verify(client, times(1)).register(any());
    }

    @Test
    void shouldStartBackgroundThread_WhenFirstRegistrationFails() throws InterruptedException {
        // Given
        when(config.isAutoRegister()).thenReturn(true);
        when(config.getAgentId()).thenReturn("test-agent");
        when(config.getBaseUrl()).thenReturn("http://localhost:8080");
        when(config.getMaxConcurrentTasks()).thenReturn(10);
        when(config.getMaxPendingTasks()).thenReturn(100);
        when(config.getSupportedExecutionKeys()).thenReturn(List.of("echo"));
        when(client.register(any(AgentRegisterRequest.class)))
                .thenReturn(Response.serverError().build());

        // When
        DagTaskAgent agent = new DagTaskAgent(config, client, executor);
        agent.start();

        // Then
        assertThat(agent.isRegistered()).isFalse();
        assertThat(agent.getRegistrationRetryThread()).isNotNull();
        assertThat(agent.getRegistrationRetryThread().isAlive()).isTrue();

        // Cleanup - interrupt the thread
        agent.stop(false);
        // Give the thread a chance to exit
        agent.getRegistrationRetryThread().join(100);
        // After stop, the thread should no longer be alive
        assertThat(agent.getRegistrationRetryThread().isAlive()).isFalse();
    }

    @Test
    void shouldApplyJitterWithinExpectedRange() {
        // Given
        lenient().when(config.getMaxConcurrentTasks()).thenReturn(10);
        lenient().when(config.getMaxPendingTasks()).thenReturn(100);
        lenient().when(config.getSupportedExecutionKeys()).thenReturn(List.of("echo"));
        DagTaskAgent agent = new DagTaskAgent(config, client, executor);
        long delay = 100L;

        // When - test multiple times to check range
        for (int i = 0; i < 100; i++) {
            long jittered = agent.applyJitter(delay);
            // Expected range: 90 - 110 (±10%)
            assertThat(jittered).isBetween(90L, 110L);
        }
    }

    @Test
    void shouldVerifyConstants() {
        // Verify the requirements are met
        assertThat(DagTaskAgent.INITIAL_DELAY_MS).isEqualTo(10L);
        assertThat(DagTaskAgent.MAX_DELAY_MS).isEqualTo(5 * 60 * 1000L); // 5 minutes
    }
}
