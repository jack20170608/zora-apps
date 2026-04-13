package top.ilovemyhome.dagtask.agent.config;

import com.typesafe.config.Config;
import top.ilovemyhome.zora.config.ConfigLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration holder for the DAG task agent.
 */
public class AgentConfiguration {

    private String agentUrl;
    private String dagServerUrl;
    private String agentId;
    private boolean autoRegister = true;
    private int maxConcurrentTasks = 4;
    private int maxPendingTasks = 100;
    private String deadLetterPersistencePath;
    private List<String> supportedExecutionKeys = new ArrayList<>();

    // Required for Typesafe Config bean reflection
    public AgentConfiguration() {
    }

    private AgentConfiguration(Builder builder) {
        this.agentUrl = builder.agentUrl;
        this.dagServerUrl = builder.dagServerUrl;
        this.agentId = builder.agentId;
        this.autoRegister = builder.autoRegister;
        this.maxConcurrentTasks = builder.maxConcurrentTasks;
        this.maxPendingTasks = builder.maxPendingTasks;
        this.deadLetterPersistencePath = builder.deadLetterPersistencePath;
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

    public boolean isAutoRegister() {
        return autoRegister;
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

    public void setAgentUrl(String agentUrl) {
        this.agentUrl = agentUrl;
    }

    public void setDagServerUrl(String dagServerUrl) {
        this.dagServerUrl = dagServerUrl;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public void setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
    }

    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    public void setMaxPendingTasks(int maxPendingTasks) {
        this.maxPendingTasks = maxPendingTasks;
    }

    public void setSupportedExecutionKeys(List<String> supportedExecutionKeys) {
        this.supportedExecutionKeys = new ArrayList<>(supportedExecutionKeys);
    }

    public String getDeadLetterPersistencePath() {
        return deadLetterPersistencePath;
    }

    public void setDeadLetterPersistencePath(String deadLetterPersistencePath) {
        this.deadLetterPersistencePath = deadLetterPersistencePath;
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
        return ConfigLoader.loadConfigAsBean(config, pathPrefix, AgentConfiguration.class);
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
                ", autoRegister=" + autoRegister +
                ", maxConcurrentTasks=" + maxConcurrentTasks +
                ", maxPendingTasks=" + maxPendingTasks +
                ", deadLetterPersistencePath='" + deadLetterPersistencePath + '\'' +
                ", supportedExecutionKeysCount=" + supportedExecutionKeys.size() +
                '}';
    }


    public static class Builder {
        private String agentUrl;
        private String dagServerUrl;
        private String agentId;
        private boolean autoRegister = true;
        private int maxConcurrentTasks = 4;
        private int maxPendingTasks = 100;
        private String deadLetterPersistencePath;
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

        public Builder autoRegister(boolean autoRegister) {
            this.autoRegister = autoRegister;
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
            return new AgentConfiguration(this);
        }
    }
}
