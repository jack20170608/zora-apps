package top.ilovemyhome.dagtask.agent.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AgentConfiguration} loading from Typesafe Config.
 */
class AgentConfigurationLoadTest {

    private static final Path DEFAULT_BASE_DIR = Path.of(
        System.getProperty("user.home"), ".dag-agent");
    private static final Path DEFAULT_DEAD_LETTER_DIR = DEFAULT_BASE_DIR.resolve("dead-letter");
    private static final Path DEFAULT_TOKEN_DIR = DEFAULT_BASE_DIR.resolve("token");

    @AfterEach
    void cleanupDefaultDirs() {
        // Clean up default directories created during tests
        try {
            if (Files.exists(DEFAULT_DEAD_LETTER_DIR)) {
                Files.deleteIfExists(DEFAULT_DEAD_LETTER_DIR);
            }
            if (Files.exists(DEFAULT_TOKEN_DIR)) {
                Files.deleteIfExists(DEFAULT_TOKEN_DIR);
            }
            if (Files.exists(DEFAULT_BASE_DIR)) {
                Files.deleteIfExists(DEFAULT_BASE_DIR);
            }
        } catch (Exception e) {
            // Ignore cleanup failures
        }
    }

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
                  deadLetterPersistencePath = "/tmp/dag-agent-dead-letter"
                  tokenFilePath = "/tmp/dag-agent-token"
                  token = "secret-token"
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
        assertThat(agentConfig.getDeadLetterPersistencePath()).isEqualTo("/tmp/dag-agent-dead-letter");
        assertThat(agentConfig.getTokenFilePath()).isEqualTo("/tmp/dag-agent-token");
        assertThat(agentConfig.getToken()).isEqualTo("secret-token");
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
                    deadLetterPersistencePath = ""
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
        // Only provide required fields, all optional fields are omitted
        String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "minimal-agent"
                }
                """;

        Config config = ConfigFactory.parseString(configStr);
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        assertThat(agentConfig.getAgentUrl()).isEqualTo("http://localhost:8080");
        assertThat(agentConfig.getDagServerUrl()).isEqualTo("http://localhost:8080");
        assertThat(agentConfig.getAgentId()).isEqualTo("minimal-agent");
        // All defaults should be applied
        assertThat(agentConfig.isAutoRegister()).isTrue();
        assertThat(agentConfig.getMaxConcurrentTasks()).isEqualTo(4);
        assertThat(agentConfig.getMaxPendingTasks()).isEqualTo(100);
        assertThat(agentConfig.getToken()).isEmpty();
        assertThat(agentConfig.getSupportedExecutionKeys()).isEmpty();
    }

    @Test
    void loadWithEmptySupportedExecutionKeys_success() {
        String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "empty-keys"
                  supportedExecutionKeys = []
                }
                """;

        Config config = ConfigFactory.parseString(configStr);
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        assertThat(agentConfig.getSupportedExecutionKeys()).isEmpty();
    }

    @Test
    void loadWithMissingOptionalFields_usesDefaults() {
        // Only provide required fields
        String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "default-agent"
                }
                """;

        Config config = ConfigFactory.parseString(configStr);
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        // Verify all optional fields use defaults
        assertThat(agentConfig.isAutoRegister()).isTrue();
        assertThat(agentConfig.getMaxConcurrentTasks()).isEqualTo(4);
        assertThat(agentConfig.getMaxPendingTasks()).isEqualTo(100);
        assertThat(agentConfig.getToken()).isEmpty();
        assertThat(agentConfig.getSupportedExecutionKeys()).isEmpty();
    }

    @Test
    void loadWithMissingPathFields_usesDefaultPathsAndCreatesDirectories() {
        // Ensure clean state
        cleanupDefaultDirs();

        String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "path-default-agent"
                }
                """;

        Config config = ConfigFactory.parseString(configStr);
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        // Verify default paths are used
        assertThat(agentConfig.getDeadLetterPersistencePath())
                .isEqualTo(DEFAULT_DEAD_LETTER_DIR.toString());
        assertThat(agentConfig.getTokenFilePath())
                .isEqualTo(DEFAULT_TOKEN_DIR.toString());

        // Verify directories are automatically created
        assertThat(Files.exists(DEFAULT_DEAD_LETTER_DIR)).isTrue();
        assertThat(Files.exists(DEFAULT_TOKEN_DIR)).isTrue();
        assertThat(Files.isDirectory(DEFAULT_DEAD_LETTER_DIR)).isTrue();
        assertThat(Files.isDirectory(DEFAULT_TOKEN_DIR)).isTrue();
    }

    @Test
    void loadWithBlankPathFields_usesDefaultPaths() {
        // Ensure clean state
        cleanupDefaultDirs();

        String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "blank-path-agent"
                  deadLetterPersistencePath = ""
                  tokenFilePath = ""
                }
                """;

        Config config = ConfigFactory.parseString(configStr);
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        assertThat(agentConfig.getDeadLetterPersistencePath())
                .isEqualTo(DEFAULT_DEAD_LETTER_DIR.toString());
        assertThat(agentConfig.getTokenFilePath())
                .isEqualTo(DEFAULT_TOKEN_DIR.toString());
        assertThat(Files.exists(DEFAULT_DEAD_LETTER_DIR)).isTrue();
        assertThat(Files.exists(DEFAULT_TOKEN_DIR)).isTrue();
    }

    @Test
    void loadWithExplicitPaths_usesCustomPaths() {
        String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "custom-path-agent"
                  deadLetterPersistencePath = "/custom/dead-letter"
                  tokenFilePath = "/custom/token"
                }
                """;

        Config config = ConfigFactory.parseString(configStr);
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        assertThat(agentConfig.getDeadLetterPersistencePath()).isEqualTo("/custom/dead-letter");
        assertThat(agentConfig.getTokenFilePath()).isEqualTo("/custom/token");
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
    void builderWithMissingPathFields_usesDefaultsAndCreatesDirectories() throws Exception {
        // Ensure clean state
        cleanupDefaultDirs();

        AgentConfiguration config = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("builder-default-paths")
                .build();

        assertThat(config.getDeadLetterPersistencePath())
                .isEqualTo(DEFAULT_DEAD_LETTER_DIR.toString());
        assertThat(config.getTokenFilePath())
                .isEqualTo(DEFAULT_TOKEN_DIR.toString());

        assertThat(Files.exists(DEFAULT_DEAD_LETTER_DIR)).isTrue();
        assertThat(Files.exists(DEFAULT_TOKEN_DIR)).isTrue();
        assertThat(Files.isDirectory(DEFAULT_DEAD_LETTER_DIR)).isTrue();
        assertThat(Files.isDirectory(DEFAULT_TOKEN_DIR)).isTrue();
    }

    @Test
    void builderWithCustomPaths_usesCustomPaths() {
        AgentConfiguration config = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("builder-custom-paths")
                .deadLetterPersistencePath("/custom/dead-letter")
                .tokenFilePath("/custom/token")
                .build();

        assertThat(config.getDeadLetterPersistencePath()).isEqualTo("/custom/dead-letter");
        assertThat(config.getTokenFilePath()).isEqualTo("/custom/token");
    }

    @Test
    void builderWithCustomToken_usesCustomToken() {
        AgentConfiguration config = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("builder-token")
                .token("my-secret-token")
                .build();

        assertThat(config.getToken()).isEqualTo("my-secret-token");
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
        assertThat(agentConfig.getDeadLetterPersistencePath()).isEqualTo("/tmp/dag-agent-dead-letter");
        assertThat(agentConfig.getToken()).isEmpty();
        assertThat(agentConfig.getTokenFilePath()).isEqualTo(DEFAULT_TOKEN_DIR.toString());
    }
}
