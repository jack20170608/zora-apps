package top.ilovemyhome.dagtask.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.agent.AgentRegistryService;
import top.ilovemyhome.dagtask.core.dispatcher.TaskDispatcher;
import top.ilovemyhome.dagtask.core.server.DagServerConfig;
import top.ilovemyhome.dagtask.si.persistence.*;
import top.ilovemyhome.dagtask.si.service.DagManageService;
import top.ilovemyhome.dagtask.si.service.DagScheduleService;
import top.ilovemyhome.dagtask.si.service.TaskOrderService;
import top.ilovemyhome.dagtask.si.service.TaskTemplateService;

import java.util.*;
import java.util.concurrent.*;

/**
 * Top-level entry point for the DAG scheduling server (scheduler center).
 * <p>
 * This class is the main entry point for running the DAG scheduler outside of
 * a Spring DI container. It manually assembles all components from the configuration
 * and manages their lifecycle (start/stop).
 * </p>
 * <p>
 * For Spring-based applications, you can still use component scanning and
 * dependency injection - this class is just for standalone/non-Spring usage.
 * </p>
 * <p>
 * Usage example:
 * <pre>{@code
 * DagServerConfig config = new DagServerConfig()
 *     .setHttpPort(8080)
 *     .setDataSourceUrl("jdbc:postgresql://localhost/dagtask")
 *     .setDataSourceUsername("postgres")
 *     .setDataSourcePassword("password");
 *
 * DataSource dataSource = createDataSource(config);
 * DagSchedulerServer server = DagSchedulerServer.builder()
 *     .config(config)
 *     .dataSource(dataSource)
 *     .build();
 *
 * server.start();
 *
 * // Add shutdown hook for graceful shutdown
 * Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
 * }</pre>
 */
public class DagSchedulerServer {

    private static final Logger logger = LoggerFactory.getLogger(DagSchedulerServer.class);

    private final DagServerConfig config;
    private final Jdbi jdbi;
    private final ObjectMapper objectMapper;

    // DAO layer
    private final AgentRegistryDao agentRegistryDao;
    private final TaskOrderDao taskOrderDao;
    private final TaskRecordDao taskRecordDao;
    private final TaskTemplateDao taskTemplateDao;
    private final TaskDispatchDao taskDispatchDao;
    private final ExecutorService threadPool;

    // Service layer
    private final AgentRegistryService agentRegistryService;
    private final TaskTemplateService taskTemplateService;
    private final DagManageService dagManageService;
    private final DagScheduleService dagScheduleService;
    private final TaskDispatcher taskDispatcher;
    // Components that need lifecycle management
    private final List<Startable> startableComponents = new ArrayList<>();

    private boolean started = false;

    public DagSchedulerServer(
        DagServerConfig config,
        Jdbi jdbi,
        ObjectMapper objectMapper,
        AgentRegistryDao agentRegistryDao,
        TaskOrderDao taskOrderDao,
        TaskRecordDao taskRecordDao,
        TaskTemplateDao taskTemplateDao,
        TaskDispatchDao taskDispatchDao,
        AgentRegistryService agentRegistryService,
        TaskTemplateService taskTemplateService,
        DagManageService dagManageService,
        DagScheduleService dagScheduleService,
        TaskDispatcher taskDispatcher) {
        this.config = config;
        this.jdbi = Objects.requireNonNull(jdbi, "jdbi must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.agentRegistryDao = Objects.requireNonNull(agentRegistryDao, "agentRegistryDao must not be null");
        this.taskOrderDao = Objects.requireNonNull(taskOrderDao, "taskOrderDao must not be null");
        this.taskRecordDao = Objects.requireNonNull(taskRecordDao, "taskRecordDao must not be null");
        this.taskTemplateDao = Objects.requireNonNull(taskTemplateDao, "taskTemplateDao must not be null");
        this.taskDispatchDao = Objects.requireNonNull(taskDispatchDao, "taskDispatchDao must not be null");
        this.agentRegistryService = Objects.requireNonNull(agentRegistryService, "agentRegistryService must not be null");
        this.taskTemplateService = Objects.requireNonNull(taskTemplateService, "taskTemplateService must not be null");
        this.dagManageService = Objects.requireNonNull(dagManageService, "dagManageService must not be null");
        this.dagScheduleService = Objects.requireNonNull(dagScheduleService, "dagScheduleService must not be null");
        this.taskDispatcher = Objects.requireNonNull(taskDispatcher, "taskDispatcher must not be null");
        int totalProcessorSize = Runtime.getRuntime().availableProcessors();
        int nThreads = Math.min(totalProcessorSize, 16);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("TaskDagService-%d").build();

        this.threadPool = new ThreadPoolExecutor(nThreads, 16, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024)
            , namedThreadFactory, new ThreadPoolExecutor.AbortPolicy())
        ;

        this.startableComponents.add(new Startable() {
            @Override
            public void start() {
                // Thread pool is already created and ready
            }

            @Override
            public void stop() {
                threadPool.shutdown();
            }
        });
        //
    }




