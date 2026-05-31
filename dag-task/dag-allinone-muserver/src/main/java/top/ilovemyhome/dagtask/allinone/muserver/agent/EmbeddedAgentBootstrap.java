package top.ilovemyhome.dagtask.allinone.muserver.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.agent.core.DagTaskAgent;
import top.ilovemyhome.dagtask.agent.core.TaskExecutionEngine;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Boots the embedded agent within the same JVM process.
 * Registers a "local-agent" record in the database and wires
 * the in-process scheduler client for result reporting.
 */
public class EmbeddedAgentBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedAgentBootstrap.class);
    private static final String LOCAL_AGENT_ID = "local-agent";
    private static final String LOCAL_AGENT_URL = "http://localhost:8080";

    private final Config appConfig;
    private final Jdbi jdbi;
    private final AgentConfiguration agentConfig;
    private final DagTaskAgent dagTaskAgent;
    private final ExecutorService executorService;

    public EmbeddedAgentBootstrap(Config appConfig, Jdbi jdbi, AgentSchedulerClient schedulerClient) {
        this.appConfig = appConfig;
        this.jdbi = jdbi;
        this.agentConfig = buildAgentConfiguration(appConfig);
        this.executorService = Executors.newFixedThreadPool(agentConfig.getMaxConcurrentTasks());
        ObjectMapper objectMapper = new ObjectMapper();
        this.dagTaskAgent = new DagTaskAgent(agentConfig, schedulerClient, executorService, objectMapper);
    }

    /**
     * Starts the embedded agent: registers in database and starts queue processor.
     */
    public void start() {
        LOGGER.info("Starting embedded agent '{}' ...", LOCAL_AGENT_ID);

        // Register or update "local-agent" in t_agents and t_agent_status tables
        jdbi.useHandle(handle -> {
            // Insert into t_agents (identity table) if not exists
            handle.createUpdate("""
                INSERT INTO t_agents (agent_id, name, status, registered_at, created_at, updated_at)
                VALUES (:agentId, :name, :status, NOW(), NOW(), NOW())
                ON CONFLICT (agent_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    updated_at = NOW()
                """)
                .bind("agentId", LOCAL_AGENT_ID)
                .bind("name", "Embedded Local Agent")
                .bind("status", "ACTIVE")
                .execute();

            // Insert or update t_agent_status (runtime status table)
            handle.createUpdate("""
                INSERT INTO t_agent_status (agent_id, agent_url, max_concurrent_tasks,
                    max_pending_tasks, supported_execution_keys, running, pending_tasks,
                    running_tasks, finished_tasks, last_heartbeat_at)
                VALUES (:agentId, :url, :maxConcurrent, :maxPending, :supportedKeys,
                    true, 0, 0, 0, NOW())
                ON CONFLICT (agent_id) DO UPDATE SET
                    agent_url = EXCLUDED.agent_url,
                    max_concurrent_tasks = EXCLUDED.max_concurrent_tasks,
                    max_pending_tasks = EXCLUDED.max_pending_tasks,
                    supported_execution_keys = EXCLUDED.supported_execution_keys,
                    running = true,
                    last_heartbeat_at = NOW()
                """)
                .bind("agentId", LOCAL_AGENT_ID)
                .bind("url", LOCAL_AGENT_URL)
                .bind("maxConcurrent", agentConfig.getMaxConcurrentTasks())
                .bind("maxPending", agentConfig.getMaxPendingTasks())
                .bind("supportedKeys", "")
                .execute();

            LOGGER.info("Registered embedded agent in database");
        });

        // Start the task execution engine (queue processor)
        dagTaskAgent.start();
        LOGGER.info("Embedded agent '{}' started successfully", LOCAL_AGENT_ID);
    }

    /**
     * Gracefully shuts down the embedded agent.
     */
    public void stop() {
        LOGGER.info("Stopping embedded agent...");
        dagTaskAgent.stop();
        LOGGER.info("Embedded agent stopped");
    }

    public TaskExecutionEngine getTaskExecutionEngine() {
        return dagTaskAgent.getExecutionEngine();
    }

    public DagTaskAgent getDagTaskAgent() {
        return dagTaskAgent;
    }

    public AgentConfiguration getAgentConfiguration() {
        return agentConfig;
    }

    private AgentConfiguration buildAgentConfiguration(Config config) {
        int maxConcurrent = config.hasPath("agent.maxConcurrentTasks")
            ? config.getInt("agent.maxConcurrentTasks") : 4;
        int maxPending = config.hasPath("agent.maxPendingTasks")
            ? config.getInt("agent.maxPendingTasks") : 100;
        String taskLogDir = config.hasPath("agent.taskLogDir")
            ? config.getString("agent.taskLogDir")
            : System.getProperty("java.io.tmpdir") + "/dagtask/logs";

        return AgentConfiguration.builder()
            .agentId(LOCAL_AGENT_ID)
            .agentName("Embedded Local Agent")
            .agentUrl(LOCAL_AGENT_URL)
            .dagServerUrl(LOCAL_AGENT_URL)
            .maxConcurrentTasks(maxConcurrent)
            .maxPendingTasks(maxPending)
            .taskLogDir(taskLogDir)
            .supportedExecutionKeys(List.of())
            .autoRegister(false)
            .build();
    }
}
