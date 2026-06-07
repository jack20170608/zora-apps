package top.ilovemyhome.dagtask.allinone.muserver.application;

import com.typesafe.config.Config;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.admin.server.application.AppContext;
import top.ilovemyhome.dagtask.allinone.muserver.agent.EmbeddedAgentBootstrap;
import top.ilovemyhome.dagtask.allinone.muserver.client.InProcessSchedulerClient;
import top.ilovemyhome.dagtask.allinone.muserver.database.DatabaseBootstrap;
import top.ilovemyhome.dagtask.allinone.muserver.dispatcher.InProcessTaskDispatcher;
import top.ilovemyhome.dagtask.core.DagSchedulerServer;
import top.ilovemyhome.dagtask.core.dao.TaskDispatchDaoJdbiImpl;
import top.ilovemyhome.dagtask.si.service.DagScheduleService;

/**
 * Application context for the all-in-one server that combines scheduler, admin, and agent
 * in a single JVM process.
 * <p>
 * This context initializes a shared database, the admin application context (which provides
 * security, Flyway migrations, and the DAG scheduler server), an embedded agent, and
 * in-process communication components to eliminate HTTP overhead where possible.
 * </p>
 */
public class AllInOneAppContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllInOneAppContext.class);

    private final String env;
    private final Config config;
    private final DatabaseBootstrap databaseBootstrap;
    private final AppContext adminAppContext;
    private final InProcessSchedulerClient inProcessSchedulerClient;
    private final EmbeddedAgentBootstrap embeddedAgentBootstrap;
    private final InProcessTaskDispatcher inProcessTaskDispatcher;

    /**
     * Creates and wires all components for the all-in-one mode.
     *
     * @param env    the environment name (e.g., dev, sit, prod)
     * @param config the Typesafe configuration
     */
    public AllInOneAppContext(String env, Config config) {
        this.env = env;
        this.config = config;

        // 1. Initialize shared database for custom all-in-one components
        this.databaseBootstrap = new DatabaseBootstrap(config);
        Jdbi sharedJdbi = this.databaseBootstrap.start();

        // 2. Initialize admin application context.
        // Note: Both scheduler and admin AppContext instantiate their own DagSchedulerServer.
        // To avoid duplication, only the admin AppContext is created here because it is a
        // superset (adds user management and Flyway migrations). The scheduler REST APIs
        // will reuse the DagSchedulerServer bean from this admin context.
        this.adminAppContext = new AppContext(env, config);

        // 3. Create in-process scheduler client for result reporting
        DagSchedulerServer dagSchedulerServer = this.adminAppContext.getBean("dagSchedulerServer", DagSchedulerServer.class);
        DagScheduleService dagScheduleService = dagSchedulerServer.getDagScheduleService();
        this.inProcessSchedulerClient = new InProcessSchedulerClient(dagScheduleService);

        // 4. Initialize the embedded agent
        this.embeddedAgentBootstrap = new EmbeddedAgentBootstrap(config, sharedJdbi, this.inProcessSchedulerClient);

        // 5. Create in-process task dispatcher and bind to the embedded agent's execution engine
        var taskDispatchDao = new TaskDispatchDaoJdbiImpl(sharedJdbi);
        this.inProcessTaskDispatcher = new InProcessTaskDispatcher(taskDispatchDao);
        this.inProcessTaskDispatcher.bindTaskExecutionEngine(this.embeddedAgentBootstrap.getTaskExecutionEngine());

        // 6. Log limitation: the scheduler's DagSchedulerServer was built internally by
        //    DagSchedulerBuilder with a DefaultTaskDispatcher. There is no public API to
        //    inject a custom TaskDispatcher, so the scheduler will dispatch tasks via HTTP
        //    to the local agent URL. The in-process dispatcher is bound but not on the
        //    scheduler's dispatch path.
        LOGGER.warn("InProcessTaskDispatcher is bound to the embedded agent's execution engine, "
            + "but the scheduler's DagSchedulerServer uses its own DefaultTaskDispatcher. "
            + "Tasks will dispatch via HTTP localhost loopback instead of direct method calls.");
    }

    /**
     * Starts all components: embedded agent queue processor.
     * The admin AppContext already started DagSchedulerServer in its constructor.
     */
    public void start() {
        LOGGER.info("Starting AllInOneAppContext...");
        this.embeddedAgentBootstrap.start();
        LOGGER.info("AllInOneAppContext started successfully");
    }

    /**
     * Stops all components gracefully in reverse initialization order.
     */
    public void stop() {
        LOGGER.info("Stopping AllInOneAppContext...");
        this.embeddedAgentBootstrap.stop();
        DagSchedulerServer dagServer = this.adminAppContext.getBean("dagSchedulerServer", DagSchedulerServer.class);
        dagServer.stop();
        this.databaseBootstrap.stop();
        LOGGER.info("AllInOneAppContext stopped successfully");
    }

    // Getters for WebServerBootstrap and other consumers

    public String getEnv() {
        return env;
    }

    public Config getConfig() {
        return config;
    }

    public AppContext getAdminAppContext() {
        return adminAppContext;
    }

    public DatabaseBootstrap getDatabaseBootstrap() {
        return databaseBootstrap;
    }

    public EmbeddedAgentBootstrap getEmbeddedAgentBootstrap() {
        return embeddedAgentBootstrap;
    }

    public InProcessTaskDispatcher getInProcessTaskDispatcher() {
        return inProcessTaskDispatcher;
    }

    public InProcessSchedulerClient getInProcessSchedulerClient() {
        return inProcessSchedulerClient;
    }
}
