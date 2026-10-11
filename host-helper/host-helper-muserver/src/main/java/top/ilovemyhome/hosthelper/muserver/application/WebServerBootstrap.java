package top.ilovemyhome.hosthelper.muserver.application;

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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;
import top.ilovemyhome.zora.muserver.security.AppSecurityContext;
import top.ilovemyhome.hosthelper.muserver.api.FooUserHandler;
import top.ilovemyhome.hosthelper.muserver.api.QueryHandler;
import top.ilovemyhome.hosthelper.muserver.api.SecurityHandler;
import top.ilovemyhome.hosthelper.muserver.sso.SsoAuthenticationHandler;
import top.ilovemyhome.hosthelper.muserver.sso.SsoClientConfiguration;

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
        SsoClientConfiguration ssoConfiguration = SsoClientConfiguration.from(config);
        SsoAuthenticationHandler ssoAuthenticationHandler = new SsoAuthenticationHandler(appContext);
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
            .addHandler(Method.GET, ssoConfiguration.loginPath(), ssoAuthenticationHandler::begin)
            .addHandler(Method.GET, ssoConfiguration.callbackPath(), ssoAuthenticationHandler::callback)
            .addHandler(context(contextPath)
                .addHandler(Method.GET, "/", (req, res, map) -> res.redirect("/" + contextPath + "/index.html"))
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
        return new JacksonJsonProvider((JsonMapper) JacksonUtil.MAPPER);
    }

    private static RestHandlerBuilder createRestHandler(AppContext appContext) {

        return RestHandlerBuilder
            .restHandler(new FooUserHandler(appContext), new QueryHandler(appContext))
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
            .addExceptionMapper(JacksonException.class, e -> Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", e.getMessage()))
                .build())
            .withOpenApiDocument(
                OpenAPIObjectBuilder.openAPIObject()
                    .withInfo(
                        infoObject()
                            .withTitle("HostHelper API")
                            .withDescription("This is a API for the host helper application!")
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
