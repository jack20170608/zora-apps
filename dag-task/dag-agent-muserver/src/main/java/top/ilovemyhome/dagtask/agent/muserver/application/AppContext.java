package top.ilovemyhome.dagtask.agent.muserver.application;

import com.typesafe.config.Config;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgent;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgentBuilder;
import top.ilovemyhome.dagtask.agent.client.DefaultAgentSchedulerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;

import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * Application context that holds all dependencies and components for the MuServer hosted DAG agent.
 */
public class AppContext {

    private final String env;
    private final Config config;
    private final AgentConfiguration agentConfiguration;
    private final DagTaskAgent dagTaskAgent;
    private final ObjectMapper objectMapper;

    public AppContext(String env, Config config) {
        this.env = Objects.requireNonNull(env, "env is required");
        this.config = Objects.requireNonNull(config, "config is required");

        LOGGER.info("Initializing application context for environment: {}", env);

        // Load agent configuration
        this.agentConfiguration = AgentConfiguration.load(config.getConfig("dag-agent"));

        // Initialize object mapper
        this.objectMapper = new ObjectMapper();

        // Create DAG task agent
        AgentSchedulerClient agentSchedulerClient = new DefaultAgentSchedulerClient(agentConfiguration);
        var executor = Executors.newFixedThreadPool(agentConfiguration.getMaxConcurrentTasks());
        this.dagTaskAgent = new DagTaskAgentBuilder()
                .config(agentConfiguration)
                .objectMapper(objectMapper)
                .agentSchedulerClient(agentSchedulerClient)
                .taskExecutor(executor)
                .build();

        LOGGER.info("Application context initialized successfully");
    }

    public String getEnv() {
        return env;
    }

    public Config getConfig() {
        return config;
    }

    public AgentConfiguration getAgentConfiguration() {
        return agentConfiguration;
    }

    public DagTaskAgent getDagTaskAgent() {
        return dagTaskAgent;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AppContext.class);
}
