package top.ilovemyhome.dagtask.agent.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import top.ilovemyhome.zora.config.ConfigLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration holder for the DAG task agent.
 */
public class AgentConfiguration {

    private static final String DEFAULT_BASE_DIR = Path.of(
        System.getProperty("user.home"), ".dag-agent").toString();
    private static final String DEFAULT_DEAD_LETTER_DIR = Path.of(
        DEFAULT_BASE_DIR, "dead-letter").toString();
    private static final String DEFAULT_TOKEN_DIR = Path.of(
        DEFAULT_BASE_DIR, "token").toString();

    private static final Config DEFAULTS = ConfigFactory.parseString("""
        deadLetterPersistencePath = ""
        tokenFilePath = ""
        token = ""
        supportedExecutionKeys = []
        autoRegister = true
        maxConcurrentTasks = 4
        maxPendingTasks = 100
        """);

    private String agentUrl;
    private String agentName;
    private String dagServerUrl;
    private String agentId;
    private boolean autoRegister = true;
    private boolean generateToken = false;
    private int maxConcurrentTasks = 4;
    private int maxPendingTasks = 100;
    private String deadLetterPersistencePath;
    private String tokenFilePath = null;
    private String token = null;
    private List<String> supportedExecutionKeys = new ArrayList<>();

    // Required for Typesafe Config bean reflection
    public AgentConfiguration() {
    }

    private static void resolveAndCreatePaths(AgentConfiguration config) {
        if (config.deadLetterPersistencePath == null || config.deadLetterPersistencePath.isBlank()) {
            config.deadLetterPersistencePath = DEFAULT_DEAD_LETTER_DIR;
        }
        if (config.tokenFilePath == null || config.tokenFilePath.isBlank()) {
            config.tokenFilePath = DEFAULT_TOKEN_DIR;
        }
        createDirectory(config.deadLetterPersistencePath);
        createDirectory(config.tokenFilePath);
    }

    private static void createDirectory(String path) {
        try {
            Files.createDirectories(Path.of(path));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory: " + path, e);
        }
    }

    private AgentConfiguration(Builder builder) {
        this.agentUrl = builder.agentUrl;
        this.dagServerUrl = builder.dagServerUrl;
        this.agentId = builder.agentId;
        this.agentName = builder.agentName;
        this.autoRegister = builder.autoRegister;
        this.generateToken = builder.generateToken;
        this.maxConcurrentTasks = builder.maxConcurrentTasks;
        this.maxPendingTasks = builder.maxPendingTasks;
        this.deadLetterPersistencePath = builder.deadLetterPersistencePath;
        this.tokenFilePath = builder.tokenFilePath;
        this.token = builder.token;
        if (builder.supportedExecutionKeys != null) {
            this.supportedExecutionKeys = new ArrayList<>(builder.supportedExecutionKeys);
        }
        // else keep the default empty list
    }

    public String getAgentUrl() {
        return agentUrl;
    }

    public String getDagServerUrl() {
        return dagServerUrl;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public boolean isAutoRegister() {
        return autoRegister;
    }

    public boolean isGenerateToken() {
        return generateToken;
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public int getMaxPendingTasks() {
        return maxPendingTasks;
    }

    public List<String> getSupportedExecutionKeys() {
        return new ArrayList<>(supportedExecutionKeys);
    }

    public String getDeadLetterPersistencePath() {
        return deadLetterPersistencePath;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getBaseUrl() {
        return agentUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AgentConfiguration load(Config config) {
        return load("dag-agent", config);
    }

    public static AgentConfiguration load(String pathPrefix, Config config){
        Objects.requireNonNull(config, "Config cannot be null");
        Objects.requireNonNull(pathPrefix, "PathPrefix cannot be null");
        Config prefixConfig = config.hasPath(pathPrefix)
            ? config.getConfig(pathPrefix)
            : ConfigFactory.empty();
        Config merged = prefixConfig.withFallback(DEFAULTS);
        Config wrapped = merged.atPath(pathPrefix);
        AgentConfiguration result = ConfigLoader.loadConfigAsBean(wrapped, pathPrefix, AgentConfiguration.class);
        resolveAndCreatePaths(result);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentConfiguration that = (AgentConfiguration) o;
        return Objects.equals(agentId, that.agentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentId);
    }

    @Override
    public String toString() {
        return "AgentConfiguration{" +
                "agentUrl='" + agentUrl + '\'' +
                ", dagServerUrl='" + dagServerUrl + '\'' +
                ", agentId='" + agentId + '\'' +
                ", agentName='" + agentName + '\'' +
                ", autoRegister=" + autoRegister +
                ", maxConcurrentTasks=" + maxConcurrentTasks +
                ", maxPendingTasks=" + maxPendingTasks +
                ", deadLetterPersistencePath='" + deadLetterPersistencePath + '\'' +
                ", tokenFilePath='" + tokenFilePath + '\'' +
                ", hasToken=" + (token != null && !token.isEmpty()) +
                ", supportedExecutionKeysCount=" + supportedExecutionKeys.size() +
                '}';
    }


    public static class Builder {
        private String agentUrl;
        private String dagServerUrl;
        private String agentId;
        private String agentName;
        private boolean autoRegister = true;
        private boolean generateToken = false;
        private int maxConcurrentTasks = 4;
        private int maxPendingTasks = 100;
        private String deadLetterPersistencePath;
        private String tokenFilePath;
        private String token;
        private List<String> supportedExecutionKeys;

        public Builder agentUrl(String agentUrl) {
            this.agentUrl = agentUrl;
            return this;
        }

        public Builder dagServerUrl(String dagServerUrl) {
            this.dagServerUrl = dagServerUrl;
            return this;
        }

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }

        public Builder autoRegister(boolean autoRegister) {
            this.autoRegister = autoRegister;
            return this;
        }

        public Builder generateToken(boolean generateToken) {
            this.generateToken = generateToken;
            return this;
        }

        public Builder maxConcurrentTasks(int maxConcurrentTasks) {
            this.maxConcurrentTasks = maxConcurrentTasks;
            return this;
        }

        public Builder maxPendingTasks(int maxPendingTasks) {
            this.maxPendingTasks = maxPendingTasks;
            return this;
        }

        public Builder deadLetterPersistencePath(String deadLetterPersistencePath) {
            this.deadLetterPersistencePath = deadLetterPersistencePath;
            return this;
        }

        public Builder tokenFilePath(String tokenFilePath) {
            this.tokenFilePath = tokenFilePath;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder supportedExecutionKeys(List<String> supportedExecutionKeys) {
            this.supportedExecutionKeys = supportedExecutionKeys;
            return this;
        }

        public AgentConfiguration build() {
            Objects.requireNonNull(dagServerUrl, "dagServerUrl is required");
            Objects.requireNonNull(agentId, "agentId is required");
            Objects.requireNonNull(agentUrl, "agentUrl is required");
            if (maxConcurrentTasks <= 0) {
                throw new IllegalArgumentException("maxConcurrentTasks must be > 0");
            }
            if (maxPendingTasks <= 0) {
                throw new IllegalArgumentException("maxPendingTasks must be > 0");
            }
            AgentConfiguration result = new AgentConfiguration(this);
            resolveAndCreatePaths(result);
            return result;
        }
    }
}
