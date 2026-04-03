package top.ilovemyhome.dagtask.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import top.ilovemyhome.dagtask.agent.client.DagServerClient;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgent;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgentBuilder;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;

import java.util.Objects;
import java.util.concurrent.Executors;

public class AgentStarter {

    private AgentStarter() {
        // Utility class
    }

    public static void start(){
        Config config = ConfigFactory.load("config/agent.conf");
        start(config);
    }

    public static void start(Config config) {
        Objects.requireNonNull(config, "config is required");

        // Load configuration using Typesafe Config
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        // Create default dependencies
        ObjectMapper objectMapper = new ObjectMapper();
        DagServerClient dagServerClient = new DagServerClient(agentConfig);
        var executor = Executors.newFixedThreadPool(agentConfig.getMaxConcurrentTasks());

        // Create and start agent
        DagTaskAgent agent = new DagTaskAgentBuilder()
                .config(agentConfig)
                .objectMapper(objectMapper)
                .dagServerClient(dagServerClient)
                .taskExecutor(executor)
                .build();
        agent.start();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            agent.stop();
        }));
    }

}
