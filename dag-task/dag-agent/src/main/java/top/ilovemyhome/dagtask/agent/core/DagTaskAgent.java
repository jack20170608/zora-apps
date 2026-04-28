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
    public DagTaskAgent(AgentConfiguration config
        , AgentSchedulerClient agentSchedulerClient
        , ExecutorService taskExecutor
        , ObjectMapper objectMapper) {
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
                    config.getAgentName(),
                    config.getAgentUrl(),
                    config.getMaxConcurrentTasks(),
                    config.getMaxPendingTasks(),
                    config.getSupportedExecutionKeys(),
                    config.isGenerateToken()
            );
            var response = agentSchedulerClient.register(registration);
            boolean registeredSuccess = response.getStatus() >= 200 && response.getStatus() < 300;
            if (registeredSuccess) {
                registered = true;
                int taskCount = registration.supportedExecutionKeys().size();
                LOGGER.info("Agent {} successfully registered with DAG server at {}, supported {} execution keys",
                        registration.agentId(), config.getDagServerUrl(), taskCount);
            } else {
                LOGGER.warn("Initial auto-registration failed with status {}, starting background retry thread",
                        response.getStatus());
                registered = false;
                RegistrationRetryTask retryTask = new RegistrationRetryTask(registration);
                registrationRetryThread = new Thread(retryTask);
                registrationRetryThread.setDaemon(true);
                registrationRetryThread.setName("dag-agent-registration-retry");
                registrationRetryThread.start();
            }
        } else {
            registered = false;
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
        // Interrupt registration retry thread if it's still running
        if (registrationRetryThread != null && registrationRetryThread.isAlive()) {
            registrationRetryThread.interrupt();
        }
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

    /**
     * Check if the agent is successfully registered.
     * Package-private for testing.
     */
    boolean isRegistered() {
        return registered;
    }

    /**
     * Get the registration retry thread.
     * Package-private for testing.
     */
    Thread getRegistrationRetryThread() {
        return registrationRetryThread;
    }

    // Auto-registration retry constants (simulated annealing / exponential backoff)
    static final long INITIAL_DELAY_MS = 10L;
    static final long MAX_DELAY_MS = 5 * 60 * 1000L; // 5 minutes
    private static final double JITTER_FACTOR = 0.1;

    // Fields for registration retry tracking
    private volatile boolean registered;
    private volatile Thread registrationRetryThread;

    /**
     * Background task that retries registration with exponential backoff (simulated annealing).
     * Retries until successful or until the agent is stopped.
     */
    private class RegistrationRetryTask implements Runnable {
        private final AgentRegisterRequest registration;
        private long currentDelayMs;

        /**
         * Creates a new retry task for the given registration.
         * @param registration the registration request to retry
         */
        public RegistrationRetryTask(AgentRegisterRequest registration) {
            this.registration = registration;
            this.currentDelayMs = INITIAL_DELAY_MS;
        }

        @Override
        public void run() {
            while (!registered && isRunning()) {
                try {
                    long jitteredDelay = applyJitter(currentDelayMs);
                    Thread.sleep(jitteredDelay);

                    var response = agentSchedulerClient.register(registration);
                    boolean success = response.getStatus() >= 200 && response.getStatus() < 300;

                    if (success) {
                        int taskCount = registration.supportedExecutionKeys().size();
                        LOGGER.info("Agent {} registered successfully after retry, supported {} execution keys",
                                registration.agentId(), taskCount);
                        registered = true;
                        return;
                    }

                    LOGGER.warn("Registration retry failed with status {}, will retry in {}ms",
                            response.getStatus(), currentDelayMs);

                    // Exponential backoff, capped at MAX_DELAY_MS
                    currentDelayMs = Math.min(currentDelayMs * 2, MAX_DELAY_MS);
                } catch (InterruptedException e) {
                    LOGGER.info("Registration retry thread interrupted, stopping retry");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Applies random jitter to the delay to avoid thundering herd problem.
     * Adds ±JITTER_FACTOR random variation.
     * @param delay the base delay in milliseconds
     * @return jittered delay
     */
    long applyJitter(long delay) { // package-private for testing
        double random = (Math.random() * 2 - 1) * JITTER_FACTOR; // -0.1 to +0.1
        double jittered = delay * (1 + random);
        return Math.round(jittered);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DagTaskAgent.class);
}
