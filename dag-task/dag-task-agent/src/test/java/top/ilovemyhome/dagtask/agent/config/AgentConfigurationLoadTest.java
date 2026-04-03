package top.ilovemyhome.dagtask.agent.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AgentConfiguration} loading from Typesafe Config.
 */
class AgentConfigurationLoadTest {

    @Test
    void loadFullConfiguration_success() {
        String configStr = """
                dag-agent {
                  agentUrl = "http://agent01.example.com:8081"
                  dagServerUrl = "http://dag-server:8080"
                  agentId = "agent-01"
                  autoRegister = false
                  maxConcurrentTasks = 8
                  maxPendingTasks = 200
                  supportedExecutionKeys = [
                    "top.ilovemyhome.dagtask.core.TestTaskExecution",
                    "com.example.MyCustomTask",
                    "com.example.AnotherTask"
                  ]
                }
                """;

        Config config = ConfigFactory.parseString(configStr);
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        assertThat(agentConfig.getAgentUrl()).isEqualTo("http://agent01.example.com:8081");
        assertThat(agentConfig.getDagServerUrl()).isEqualTo("http://dag-server:8080");
        assertThat(agentConfig.getAgentId()).isEqualTo("agent-01");
        assertThat(agentConfig.isAutoRegister()).isFalse();
        assertThat(agentConfig.getMaxConcurrentTasks()).isEqualTo(8);
        assertThat(agentConfig.getMaxPendingTasks()).isEqualTo(200);
        assertThat(agentConfig.getSupportedExecutionKeys())
                .containsExactly(
                        "top.ilovemyhome.dagtask.core.TestTaskExecution",
                        "com.example.MyCustomTask",
                        "com.example.AnotherTask"
                );
        assertThat(agentConfig.getBaseUrl()).isEqualTo("http://agent01.example.com:8081");
    }

    @Test
    void loadWithCustomPrefix_success() {
        String configStr = """
                agent {
                  dag-task {
                    agentUrl = "http://localhost:9000"
                    dagServerUrl = "http://localhost:8080"
                    agentId = "test-agent"
                    autoRegister = true
                    maxConcurrentTasks = 4
                    maxPendingTasks = 100
                    supportedExecutionKeys = []
                  }
                }
                """;

        Config config = ConfigFactory.parseString(configStr);
        AgentConfiguration agentConfig = AgentConfiguration.load("agent.dag-task", config);

        assertThat(agentConfig.getAgentUrl()).isEqualTo("http://localhost:9000");
        assertThat(agentConfig.getDagServerUrl()).isEqualTo("http://localhost:8080");
        assertThat(agentConfig.getAgentId()).isEqualTo("test-agent");
        // Default values
        assertThat(agentConfig.isAutoRegister()).isTrue();
        assertThat(agentConfig.getMaxConcurrentTasks()).isEqualTo(4);
        assertThat(agentConfig.getMaxPendingTasks()).isEqualTo(100);
        assertThat(agentConfig.getSupportedExecutionKeys()).isEmpty();
    }

    @Test
    void loadWithMinimumRequiredConfig_success() {
        String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "minimal-agent"
                  autoRegister = true
                  maxConcurrentTasks = 4
                  maxPendingTasks = 100
                  supportedExecutionKeys = []
                }
                """;

        Config config = ConfigFactory.parseString(configStr);
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        assertThat(agentConfig.getAgentUrl()).isEqualTo("http://localhost:8080");
        assertThat(agentConfig.getDagServerUrl()).isEqualTo("http://localhost:8080");
        assertThat(agentConfig.getAgentId()).isEqualTo("minimal-agent");
        // Check all defaults
        assertThat(agentConfig.isAutoRegister()).isTrue();
        assertThat(agentConfig.getMaxConcurrentTasks()).isEqualTo(4);
        assertThat(agentConfig.getMaxPendingTasks()).isEqualTo(100);
        assertThat(agentConfig.getSupportedExecutionKeys()).isEmpty();
    }

    @Test
    void loadWithEmptySupportedExecutionKeys_success() {
        String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "empty-keys"
                  autoRegister = true
                  maxConcurrentTasks = 4
                  maxPendingTasks = 100
                  supportedExecutionKeys = []
                }
                """;

        Config config = ConfigFactory.parseString(configStr);
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        assertThat(agentConfig.getSupportedExecutionKeys()).isEmpty();
    }

    @Test
    void loadNullConfig_throwsNpe() {
        assertThatThrownBy(() -> AgentConfiguration.load(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Config cannot be null");
    }

    @Test
    void loadNullPathPrefix_throwsNpe() {
        Config config = ConfigFactory.empty();
        assertThatThrownBy(() -> AgentConfiguration.load(null, config))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("PathPrefix cannot be null");
    }

    @Test
    void loadBuilderWithInvalidMaxConcurrentTasks_throwsException() {
        // Setting just one field doesn't build, shouldn't throw yet
        AgentConfiguration.builder()
                .dagServerUrl("http://localhost:8080");
        AgentConfiguration.builder()
                .agentUrl("http://localhost:8080");
        // Now test the invalid value - 0 for maxConcurrentTasks
        assertThatThrownBy(() -> AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("test")
                .maxConcurrentTasks(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxConcurrentTasks must be > 0");
    }

    @Test
    void loadBuilderWithInvalidMaxPendingTasks_throwsException() {
        assertThatThrownBy(() -> AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("test")
                .maxPendingTasks(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxPendingTasks must be > 0");
    }

    @Test
    void loadBuilderWithoutDagServerUrl_throwsNpe() {
        assertThatThrownBy(() -> AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .agentId("test")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dagServerUrl is required");
    }

    @Test
    void loadBuilderWithoutAgentUrl_throwsNpe() {
        assertThatThrownBy(() -> AgentConfiguration.builder()
                .dagServerUrl("http://localhost:8080")
                .agentId("test")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("agentUrl is required");
    }

    @Test
    void loadBuilderWithoutAgentId_throwsNpe() {
        assertThatThrownBy(() -> AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("agentId is required");
    }

    @Test
    void loadFromClasspathConfigFile_success() {
        // Load from classpath:config/agent.conf
        Config config = ConfigFactory.load("config/agent.conf");
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        assertThat(agentConfig.getAgentUrl()).isEqualTo("http://agent01.example.com:8081");
        assertThat(agentConfig.getDagServerUrl()).isEqualTo("http://dag-server:8080");
        assertThat(agentConfig.getAgentId()).isEqualTo("agent-from-classpath");
        assertThat(agentConfig.isAutoRegister()).isTrue();
        assertThat(agentConfig.getMaxConcurrentTasks()).isEqualTo(4);
        assertThat(agentConfig.getMaxPendingTasks()).isEqualTo(100);
        assertThat(agentConfig.getSupportedExecutionKeys())
                .containsExactly(
                        "top.ilovemyhome.dagtask.core.ShellTaskExecution",
                        "top.ilovemyhome.dagtask.core.PythonTaskExecution"
                );
    }
}
