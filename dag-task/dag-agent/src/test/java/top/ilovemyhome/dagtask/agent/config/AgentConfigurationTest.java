package top.ilovemyhome.dagtask.agent.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive unit tests for {@link AgentConfiguration}.
 * Covers config loading, builder pattern, setters, equals/hashCode, and toString.
 */
class AgentConfigurationTest {

    private static final Path DEFAULT_BASE_DIR = Path.of(
        System.getProperty("user.home"), ".dag-agent");
    private static final Path DEFAULT_DEAD_LETTER_DIR = DEFAULT_BASE_DIR.resolve("dead-letter");

    @AfterEach
    void cleanupDefaultDirs() {
        try {
            Files.deleteIfExists(DEFAULT_DEAD_LETTER_DIR);
            Files.deleteIfExists(DEFAULT_BASE_DIR);
        } catch (Exception e) {
            // Ignore cleanup failures
        }
    }

    // ============================================================================
    // 1. Config Loading Tests
    // ============================================================================
    @Nested
    class LoadFromConfigTests {

        @Test
        void loadFullConfiguration_allFieldsMapped() {
            String configStr = """
                dag-agent {
                  agentUrl = "http://agent01.example.com:8081"
                  agentName = "agent-01"
                  dagServerUrl = "http://dag-server:8080"
                  agentId = "test-agent-01"
                  autoRegister = false
                  generateToken = true
                  maxConcurrentTasks = 8
                  maxPendingTasks = 200
                  deadLetterPersistencePath = "/tmp/dag-agent-dead-letter"
                  token = "secret-token"
                  supportedExecutionKeys = [
                    "top.ilovemyhome.dagtask.core.TestTaskExecution",
                    "com.example.MyCustomTask"
                  ]
                }
                """;

            AgentConfiguration cfg = AgentConfiguration.load(ConfigFactory.parseString(configStr));

            assertThat(cfg.getAgentUrl()).isEqualTo("http://agent01.example.com:8081");
            assertThat(cfg.getAgentName()).isEqualTo("agent-01");
            assertThat(cfg.getDagServerUrl()).isEqualTo("http://dag-server:8080");
            assertThat(cfg.getAgentId()).isEqualTo("test-agent-01");
            assertThat(cfg.isAutoRegister()).isFalse();
            assertThat(cfg.isGenerateToken()).isTrue();
            assertThat(cfg.getMaxConcurrentTasks()).isEqualTo(8);
            assertThat(cfg.getMaxPendingTasks()).isEqualTo(200);
            assertThat(cfg.getDeadLetterPersistencePath()).isEqualTo("/tmp/dag-agent-dead-letter");
            assertThat(cfg.getToken()).isEqualTo("secret-token");
            assertThat(cfg.getSupportedExecutionKeys())
                .containsExactly(
                    "top.ilovemyhome.dagtask.core.TestTaskExecution",
                    "com.example.MyCustomTask"
                );
            assertThat(cfg.getBaseUrl()).isEqualTo("http://agent01.example.com:8081");
        }

        @Test
        void loadWithCustomPrefix_success() {
            String configStr = """
                schedulerServer {
                  agent {
                    agentUrl = "http://localhost:9000"
                    dagServerUrl = "http://localhost:8080"
                    agentId = "prefixed-agent"
                  }
                }
                """;

            AgentConfiguration cfg = AgentConfiguration.load("schedulerServer.agent", ConfigFactory.parseString(configStr));

            assertThat(cfg.getAgentUrl()).isEqualTo("http://localhost:9000");
            assertThat(cfg.getDagServerUrl()).isEqualTo("http://localhost:8080");
            assertThat(cfg.getAgentId()).isEqualTo("prefixed-agent");
            assertThat(cfg.isAutoRegister()).isTrue();
            assertThat(cfg.isGenerateToken()).isFalse();
        }