    /**
     * Starts the DAG scheduler server and all its components.
     * Does nothing if already started.
     */
    public synchronized void start() {
        if (started) {
            logger.warn("DagSchedulerServer already started, ignoring start request");
            return;
        }

        logger.info("Starting DagSchedulerServer...");

        // Start components in dependency order
        // Currently no background threads needed in core, but prepared for future
        for (Startable component : startableComponents) {
            component.start();
        }

        started = true;
        logger.info("DagSchedulerServer started successfully. " +
                "Max system concurrent tasks: {}, scan interval: {}s",
            config.maxSystemConcurrentTasks(), config.scanIntervalSeconds());
    }

    /**
     * Stops the DAG scheduler server and all its components gracefully.
     * Does nothing if already stopped.
     */
    public synchronized void stop() {
        if (!started) {
            logger.warn("DagSchedulerServer not started, ignoring stop request");
            return;
        }

        logger.info("Stopping DagSchedulerServer...");

        // Stop in reverse order
        List<Startable> reversed = new ArrayList<>(startableComponents);
        Collections.reverse(reversed);
        for (Startable component : reversed) {
            component.stop();
        }

        started = false;
        logger.info("DagSchedulerServer stopped successfully");
    }

    /**
     * Checks if the server is currently running.
     *
     * @return true if started, false otherwise
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Gets the Jdbi instance used by this server.
     *
     * @return the Jdbi instance
     */
    public Jdbi getJdbi() {
        return jdbi;
    }

    /**
     * Gets the ObjectMapper used for JSON processing.
     *
     * @return the ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Gets the AgentRegistryService for this server.
     *
     * @return the agent registry service
     */
    public AgentRegistryService getAgentRegistryService() {
        return agentRegistryService;
    }

    /**
     * Gets the TaskTemplateService for this server.
     *
     * @return the task template service
     */
    public TaskTemplateService getTaskTemplateService() {
        return taskTemplateService;
    }

    /**
     * Gets the TaskOrderDao for this server.
     *
     * @return the task order DAO
     */
    public TaskOrderDao getTaskOrderDao() {
        return taskOrderDao;
    }

    /**
     * Gets the TaskRecordDao for this server.
     *
     * @return the task record DAO
     */
    public TaskRecordDao getTaskRecordDao() {
        return taskRecordDao;
    }

    public DagServerConfig getConfig() {
        return config;
    }

    /**
     * Gets the DagScheduleService for this server.
     *
     * @return the DAG schedule service
     */
    public DagScheduleService getDagScheduleService() {
        return dagScheduleService;
    }

    public AgentRegistryDao getAgentRegistryDao() {
        return agentRegistryDao;
    }

    public TaskTemplateDao getTaskTemplateDao() {
        return taskTemplateDao;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }


    /**
     * Interface for components that need lifecycle management.
     */
    public interface Startable {
        /**
         * Start the component.
         */
        void start();

        /**
         * Stop the component.
         */
        void stop();
    }
}
