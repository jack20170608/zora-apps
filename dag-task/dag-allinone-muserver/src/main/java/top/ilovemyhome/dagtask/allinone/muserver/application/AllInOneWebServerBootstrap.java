package top.ilovemyhome.dagtask.allinone.muserver.application;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.typesafe.config.Config;
import io.muserver.*;
import io.muserver.handlers.ResourceHandlerBuilder;
import io.muserver.openapi.OpenAPIObjectBuilder;
import io.muserver.rest.CollectionParameterStrategy;
import io.muserver.rest.RestHandlerBuilder;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.admin.interfaces.api.AgentAdminApi;
import top.ilovemyhome.dagtask.admin.interfaces.api.AgentWhitelistAdminApi;
import top.ilovemyhome.dagtask.admin.interfaces.api.ExecutionApi;
import top.ilovemyhome.dagtask.admin.interfaces.api.StatsApi;
import top.ilovemyhome.dagtask.admin.interfaces.api.WorkflowApi;
import top.ilovemyhome.dagtask.admin.server.web.LoginHandler;
import top.ilovemyhome.dagtask.agent.api.TaskAgentResource;
import top.ilovemyhome.dagtask.allinone.muserver.security.AllInOneSecurityHandler;
import top.ilovemyhome.dagtask.core.DagSchedulerServer;
import top.ilovemyhome.dagtask.core.interfaces.AgentRegistryApi;
import top.ilovemyhome.dagtask.core.interfaces.DagManageApi;
import top.ilovemyhome.dagtask.core.interfaces.TaskOrderApi;
import top.ilovemyhome.dagtask.core.interfaces.TaskTemplateApi;
import top.ilovemyhome.dagtask.scheduler.config.JwtConfig;
import top.ilovemyhome.dagtask.scheduler.token.TokenManagementApi;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;
import top.ilovemyhome.zora.muserver.security.AppSecurityContext;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.muserver.ContextHandlerBuilder.context;
import static io.muserver.openapi.ExternalDocumentationObjectBuilder.externalDocumentationObject;
import static io.muserver.openapi.InfoObjectBuilder.infoObject;

/**
 * Bootstrap class that starts a single unified MuServer for the all-in-one deployment.
 * Registers security handler, static assets, login endpoint, and combined REST APIs
 * from admin, scheduler, and agent modules.
 */
