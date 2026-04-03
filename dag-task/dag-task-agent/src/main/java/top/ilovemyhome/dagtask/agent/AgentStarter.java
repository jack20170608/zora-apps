package top.ilovemyhome.dagtask.agent;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgent;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgentBuilder;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;


public class AgentStarter {

    public static void start(){
        Config config = ConfigFactory.load("config/agent.conf");
        start(config);
    }

    public static void start(Config config) {
        // Load configuration using Typesafe Config
        AgentConfiguration agentConfig = AgentConfiguration.load(config);

        // Create and start agent
        DagTaskAgent agent = new DagTaskAgentBuilder()
                .config(agentConfig)
                .build();
        agent.start();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            agent.stop();
        }));
    }



}