        @Test
        void loadWithMinimumRequiredConfig_usesDefaultsForOptionalFields() {
            String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "minimal-agent"
                }
                """;

            AgentConfiguration cfg = AgentConfiguration.load(ConfigFactory.parseString(configStr));

            assertThat(cfg.getAgentUrl()).isEqualTo("http://localhost:8080");
            assertThat(cfg.isAutoRegister()).isTrue();
            assertThat(cfg.isGenerateToken()).isFalse();
            assertThat(cfg.getMaxConcurrentTasks()).isEqualTo(4);
            assertThat(cfg.getMaxPendingTasks()).isEqualTo(100);
            assertThat(cfg.getToken()).isEmpty();
            assertThat(cfg.getSupportedExecutionKeys()).isEmpty();
            assertThat(cfg.getAgentName()).isEmpty();
        }

        @Test
        void loadWithMissingPathFields_createsDefaultDirectories() {
            cleanupDefaultDirs();

            String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "default-path-agent"
                }
                """;

            AgentConfiguration cfg = AgentConfiguration.load(ConfigFactory.parseString(configStr));

            assertThat(cfg.getDeadLetterPersistencePath())
                .isEqualTo(DEFAULT_DEAD_LETTER_DIR.toString());
            assertThat(Files.exists(DEFAULT_DEAD_LETTER_DIR)).isTrue();
            assertThat(Files.isDirectory(DEFAULT_DEAD_LETTER_DIR)).isTrue();
        }

