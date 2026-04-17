package top.ilovemyhome.dagtask.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.ilovemyhome.dagtask.agent.client.DefaultAgentSchedulerClient;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgent;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgentBuilder;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;

import java.util.Objects;
import java.util.concurrent.Executors;

public class AgentStarter {

    private AgentStarter() {
        // Utility class
    }

    public static void start(AgentConfiguration config) {
        start(config, null);
    }

    public static void start(AgentConfiguration agentConfig, ObjectMapper objectMapper) {
        Objects.requireNonNull(agentConfig, "agentConfig is required");
        AgentSchedulerClient agentSchedulerClient = new DefaultAgentSchedulerClient(agentConfig);
        var executor = Executors.newFixedThreadPool(agentConfig.getMaxConcurrentTasks());

        // Create and start agent
        DagTaskAgent agent = new DagTaskAgentBuilder()
                .config(agentConfig)
                .objectMapper(Objects.nonNull(objectMapper) ? objectMapper : new ObjectMapper())
                .agentSchedulerClient(agentSchedulerClient)
                .taskExecutor(executor)
                .build();
        agent.start();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(agent::stop));
    }
}
