package top.ilovemyhome.dagtask.agent.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import top.ilovemyhome.dagtask.agent.client.DagServerClient;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgent;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgentBuilder;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * Command-line main entry point for starting the DAG task agent with MuServer.
 * Loads configuration from classpath:config/agent.conf by default.
 * <p>
 * This class only exists when MuServer is available on the classpath.
 */
public class AgentMain {

    private AgentMain() {
        // Utility class
    }

    /**
     * Starts the agent with the given Typesafe Config.
     * Starts MuServer and registers shutdown hook.
     *
     * @param config the Typesafe Config to load agent configuration from
     * @throws IOException if MuServer fails to start
     */
    public static void start(Config config) throws IOException {
        // Load configuration using Typesafe Config
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        // Create all dependencies
        ObjectMapper objectMapper = new ObjectMapper();
        DagServerClient dagServerClient = new DagServerClient(agentConfig);
        var executor = Executors.newFixedThreadPool(agentConfig.getMaxConcurrentTasks());

        // Build and start agent
        DagTaskAgent agent = new DagTaskAgentBuilder()
                .config(agentConfig)
                .objectMapper(objectMapper)
                .dagServerClient(dagServerClient)
                .taskExecutor(executor)
                .build();
        agent.start();

        // Start MuServer
        MuServerStarter starter = new MuServerStarter(agent);
        starter.start();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            starter.stop();
            agent.stop();
        }));
    }

    /**
     * Command-line main entry point.
     * Loads configuration from {@code classpath:config/agent.conf} by default.
     */
    public static void main(String[] args) throws IOException {
        // Load config from classpath:config/agent.conf
        Config config = ConfigFactory.load("config/agent");
        start(config);
    }
}
