package top.ilovemyhome.dagtask.agent.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.client.DagServerClient;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.agent.api.TaskAgentResource;
import top.ilovemyhome.dagtask.si.TaskFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final DagServerClient dagServerClient;
    private final ExecutorService taskExecutor;
    private final TaskExecutionManager executionManager;
    private final TaskAgentResource resource;
    private boolean running = false;

    /**
     * Creates a DagTaskAgent with default dependencies.
     * Uses default DagServerClient and creates a new fixed thread pool.
     * Creates a new ObjectMapper internally.
     */
    public DagTaskAgent(AgentConfiguration config) {
        this.config = config;
        this.dagServerClient = new DagServerClient(config);
        this.taskExecutor = Executors.newFixedThreadPool(config.getMaxConcurrentTasks());
        ObjectMapper objectMapper = new ObjectMapper();
        TaskFactory taskFactory = new DefaultTaskFactory();
        this.executionManager = new TaskExecutionManager(config, dagServerClient, taskExecutor, objectMapper, taskFactory);
        this.resource = new TaskAgentResource(this, executionManager);
    }

    /**
     * Creates a DagTaskAgent with provided DagServerClient and ExecutorService.
     * Creates a new ObjectMapper internally.
     */
    public DagTaskAgent(AgentConfiguration config, DagServerClient dagServerClient, ExecutorService taskExecutor) {
        this.config = config;
        this.dagServerClient = dagServerClient;
        if (taskExecutor != null) {
            this.taskExecutor = taskExecutor;
        } else {
            this.taskExecutor = Executors.newFixedThreadPool(config.getMaxConcurrentTasks());
        }
        ObjectMapper objectMapper = new ObjectMapper();
        TaskFactory taskFactory = new DefaultTaskFactory();
        this.executionManager = new TaskExecutionManager(config, dagServerClient, taskExecutor, objectMapper, taskFactory);
        this.resource = new TaskAgentResource(this, executionManager);
    }

    /**
     * Creates a DagTaskAgent with all dependencies provided explicitly.
     * All parameters are required.
     */
    public DagTaskAgent(AgentConfiguration config, DagServerClient dagServerClient,
                        ExecutorService taskExecutor, ObjectMapper objectMapper) {
        this.config = Objects.requireNonNull(config, "config is required");
        this.dagServerClient = Objects.requireNonNull(dagServerClient, "dagServerClient is required");
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor is required");
        Objects.requireNonNull(objectMapper, "objectMapper is required");
        TaskFactory taskFactory = new DefaultTaskFactory();
        this.executionManager = new TaskExecutionManager(config, dagServerClient, taskExecutor, objectMapper, taskFactory);
        this.resource = new TaskAgentResource(this, executionManager);
    }


    /**
     * Start the agent: start queue processor and auto-register with server.
     * Does NOT start HTTP server - HTTP server is started by external starter.
     */
    public void start() {
        // Start the task queue processor
        executionManager.start();
        running = true;

        LOGGER.info("DAG Task Agent started, agentUrl={}, maxConcurrent={}, maxPending={}",
                config.getAgentUrl(), config.getMaxConcurrentTasks(), config.getMaxPendingTasks());

        // Auto-register with server
        if (config.isAutoRegister()) {
            boolean registered = dagServerClient.register();
            if (!registered) {
                LOGGER.warn("Auto-registration failed. Agent will not be known to the DAG server.");
            }
        }
    }

    /**
     * Stop the agent.
     */
    public void stop() {
        running = false;
        executionManager.stop();
        taskExecutor.shutdown();
        LOGGER.info("DAG Task Agent stopped");
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

    public TaskExecutionManager getExecutionManager() {
        return executionManager;
    }

    public TaskAgentResource getResource() {
        return resource;
    }

    private static class DefaultTaskFactory implements TaskFactory {
        // Use default implementation
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DagTaskAgent.class);
}
