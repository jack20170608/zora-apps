package top.ilovemyhome.dagtask.server.application;

import com.fasterxml.jackson.databind.JsonMappingException;
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
import top.ilovemyhome.dagtask.core.DagSchedulerServer;
import top.ilovemyhome.dagtask.core.interfaces.AgentRegistryApi;
import top.ilovemyhome.dagtask.core.interfaces.TaskOrderApi;
import top.ilovemyhome.dagtask.core.interfaces.TaskTemplateApi;
import top.ilovemyhome.dagtask.server.interfaces.api.FooUserHandler;
import top.ilovemyhome.dagtask.server.web.LoginHandler;
import top.ilovemyhome.dagtask.server.web.security.SecurityHandler;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;
import top.ilovemyhome.zora.muserver.security.AppSecurityContext;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.muserver.ContextHandlerBuilder.context;
import static io.muserver.openapi.ExternalDocumentationObjectBuilder.externalDocumentationObject;
import static io.muserver.openapi.InfoObjectBuilder.infoObject;

public class WebServerBootstrap {

    public static MuServer start(AppContext appContext) {
        return startMuServer(appContext);
    }

    private static MuServer startMuServer(AppContext appContext) {
        Config config = appContext.getConfig();
        String contextPath = config.getString("server.contextPath");
        int port = config.getInt("server.port");
        logger.info("Start mu server on port: {}.", port);
        long start = System.currentTimeMillis();
        AppSecurityContext appSecurityContext = appContext.getBean("appSecurityContext", AppSecurityContext.class);
        MuServerBuilder muServerBuilder = MuServerBuilder.httpServer()
                .withHttpPort(port)
                .addResponseCompleteListener(info -> {
                    MuRequest req = info.request();
                    MuResponse resp = info.response();
                    logger.info("Response completed: success={}, remoteAddr={}, clientAddress={}, req={}, status={}, duration={}."
                            , info.completedSuccessfully(), req.remoteAddress(), req.clientIP(), req, resp.status(), info.duration());
                })
                .withIdleTimeout(30, TimeUnit.MINUTES)
                .withMaxRequestSize(300_000_000) //300MB
                .addHandler(Method.GET, "/", (req, res, map) -> res.redirect("/" + contextPath + "/index.html"))
                .addHandler(Method.GET, "/index.html", (req, res, map) -> res.redirect("/" + contextPath + "/index.html"))
                .addHandler(Method.POST, "/login", new LoginHandler(appContext))
                .addHandler(context(contextPath)
                        .addHandler(ResourceHandlerBuilder.classpathHandler("/swagger-ui"))
                        .addHandler(Method.GET, "/", (req, res, map) -> res.redirect("/" + contextPath + "/swagger-ui/index.html?url=/" + contextPath + "/openapi.json"))
                        .addHandler(Method.GET, "/index.html", (req, res, map) -> res.redirect("/" + contextPath + "/swagger-ui/index.html?url=/" + contextPath + "/openapi.json"))
                        .addHandler(new SecurityHandler(appContext))
                        .addHandler(ResourceHandlerBuilder.classpathHandler("/static"))
                        .addHandler(createRestHandler(appContext)
                                .addRequestFilter(appSecurityContext.getFacetFilter()))
                );

        MuServer muServer = muServerBuilder.start();
        logger.info("Mu server started in {} ms.", System.currentTimeMillis() - start);
        logger.info("Started app at {}.", muServer.uri().resolve("/" + contextPath));
        logger.info("api.html at {}.", muServer.uri().resolve("/" + contextPath + "/api.html"));
        logger.info("openapi.json at {}.", muServer.uri().resolve("/" + contextPath + "/openapi.json"));
        return muServer;
    }

    private static JacksonJsonProvider createJacksonJsonProvider() {
        return new JacksonJsonProvider(JacksonUtil.MAPPER);
    }

    private static RestHandlerBuilder createRestHandler(AppContext appContext) {

        DagSchedulerServer schedulerServer = appContext.getBean("dagSchedulerServer", DagSchedulerServer.class);
        TaskOrderApi taskOrderApi = new TaskOrderApi(schedulerServer.getTaskOrderDao());
        TaskTemplateApi taskTemplateApi = new TaskTemplateApi(schedulerServer.getTaskTemplateService());
        AgentRegistryApi agentRegistryApi = new AgentRegistryApi(schedulerServer.getAgentRegistryService());

        return RestHandlerBuilder
                .restHandler(new FooUserHandler(appContext),
                    agentRegistryApi,
                    taskOrderApi,
                    taskTemplateApi)
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
                .addExceptionMapper(JsonMappingException.class, e -> Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("message", e.getMessage()))
                        .build())
                .withOpenApiDocument(
                        OpenAPIObjectBuilder.openAPIObject()
                                .withInfo(
                                        infoObject()
                                                .withTitle("Dag Task Server API")
                                                .withDescription("DAG-based task scheduling system REST API")
                                                .withVersion("1.0")
                                                .build())
                                .withExternalDocs(
                                        externalDocumentationObject()
                                                .withDescription("Documentation docs")
                                                .withUrl(URI.create("https//muserver.io/jaxrs"))
                                                .build()
                                )
                );
    }

    private static final Logger logger = LoggerFactory.getLogger(WebServerBootstrap.class);
}
