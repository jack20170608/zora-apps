package top.ilovemyhome.dagtask.agent.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.client.DefaultAgentSchedulerClient;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.agent.api.TaskAgentResource;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DAG task agent that manages task execution lifecycle.
 * Core agent component without direct dependency on any specific HTTP server implementation.
 * HTTP server embedding is handled by separate starter classes (e.g. MuServerStarter).
 */
public class DagTaskAgent {

    private final AgentConfiguration config;
    private final AgentSchedulerClient agentSchedulerClient;
    private final ExecutorService taskExecutor;
    private final TaskExecutionEngine executionEngine;
    private final TaskAgentResource resource;
    private boolean running = false;

    /**
     * Creates a DagTaskAgent with default dependencies.
     * Uses default AgentSchedulerClient and creates a new fixed thread pool.
     * Creates a new ObjectMapper internally.
     */
    public DagTaskAgent(AgentConfiguration config) {
        this.config = config;
        this.agentSchedulerClient = new DefaultAgentSchedulerClient(config);
        this.taskExecutor = Executors.newFixedThreadPool(config.getMaxConcurrentTasks());
        ObjectMapper objectMapper = new ObjectMapper();
        this.executionEngine = new TaskExecutionEngine(config, agentSchedulerClient, taskExecutor, objectMapper);
        this.resource = new TaskAgentResource(this, executionEngine);
    }

    /**
     * Creates a DagTaskAgent with provided AgentSchedulerClient and ExecutorService.
     * Creates a new ObjectMapper internally.
     */
    public DagTaskAgent(AgentConfiguration config, AgentSchedulerClient agentSchedulerClient, ExecutorService taskExecutor) {
        this.config = config;
        this.agentSchedulerClient = agentSchedulerClient;
        if (taskExecutor != null) {
            this.taskExecutor = taskExecutor;
        } else {
            this.taskExecutor = Executors.newFixedThreadPool(config.getMaxConcurrentTasks());
        }
        ObjectMapper objectMapper = new ObjectMapper();
        this.executionEngine = new TaskExecutionEngine(config, agentSchedulerClient, taskExecutor, objectMapper);
        this.resource = new TaskAgentResource(this, executionEngine);
    }

    /**
     * Creates a DagTaskAgent with all dependencies provided explicitly.
     * All parameters are required.
     */
    public DagTaskAgent(AgentConfiguration config, AgentSchedulerClient agentSchedulerClient,
                        ExecutorService taskExecutor, ObjectMapper objectMapper) {
        this.config = Objects.requireNonNull(config, "config is required");
        this.agentSchedulerClient = Objects.requireNonNull(agentSchedulerClient, "AgentSchedulerClient is required");
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor is required");
        Objects.requireNonNull(objectMapper, "objectMapper is required");
        this.executionEngine = new TaskExecutionEngine(config, agentSchedulerClient, taskExecutor, objectMapper);
        this.resource = new TaskAgentResource(this, executionEngine);
    }


    /**
     * Start the agent: start queue processor and auto-register with server.
     * Does NOT start HTTP server - HTTP server is started by external starter.
     */
    public void start() {
        // Start the task queue processor
        executionEngine.start();
        running = true;

        LOGGER.info("DAG Task Agent started, agentUrl={}, maxConcurrent={}, maxPending={}",
                config.getAgentUrl(), config.getMaxConcurrentTasks(), config.getMaxPendingTasks());

        // Auto-register with server
        if (config.isAutoRegister()) {
            var registration = new AgentRegisterRequest(
                    config.getAgentId(),
                    config.getBaseUrl(),
                    config.getMaxConcurrentTasks(),
                    config.getMaxPendingTasks(),
                    config.getSupportedExecutionKeys()
            );
            var response = agentSchedulerClient.register(registration);
            boolean registered = response.getStatus() >= 200 && response.getStatus() < 300;
            if (!registered) {
                LOGGER.warn("Auto-registration failed. Agent will not be known to the DAG scheduler.");
            }
        }
    }

    /**
     * Stop the agent without unregistering from the server.
     */
    public void stop() {
        stop(false);
    }

    /**
     * Stop the agent and optionally unregister from the DAG server.
     *
     * @param unregister whether to unregister from the DAG server before stopping
     */
    public void stop(boolean unregister) {
        running = false;
        executionEngine.stop();
        if (unregister) {
            var unregistration = new AgentUnregistration(config.getAgentId());
            var response = agentSchedulerClient.unregister(unregistration);
            boolean success = response.getStatus() >= 200 && response.getStatus() < 300;
            if (!success) {
                LOGGER.warn("Failed to unregister agent from DAG server during shutdown");
            }
        }
        taskExecutor.shutdown();
        LOGGER.info("DAG Task Agent stopped{}", unregister ? " (unregistered)" : "");
    }

    /**
     * Get the current agent status.
     */
    public boolean isRunning() {
        return running;
    }

    public AgentConfiguration getConfig() {
        return config;
    }


    public ExecutorService getTaskExecutor() {
        return taskExecutor;
    }

    public TaskExecutionEngine getExecutionEngine() {
        return executionEngine;
    }

    public TaskAgentResource getResource() {
        return resource;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DagTaskAgent.class);
}
