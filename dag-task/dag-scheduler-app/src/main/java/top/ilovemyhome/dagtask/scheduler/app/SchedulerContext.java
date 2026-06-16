package top.ilovemyhome.dagtask.scheduler.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.jdbi.v3.core.Jdbi;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.AgentDaoJdbiImpl;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.AgentStatusDaoJdbiImpl;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.AgentTokenDaoJdbiImpl;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.AgentWhitelistDaoJdbiImpl;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.JacksonDagDefinitionParser;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.JdbiUnitOfWork;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.SequenceIdGenerator;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.SystemClock;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.TaskDispatchDaoJdbiImpl;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.TaskOrderDaoJdbiImpl;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.TaskRecordDaoJdbiImpl;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.TaskTemplateDaoJdbiImpl;
import top.ilovemyhome.dagtask.scheduler.application.AgentHeartbeatService;
import top.ilovemyhome.dagtask.scheduler.application.InstantiateDagTemplateService;
import top.ilovemyhome.dagtask.scheduler.application.RegisterAgentService;
import top.ilovemyhome.dagtask.scheduler.application.ReportTaskResultService;
import top.ilovemyhome.dagtask.scheduler.application.ScheduleDagRunService;
import top.ilovemyhome.dagtask.scheduler.application.TaskOrderApplicationService;
import top.ilovemyhome.dagtask.scheduler.application.TaskTemplateApplicationService;
import top.ilovemyhome.dagtask.scheduler.config.JwtConfig;
import top.ilovemyhome.dagtask.scheduler.domain.dispatcher.LoadBalanceStrategy;
import top.ilovemyhome.dagtask.scheduler.port.in.AgentHeartbeatUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.InstantiateDagTemplateUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.ManageTaskOrderUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.ManageTaskTemplateUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.QueryTaskOrderUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.QueryTaskTemplateUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.RegisterAgentUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.ReportTaskResultUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.ScheduleDagRunUseCase;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;
import top.ilovemyhome.dagtask.scheduler.token.TokenManagementApi;

/**
 * Manual DI context for scheduler hexagonal modules.
 */
public class SchedulerContext {

    private final AgentDaoJdbiImpl agentRepository;
    private final AgentStatusDaoJdbiImpl agentStatusRepository;
    private final AgentTokenDaoJdbiImpl agentTokenRepository;
    private final AgentWhitelistDaoJdbiImpl agentWhitelistRepository;
    private final TaskDispatchDaoJdbiImpl taskDispatchRepository;
    private final TaskOrderDaoJdbiImpl taskOrderRepository;
    private final TaskRecordDaoJdbiImpl taskRecordRepository;
    private final TaskTemplateDaoJdbiImpl taskTemplateRepository;

    private final TokenService tokenService;
    private final QueryTaskTemplateUseCase queryTaskTemplateUseCase;
    private final ManageTaskTemplateUseCase manageTaskTemplateUseCase;
    private final InstantiateDagTemplateUseCase instantiateDagTemplateUseCase;
    private final QueryTaskOrderUseCase queryTaskOrderUseCase;
    private final ManageTaskOrderUseCase manageTaskOrderUseCase;
    private final RegisterAgentUseCase registerAgentUseCase;
    private final AgentHeartbeatUseCase agentHeartbeatUseCase;
    private final ReportTaskResultUseCase reportTaskResultUseCase;
    private final ScheduleDagRunUseCase scheduleDagRunUseCase;
    private final TokenManagementApi tokenManagementApi;