public class AllInOneWebServerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllInOneWebServerBootstrap.class);

    /**
     * Starts the unified MuServer with all handlers registered.
     *
     * @param appContext the all-in-one application context
     * @return the started MuServer instance
     */
    public static MuServer start(AllInOneAppContext appContext) {
        return startMuServer(appContext);
    }

    private static MuServer startMuServer(AllInOneAppContext appContext) {
        Config config = appContext.getConfig();
        int port = config.getInt("server.port");
        String contextPath = config.getString("server.contextPath");

        LOGGER.info("Starting unified MuServer on port: {}, contextPath: {}", port, contextPath);
        long start = System.currentTimeMillis();

        // Simple token validator: in dev mode any non-empty token is accepted
        Function<String, String> tokenValidator = token -> {
            if (token != null && !token.isBlank()) {
                return "dev-user";
            }
            return null;
        };

        MuServerBuilder muServerBuilder = MuServerBuilder.httpServer()
            .withHttpPort(port)
            .addResponseCompleteListener(info -> {
                MuRequest req = info.request();
                MuResponse resp = info.response();
                LOGGER.info("Response completed: success={}, remoteAddr={}, clientAddress={}, req={}, status={}, duration={}.",
                    info.completedSuccessfully(), req.remoteAddress(), req.clientIP(), req, resp.status(), info.duration());
            })
            .withIdleTimeout(30, TimeUnit.MINUTES)
            .withMaxRequestSize(300_000_000) // 300MB
            .addHandler(Method.GET, "/", (req, res, map) -> res.redirect("/" + contextPath + "/index.html"))
            .addHandler(Method.GET, "/index.html", (req, res, map) -> res.redirect("/" + contextPath + "/swagger-ui/index.html?url=/" + contextPath + "/openapi.json"))
            .addHandler(context(contextPath)
                .addHandler(ResourceHandlerBuilder.classpathHandler("/swagger-ui"))
                .addHandler(Method.GET, "/", (req, res, map) -> res.redirect("/" + contextPath + "/swagger-ui/index.html?url=/" + contextPath + "/openapi.json"))
                .addHandler(Method.GET, "/index.html", (req, res, map) -> res.redirect("/" + contextPath + "/swagger-ui/index.html?url=/" + contextPath + "/openapi.json"))
                .addHandler(new AllInOneSecurityHandler("token", tokenValidator,
                    List.of("/login", "/api/v1/agent/health", "/api/v1/agent/ping", "/swagger", "/static")))
                .addHandler(Method.POST, "/login", new LoginHandler(appContext.getAdminAppContext()))
                .addHandler(ResourceHandlerBuilder.classpathHandler("/static"))
                .addHandler(createRestHandler(appContext))
            );

        MuServer muServer = muServerBuilder.start();
        LOGGER.info("Unified MuServer started in {} ms.", System.currentTimeMillis() - start);
        LOGGER.info("Started app at {}.", muServer.uri().resolve("/" + contextPath));
        LOGGER.info("api.html at {}.", muServer.uri().resolve("/" + contextPath + "/api.html"));
        LOGGER.info("openapi.json at {}.", muServer.uri().resolve("/" + contextPath + "/openapi.json"));
        return muServer;
    }

    private static JacksonJsonProvider createJacksonJsonProvider() {
        return new JacksonJsonProvider(JacksonUtil.MAPPER);
    }

    private static RestHandlerBuilder createRestHandler(AllInOneAppContext appContext) {
        top.ilovemyhome.dagtask.admin.server.application.AppContext adminAppContext = appContext.getAdminAppContext();
        DagSchedulerServer schedulerServer = adminAppContext.getBean("dagSchedulerServer", DagSchedulerServer.class);
        AppSecurityContext appSecurityContext = adminAppContext.getBean("appSecurityContext", AppSecurityContext.class);

        // Admin REST APIs
        TaskOrderApi taskOrderApi = new TaskOrderApi(schedulerServer.getTaskOrderDao());
        TaskTemplateApi taskTemplateApi = new TaskTemplateApi(schedulerServer.getTaskTemplateService());
        AgentWhitelistAdminApi agentWhitelistAdminApi = new AgentWhitelistAdminApi(schedulerServer.getAgentWhitelistDao());

        JwtConfig jwtConfig = adminAppContext.getBean("jwtConfig", JwtConfig.class);
        TokenService tokenService = new TokenService(schedulerServer.getAgentTokenDao(), jwtConfig);
        TokenManagementApi tokenManagementApi = new TokenManagementApi(tokenService);

        DagManageApi dagManageApi = new DagManageApi(schedulerServer.getDagManageService());
        WorkflowApi workflowApi = new WorkflowApi(schedulerServer.getTaskTemplateService(), schedulerServer.getDagManageService());
        ExecutionApi executionApi = new ExecutionApi(schedulerServer.getTaskOrderDao(), schedulerServer.getTaskRecordDao());
        AgentAdminApi agentAdminApi = new AgentAdminApi(schedulerServer.getAgentDao(), schedulerServer.getAgentStatusDao());
        StatsApi statsApi = new StatsApi();

        // Scheduler REST APIs
        AgentRegistryApi agentRegistryApi = new AgentRegistryApi(schedulerServer.getAgentRegistryService());

        // Agent REST APIs
        TaskAgentResource taskAgentResource = appContext.getEmbeddedAgentBootstrap().getDagTaskAgent().getResource();

        return RestHandlerBuilder
            .restHandler(
                taskOrderApi,
                taskTemplateApi,
                agentWhitelistAdminApi,
                tokenManagementApi,
                dagManageApi,
                workflowApi,
                executionApi,
                agentAdminApi,
                statsApi,
                agentRegistryApi,
                taskAgentResource
            )
            .addRequestFilter(appSecurityContext.getFacetFilter())
            .addCustomReader(createJacksonJsonProvider())
            .addCustomWriter(createJacksonJsonProvider())
            .withCollectionParameterStrategy(CollectionParameterStrategy.NO_TRANSFORM)
            .withOpenApiHtmlUrl("/api.html")
            .withOpenApiJsonUrl("/openapi.json")
            .addExceptionMapper(ClientErrorException.class, e -> Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", e.getMessage()))
                .build())
            .addExceptionMapper(InternalServerErrorException.class, e -> Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", e.getMessage()))
                .build())
            .withOpenApiDocument(
                OpenAPIObjectBuilder.openAPIObject()
                    .withInfo(
                        infoObject()
                            .withTitle("Dag Task AllInOne API")
                            .withDescription("Unified API for DAG Task Scheduler, Admin, and Agent")
                            .withVersion("1.0")
                            .build())
                    .withExternalDocs(
                        externalDocumentationObject()
                            .withDescription("Documentation docs")
                            .withUrl(URI.create("https//muserver.io/jaxrs"))
                            .build())
            );
    }
}
