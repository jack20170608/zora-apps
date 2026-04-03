package top.ilovemyhome.dagtask.agent.core;

import top.ilovemyhome.dagtask.agent.client.DagServerClient;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;

import java.util.concurrent.ExecutorService;

/**
 * Fluent builder for creating {@link DagTaskAgent} instances.
 * Provides convenient configuration and construction.
 */
public class DagTaskAgentBuilder {

    private AgentConfiguration config;
    private DagServerClient dagServerClient;
    private ExecutorService taskExecutor;

    /**
     * Sets the agent configuration (required).
     */
    public DagTaskAgentBuilder config(AgentConfiguration config) {
        this.config = config;
        return this;
    }

    /**
     * Sets a custom DagServerClient (optional).
     * If not provided, a default client will be created.
     */
    public DagTaskAgentBuilder dagServerClient(DagServerClient dagServerClient) {
        this.dagServerClient = dagServerClient;
        return this;
    }

    /**
     * Sets a custom ExecutorService (optional).
     * If not provided, a fixed thread pool will be created based on configuration.
     */
    public DagTaskAgentBuilder taskExecutor(ExecutorService taskExecutor) {
        this.taskExecutor = taskExecutor;
        return this;
    }

    /**
     * Builds the DagTaskAgent instance.
     *
     * @return the built agent
     * @throws IllegalArgumentException if required configuration is missing
     */
    public DagTaskAgent build() {
        if (config == null) {
            throw new IllegalArgumentException("AgentConfiguration is required");
        }

        if (dagServerClient != null && taskExecutor != null) {
            return new DagTaskAgent(config, dagServerClient, taskExecutor);
        }

        if (dagServerClient != null) {
            return new DagTaskAgent(config, dagServerClient, null);
        }

        return new DagTaskAgent(config);
    }
}
