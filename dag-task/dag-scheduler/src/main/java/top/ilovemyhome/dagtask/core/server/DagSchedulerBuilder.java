package top.ilovemyhome.dagtask.core.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import top.ilovemyhome.dagtask.core.DagSchedulerServer;
import top.ilovemyhome.dagtask.core.agent.DefaultAgentRegistryService;
import top.ilovemyhome.dagtask.core.dao.AgentRegistryDaoJdbiImpl;
import top.ilovemyhome.dagtask.core.dao.TaskOrderDaoJdbiImpl;
import top.ilovemyhome.dagtask.core.dao.TaskRecordDaoJdbiImpl;
import top.ilovemyhome.dagtask.core.dao.TaskTemplateDaoJdbiImpl;
import top.ilovemyhome.dagtask.core.task.TaskDagServiceImpl;
import top.ilovemyhome.dagtask.core.task.TaskTemplateServiceImpl;
import top.ilovemyhome.dagtask.core.task.TaskOrderServiceImpl;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.zora.common.date.LocalDateUtils;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import javax.sql.DataSource;
import java.sql.Types;
import java.time.YearMonth;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DagSchedulerBuilder {

    public static final int DEFAULT_SCAN_INTERVAL_SECONDS = 10;
    public static final int DEFAULT_HEARTBEAT_TIMEOUT_SECONDS = 5;
    public static final int DEFAULT_HEARTBEAT_INTERVAL_SECONDS = 30;
    public static final int DEFAULT_MAX_HEARTBEAT_FAILED_TIMES = 3;
    public static final int DEFAULT_MAX_SYSTEM_CONCURRENT_TASKS = 200;
    public static final String DEFAULT_DATABASE_TYPE = "postgresql";

    private DataSource dataSource;
    private Jdbi jdbi;
    private ObjectMapper objectMapper;
    //扫描Ready任务的时间间隔
    private int scanIntervalSeconds = DEFAULT_SCAN_INTERVAL_SECONDS;
    //系统最大调度的任务数量
    private int maxSystemConcurrentTasks = DEFAULT_MAX_SYSTEM_CONCURRENT_TASKS;
    //DataBase Type
    private String databaseType = DEFAULT_DATABASE_TYPE;
    //服务器发送heartBeat请求，Agent响应的timeout时间，默认是5s
    private int heartbeatTimeoutSeconds = DEFAULT_HEARTBEAT_TIMEOUT_SECONDS;
    //服务器端发送heartbeat请求的时间间隔,默认是30s
    private int heartbeatIntervalSeconds = DEFAULT_HEARTBEAT_INTERVAL_SECONDS;
    //服务器端，连续几次heartbeat失败，则下线agent,3次heartBeat失败则offline agent
    private int maxHeartbeatFailedTimes = DEFAULT_MAX_HEARTBEAT_FAILED_TIMES;


    public static DagSchedulerBuilder builder() {
        return new DagSchedulerBuilder();
    }

    public DagSchedulerBuilder dataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public DagSchedulerBuilder jdbi(Jdbi jdbi) {
        this.jdbi = jdbi;
        return this;
    }

    public DagSchedulerBuilder objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public DagSchedulerBuilder scanIntervalSeconds(int scanIntervalSeconds) {
        this.scanIntervalSeconds = scanIntervalSeconds;
        return this;
    }

    public DagSchedulerBuilder maxSystemConcurrentTasks(int maxSystemConcurrentTasks) {
        this.maxSystemConcurrentTasks = maxSystemConcurrentTasks;
        return this;
    }

    public DagSchedulerBuilder databaseType(String databaseType) {
        this.databaseType = databaseType;
        return this;
    }

    public DagSchedulerBuilder heartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        return this;
    }

    public DagSchedulerBuilder heartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        return this;
    }

    public DagSchedulerBuilder maxHeartbeatFailedTimes(int maxHeartbeatFailedTimes) {
        this.maxHeartbeatFailedTimes = maxHeartbeatFailedTimes;
        return this;
    }


    private void bindingJdbiArguments(Jdbi jdbi) {
        //customize some argument mapper
        jdbi.registerArgument(new AbstractArgumentFactory<Map<String, String>>(Types.VARCHAR) {
            @Override
            protected Argument build(Map<String, String> value, ConfigRegistry config) {
                return (position, statement, ctx) -> statement.setString(position, JacksonUtil.toJson(value));
            }
        });
        jdbi.registerArgument(new AbstractArgumentFactory<Set<Long>>(Types.VARCHAR) {
            @Override
            protected Argument build(Set<Long> value, ConfigRegistry config) {
                return (position, statement, ctx) -> statement.setString(position, JacksonUtil.toJson(value));
            }
        });
        jdbi.registerArgument(new AbstractArgumentFactory<YearMonth>(Types.VARCHAR) {
            @Override
            protected Argument build(YearMonth value, ConfigRegistry config) {
                return (position, statement, ctx) -> statement.setString(position, LocalDateUtils.formatYearMonth(value));
            }
        });
        jdbi.registerArgument(new AbstractArgumentFactory<TaskInput>(Types.VARCHAR) {
            @Override
            protected Argument build(TaskInput taskInput, ConfigRegistry config) {
                return (position, statement, ctx) -> statement.setString(position, JacksonUtil.toJson(taskInput));
            }
        });
    }

    public DagSchedulerServer build() {
        Jdbi jdbiToUse;
        if (this.jdbi != null) {
            // Use provided Jdbi instance
            jdbiToUse = this.jdbi;
        } else {
            Objects.requireNonNull(dataSource, "Either dataSource or jdbi must be set");
            // Create new Jdbi from DataSource
            jdbiToUse = Jdbi.create(dataSource);
        }
        bindingJdbiArguments(jdbiToUse);
        var config = new DagServerConfig(scanIntervalSeconds, maxSystemConcurrentTasks, databaseType, heartbeatTimeoutSeconds, heartbeatIntervalSeconds, maxHeartbeatFailedTimes);

        // Create ObjectMapper if not provided
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        // Create DAOs if not overridden
        var agentRegistryDao = new AgentRegistryDaoJdbiImpl(jdbiToUse);
        var taskTemplateDao = new TaskTemplateDaoJdbiImpl(jdbiToUse);
        var taskOrderDao = new TaskOrderDaoJdbiImpl(jdbiToUse);
        var taskRecordDao = new TaskRecordDaoJdbiImpl(jdbiToUse);

        var taskOrderService = new TaskOrderServiceImpl(jdbiToUse, taskRecordDao, taskOrderDao);
        var agentRegistryService = new DefaultAgentRegistryService(taskRecordDao, agentRegistryDao);
        var taskTemplateService = new TaskTemplateServiceImpl(
            taskTemplateDao, taskOrderDao, taskRecordDao, objectMapper);
        var taskDagService = new TaskDagServiceImpl(config, jdbiToUse, taskOrderDao, taskRecordDao, agentRegistryDao, taskTemplateDao);
        return new DagSchedulerServer(
            config,
            jdbiToUse,
            objectMapper,
            agentRegistryDao,
            taskOrderDao,
            taskRecordDao,
            taskTemplateDao,
            agentRegistryService,
            taskTemplateService,
            taskOrderService,
            taskDagService
        );
    }

}
