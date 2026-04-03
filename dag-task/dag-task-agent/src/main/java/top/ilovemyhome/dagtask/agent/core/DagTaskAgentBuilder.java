package top.ilovemyhome.dagtask.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.ilovemyhome.dagtask.agent.client.DagServerClient;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Fluent builder for creating {@link DagTaskAgent} instances.
 * All parameters are required.
 */
public class DagTaskAgentBuilder {

    private AgentConfiguration config;
    private ObjectMapper objectMapper;
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
     * Sets the ObjectMapper for JSON processing (required).
     */
    public DagTaskAgentBuilder objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    /**
     * Sets the DagServerClient for communicating with DAG server (required).
     */
    public DagTaskAgentBuilder dagServerClient(DagServerClient dagServerClient) {
        this.dagServerClient = dagServerClient;
        return this;
    }

    /**
     * Sets the ExecutorService for task execution (required).
     */
    public DagTaskAgentBuilder taskExecutor(ExecutorService taskExecutor) {
        this.taskExecutor = taskExecutor;
        return this;
    }

    /**
     * Builds the DagTaskAgent instance.
     * All parameters must be set before building.
     *
     * @return the built agent
     * @throws NullPointerException if any required parameter is missing
     */
    public DagTaskAgent build() {
        Objects.requireNonNull(config, "AgentConfiguration is required");
        Objects.requireNonNull(objectMapper, "ObjectMapper is required");
        Objects.requireNonNull(dagServerClient, "DagServerClient is required");
        Objects.requireNonNull(taskExecutor, "ExecutorService is required");

        return new DagTaskAgent(config, dagServerClient, taskExecutor, objectMapper);
    }
}