    public SchedulerContext(Jdbi jdbi, ObjectMapper objectMapper, JwtConfig jwtConfig,
                            LoadBalanceStrategy loadBalanceStrategy) {
        Objects.requireNonNull(jdbi, "jdbi must not be null");
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(jwtConfig, "jwtConfig must not be null");
        Objects.requireNonNull(loadBalanceStrategy, "loadBalanceStrategy must not be null");

        this.agentRepository = new AgentDaoJdbiImpl(jdbi);
        this.agentStatusRepository = new AgentStatusDaoJdbiImpl(jdbi);
        this.agentTokenRepository = new AgentTokenDaoJdbiImpl(jdbi);
        this.agentWhitelistRepository = new AgentWhitelistDaoJdbiImpl(jdbi);
        this.taskDispatchRepository = new TaskDispatchDaoJdbiImpl(jdbi);
        this.taskOrderRepository = new TaskOrderDaoJdbiImpl(jdbi);
        this.taskRecordRepository = new TaskRecordDaoJdbiImpl(jdbi);
        this.taskTemplateRepository = new TaskTemplateDaoJdbiImpl(jdbi);

        var clock = new SystemClock();
        var unitOfWork = new JdbiUnitOfWork(jdbi);
        var idGenerator = new SequenceIdGenerator(jdbi);
        var parser = new JacksonDagDefinitionParser(objectMapper);
        var dispatcher = new HttpAgentDispatcher(objectMapper);
        this.tokenService = new TokenService(agentTokenRepository, jwtConfig);
        var tokenIssuer = new LegacyTokenIssuer(tokenService);

        var taskTemplateService = new TaskTemplateApplicationService(taskTemplateRepository);
        this.queryTaskTemplateUseCase = taskTemplateService;
        this.manageTaskTemplateUseCase = taskTemplateService;
        this.instantiateDagTemplateUseCase = new InstantiateDagTemplateService(
            taskOrderRepository, taskTemplateRepository, taskRecordRepository, unitOfWork, idGenerator, parser);

        var taskOrderService = new TaskOrderApplicationService(taskOrderRepository, taskRecordRepository, unitOfWork);
        this.queryTaskOrderUseCase = taskOrderService;
        this.manageTaskOrderUseCase = taskOrderService;

        this.registerAgentUseCase = new RegisterAgentService(
            agentRepository, agentStatusRepository, agentWhitelistRepository, tokenIssuer, unitOfWork);
        this.agentHeartbeatUseCase = new AgentHeartbeatService(agentStatusRepository);
        this.reportTaskResultUseCase = new ReportTaskResultService(taskRecordRepository, clock);
        this.scheduleDagRunUseCase = new ScheduleDagRunService(
            taskRecordRepository, dispatcher, agentStatusRepository, taskDispatchRepository, clock, loadBalanceStrategy);
        this.tokenManagementApi = new TokenManagementApi(tokenService);
    }

    public AgentDaoJdbiImpl agentRepository() { return agentRepository; }
    public AgentStatusDaoJdbiImpl agentStatusRepository() { return agentStatusRepository; }
    public AgentTokenDaoJdbiImpl agentTokenRepository() { return agentTokenRepository; }
    public AgentWhitelistDaoJdbiImpl agentWhitelistRepository() { return agentWhitelistRepository; }
    public TaskDispatchDaoJdbiImpl taskDispatchRepository() { return taskDispatchRepository; }
    public TaskOrderDaoJdbiImpl taskOrderRepository() { return taskOrderRepository; }
    public TaskRecordDaoJdbiImpl taskRecordRepository() { return taskRecordRepository; }
    public TaskTemplateDaoJdbiImpl taskTemplateRepository() { return taskTemplateRepository; }
    public TokenService tokenService() { return tokenService; }
    public QueryTaskTemplateUseCase queryTaskTemplateUseCase() { return queryTaskTemplateUseCase; }
    public ManageTaskTemplateUseCase manageTaskTemplateUseCase() { return manageTaskTemplateUseCase; }
    public InstantiateDagTemplateUseCase instantiateDagTemplateUseCase() { return instantiateDagTemplateUseCase; }
    public QueryTaskOrderUseCase queryTaskOrderUseCase() { return queryTaskOrderUseCase; }
    public ManageTaskOrderUseCase manageTaskOrderUseCase() { return manageTaskOrderUseCase; }
    public RegisterAgentUseCase registerAgentUseCase() { return registerAgentUseCase; }
    public AgentHeartbeatUseCase agentHeartbeatUseCase() { return agentHeartbeatUseCase; }
    public ReportTaskResultUseCase reportTaskResultUseCase() { return reportTaskResultUseCase; }
    public ScheduleDagRunUseCase scheduleDagRunUseCase() { return scheduleDagRunUseCase; }
    public TokenManagementApi tokenManagementApi() { return tokenManagementApi; }
}
