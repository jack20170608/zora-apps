package top.ilovemyhome.dagtask.agent.muserver.application;

import com.typesafe.config.Config;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgent;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgentBuilder;
import top.ilovemyhome.dagtask.agent.client.DefaultAgentSchedulerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * Application context that holds all dependencies and components for the MuServer hosted DAG agent.
 */
public class AppContext {

    private final String env;
    private final Config config;
    private final DagTaskAgent dagTaskAgent;

    public AppContext(String env, Config config) {
        this.env = Objects.requireNonNull(env, "env is required");
        this.config = Objects.requireNonNull(config, "config is required");
        logger.info("Initializing application context for environment: {}", env);
        var agentConfiguration = AgentConfiguration.load("dag-agent", config);
        logger.info("Loading agent: {}", agentConfiguration);
        // Create DAG task agent
        AgentSchedulerClient agentSchedulerClient = new DefaultAgentSchedulerClient(agentConfiguration);
        var executor = Executors.newFixedThreadPool(agentConfiguration.getMaxConcurrentTasks());
        this.dagTaskAgent = new DagTaskAgentBuilder()
                .config(agentConfiguration)
                .objectMapper(JacksonUtil.MAPPER)
                .agentSchedulerClient(agentSchedulerClient)
                .taskExecutor(executor)
                .build();
        logger.info("Application context initialized successfully");
    }

    public String getEnv() {
        return env;
    }

    public Config getConfig() {
        return config;
    }


    public DagTaskAgent getDagTaskAgent() {
        return dagTaskAgent;
    }

    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);
}
