package top.ilovemyhome.dagtask.agent;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for {@link AgentStarter} new API that accepts {@link AgentConfiguration} directly.
 */
class AgentStarterTest {

    @Test
    void testStartWithAgentConfiguration() {
        // This just verifies the method compiles and runs without throwing
        // Full integration test would need a running server
        AgentConfiguration config = AgentConfiguration.builder()
                .agentId("test-agent-1")
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8081")
                .maxConcurrentTasks(2)
                .maxPendingTasks(10)
                .build();

        // Just verify it doesn't throw exception when creating the agent
        // The start method starts the agent and registers a shutdown hook
        // For unit test, this is fine - it won't actually process tasks
        assertDoesNotThrow(() -> {
            AgentStarter.start(config);
        });
    }
}