        @Test
        void loadWithBlankPathFields_fallsBackToDefaults() {
            cleanupDefaultDirs();

            String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "blank-path-agent"
                  deadLetterPersistencePath = ""
                }
                """;

            AgentConfiguration cfg = AgentConfiguration.load(ConfigFactory.parseString(configStr));

            assertThat(cfg.getDeadLetterPersistencePath())
                .isEqualTo(DEFAULT_DEAD_LETTER_DIR.toString());
            assertThat(Files.exists(DEFAULT_DEAD_LETTER_DIR)).isTrue();
        }

        @Test
        void loadWithExplicitPaths_usesCustomPaths() {
            String configStr = """
                dag-agent {
                  agentUrl = "http://localhost:8080"
                  dagServerUrl = "http://localhost:8080"
                  agentId = "custom-path-agent"
                  deadLetterPersistencePath = "/custom/dead-letter"
                }
                """;

            AgentConfiguration cfg = AgentConfiguration.load(ConfigFactory.parseString(configStr));

            assertThat(cfg.getDeadLetterPersistencePath()).isEqualTo("/custom/dead-letter");
        }

        @Test
        void loadFromClasspathConfigFile_success() {
            Config config = ConfigFactory.load("config/agent.conf");
            AgentConfiguration cfg = AgentConfiguration.load(config);

            assertThat(cfg.getAgentUrl()).isEqualTo("http://agent01.example.com:8081");
            assertThat(cfg.getDagServerUrl()).isEqualTo("http://dag-server:8080");
            assertThat(cfg.getAgentId()).isEqualTo("agent-from-classpath");
            assertThat(cfg.getAgentName()).isEqualTo("agent-01");
            assertThat(cfg.isAutoRegister()).isTrue();
            assertThat(cfg.isGenerateToken()).isTrue();
            assertThat(cfg.getMaxConcurrentTasks()).isEqualTo(4);
            assertThat(cfg.getMaxPendingTasks()).isEqualTo(100);
            assertThat(cfg.getDeadLetterPersistencePath()).isEqualTo("/tmp/dag-agent-dead-letter");
            assertThat(cfg.getToken()).isEmpty();
            assertThat(cfg.getSupportedExecutionKeys())
                .containsExactly(
                    "top.ilovemyhome.dagtask.core.ShellTaskExecution",
                    "top.ilovemyhome.dagtask.core.PythonTaskExecution"
                );
        }

        @Test
        void loadNullConfig_throwsNpe() {
            assertThatThrownBy(() -> AgentConfiguration.load(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Config cannot be null");
        }

        @Test
        void loadNullPathPrefix_throwsNpe() {
            assertThatThrownBy(() -> AgentConfiguration.load(null, ConfigFactory.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("PathPrefix cannot be null");
        }
    }

    // ============================================================================
    // 2. Builder Tests
    // ============================================================================
    @Nested
    class BuilderTests {

        @Test
        void buildWithAllFields_success() {
            AgentConfiguration cfg = AgentConfiguration.builder()
                .agentUrl("http://localhost:8081")
                .dagServerUrl("http://localhost:8080")
                .agentId("full-agent")
                .agentName("Full Agent")
                .autoRegister(false)
                .generateToken(true)
                .maxConcurrentTasks(8)
                .maxPendingTasks(200)
                .deadLetterPersistencePath("/custom/dead-letter")
                .token("my-token")
                .supportedExecutionKeys(List.of("shell", "python"))
                .build();

            assertThat(cfg.getAgentUrl()).isEqualTo("http://localhost:8081");
            assertThat(cfg.getDagServerUrl()).isEqualTo("http://localhost:8080");
            assertThat(cfg.getAgentId()).isEqualTo("full-agent");
            assertThat(cfg.getAgentName()).isEqualTo("Full Agent");
            assertThat(cfg.isAutoRegister()).isFalse();
            assertThat(cfg.isGenerateToken()).isTrue();
            assertThat(cfg.getMaxConcurrentTasks()).isEqualTo(8);
            assertThat(cfg.getMaxPendingTasks()).isEqualTo(200);
            assertThat(cfg.getDeadLetterPersistencePath()).isEqualTo("/custom/dead-letter");
            assertThat(cfg.getToken()).isEqualTo("my-token");
            assertThat(cfg.getSupportedExecutionKeys()).containsExactly("shell", "python");
        }

        @Test
        void buildWithMinimalFields_usesDefaults() {
            cleanupDefaultDirs();

            AgentConfiguration cfg = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("minimal-builder-agent")
                .build();

            assertThat(cfg.isAutoRegister()).isTrue();
            assertThat(cfg.isGenerateToken()).isFalse();
            assertThat(cfg.getMaxConcurrentTasks()).isEqualTo(4);
            assertThat(cfg.getMaxPendingTasks()).isEqualTo(100);
            assertThat(cfg.getAgentName()).isNull();
            assertThat(cfg.getToken()).isNull();
            assertThat(cfg.getSupportedExecutionKeys()).isEmpty();
            assertThat(cfg.getDeadLetterPersistencePath())
                .isEqualTo(DEFAULT_DEAD_LETTER_DIR.toString());
        }

        @Test
        void buildWithNullSupportedExecutionKeys_usesEmptyList() {
            AgentConfiguration cfg = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("null-keys-agent")
                .supportedExecutionKeys(null)
                .build();

            assertThat(cfg.getSupportedExecutionKeys()).isEmpty();
        }

        @Test
        void buildWithoutDagServerUrl_throwsNpe() {
            assertThatThrownBy(() -> AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .agentId("test")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dagServerUrl is required");
        }

        @Test
        void buildWithoutAgentUrl_throwsNpe() {
            assertThatThrownBy(() -> AgentConfiguration.builder()
                .dagServerUrl("http://localhost:8080")
                .agentId("test")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("agentUrl is required");
        }

        @Test
        void buildWithoutAgentId_throwsNpe() {
            assertThatThrownBy(() -> AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("agentId is required");
        }

        @Test
        void buildWithInvalidMaxConcurrentTasks_throwsIllegalArgument() {
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
        void buildWithInvalidMaxPendingTasks_throwsIllegalArgument() {
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
        void buildWithNegativeMaxConcurrentTasks_throwsIllegalArgument() {
            assertThatThrownBy(() -> AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("test")
                .maxConcurrentTasks(-1)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxConcurrentTasks must be > 0");
        }
    }

    // ============================================================================
    // 3. Setter Tests
    // ============================================================================
    @Nested
    class SetterTests {

        @Test
        void setToken_updatesTokenValue() {
            AgentConfiguration cfg = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("test")
                .build();

            assertThat(cfg.getToken()).isNull();

            cfg.setToken("new-token");
            assertThat(cfg.getToken()).isEqualTo("new-token");

            cfg.setToken(null);
            assertThat(cfg.getToken()).isNull();
        }

        @Test
        void setSupportedExecutionKeys_withNull_createsEmptyList() {
            AgentConfiguration cfg = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("test")
                .supportedExecutionKeys(List.of("shell"))
                .build();

            assertThat(cfg.getSupportedExecutionKeys()).containsExactly("shell");

            cfg.setSupportedExecutionKeys(null);
            assertThat(cfg.getSupportedExecutionKeys()).isEmpty();
        }

        @Test
        void settersUpdateAllFields() {
            AgentConfiguration cfg = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("test")
                .build();

            cfg.setAgentUrl("http://new:8080");
            cfg.setAgentName("new-name");
            cfg.setDagServerUrl("http://new-server:8080");
            cfg.setAgentId("new-id");
            cfg.setAutoRegister(false);
            cfg.setGenerateToken(true);
            cfg.setMaxConcurrentTasks(10);
            cfg.setMaxPendingTasks(500);
            cfg.setDeadLetterPersistencePath("/new/path");
            cfg.setSupportedExecutionKeys(List.of("java", "go"));

            assertThat(cfg.getAgentUrl()).isEqualTo("http://new:8080");
            assertThat(cfg.getAgentName()).isEqualTo("new-name");
            assertThat(cfg.getDagServerUrl()).isEqualTo("http://new-server:8080");
            assertThat(cfg.getAgentId()).isEqualTo("new-id");
            assertThat(cfg.isAutoRegister()).isFalse();
            assertThat(cfg.isGenerateToken()).isTrue();
            assertThat(cfg.getMaxConcurrentTasks()).isEqualTo(10);
            assertThat(cfg.getMaxPendingTasks()).isEqualTo(500);
            assertThat(cfg.getDeadLetterPersistencePath()).isEqualTo("/new/path");
            assertThat(cfg.getSupportedExecutionKeys()).containsExactly("java", "go");
            assertThat(cfg.getBaseUrl()).isEqualTo("http://new:8080");
        }
    }

    // ============================================================================
    // 4. Equals and HashCode Tests
    // ============================================================================
    @Nested
    class EqualsAndHashCodeTests {

        @Test
        void sameObject_returnsTrue() {
            AgentConfiguration cfg = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("same")
                .build();

            assertThat(cfg.equals(cfg)).isTrue();
        }

        @Test
        void nullObject_returnsFalse() {
            AgentConfiguration cfg = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("test")
                .build();

            assertThat(cfg.equals(null)).isFalse();
        }

        @Test
        void differentClass_returnsFalse() {
            AgentConfiguration cfg = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("test")
                .build();

            assertThat(cfg.equals("not-a-config")).isFalse();
        }

        @Test
        void sameAgentId_returnsTrue() {
            AgentConfiguration a = AgentConfiguration.builder()
                .agentUrl("http://a:8080")
                .dagServerUrl("http://a:8080")
                .agentId("shared-id")
                .build();
            AgentConfiguration b = AgentConfiguration.builder()
                .agentUrl("http://b:9090")
                .dagServerUrl("http://b:9090")
                .agentId("shared-id")
                .build();

            assertThat(a.equals(b)).isTrue();
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        void differentAgentId_returnsFalse() {
            AgentConfiguration a = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("agent-a")
                .build();
            AgentConfiguration b = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("agent-b")
                .build();

            assertThat(a.equals(b)).isFalse();
        }
    }

    // ============================================================================
    // 5. ToString Tests
    // ============================================================================
    @Nested
    class ToStringTests {

        @Test
        void toString_containsKeyFields() {
            AgentConfiguration cfg = AgentConfiguration.builder()
                .agentUrl("http://localhost:8081")
                .dagServerUrl("http://localhost:8080")
                .agentId("toString-agent")
                .agentName("Test Agent")
                .autoRegister(true)
                .maxConcurrentTasks(4)
                .maxPendingTasks(100)
                .deadLetterPersistencePath("/tmp/dead-letter")
                .token("secret")
                .supportedExecutionKeys(List.of("shell"))
                .build();

            String str = cfg.toString();

            assertThat(str).contains("agentUrl='http://localhost:8081'");
            assertThat(str).contains("dagServerUrl='http://localhost:8080'");
            assertThat(str).contains("agentId='toString-agent'");
            assertThat(str).contains("agentName='Test Agent'");
            assertThat(str).contains("autoRegister=true");
            assertThat(str).contains("maxConcurrentTasks=4");
            assertThat(str).contains("maxPendingTasks=100");
            assertThat(str).contains("deadLetterPersistencePath='/tmp/dead-letter'");
            assertThat(str).contains("hasToken=true");
            assertThat(str).contains("supportedExecutionKeysCount=1");
        }

        @Test
        void toString_withNoToken_showsHasTokenFalse() {
            AgentConfiguration cfg = AgentConfiguration.builder()
                .agentUrl("http://localhost:8080")
                .dagServerUrl("http://localhost:8080")
                .agentId("no-token-agent")
                .build();

            String str = cfg.toString();

            assertThat(str).contains("hasToken=false");
        }
    }
}
