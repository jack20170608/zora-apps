package top.ilovemyhome.dagtask.agent.muserver.application;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.typesafe.config.Config;
import io.muserver.MuServer;
import io.muserver.MuServerBuilder;
import io.muserver.rest.CollectionParameterStrategy;
import io.muserver.rest.RestHandlerBuilder;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.api.TaskAgentResource;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.muserver.ContextHandlerBuilder.context;
import static io.muserver.openapi.InfoObjectBuilder.infoObject;
import static io.muserver.openapi.OpenAPIObjectBuilder.openAPIObject;

/**
 * Bootstrap class that starts the MuServer HTTP server for the DAG Task Agent.
 */
public class WebServerBootstrap {

    /**
     * Starts the MuServer with the given application context.
     * @param appContext the application context
     * @return the started MuServer instance
     */
    public static MuServer start(AppContext appContext) {
        return startMuServer(appContext);
    }

    private static MuServer startMuServer(AppContext appContext) {
        Config config = appContext.getConfig();
        Config muserverConfig = config.getConfig("muserver");
        String contextPath = muserverConfig.getString("contextPath");
        int port = muserverConfig.getInt("port");
        String host = muserverConfig.getString("host");
        int idleTimeout = muserverConfig.getInt("idleTimeout");

        LOGGER.info("Starting MuServer for DAG Task Agent on {}:{}", host, port);
        long start = System.currentTimeMillis();
        MuServerBuilder muServerBuilder = MuServerBuilder.httpServer()
                .withHttpPort(port)
                .withInterface(host)
                .addResponseCompleteListener(info -> {
                    LOGGER.info("Response completed: success={}, remoteAddr={}, clientAddress={}, req={}, status={}, duration={}",
                            info.completedSuccessfully(), info.request().remoteAddress(), info.request().clientIP(),
                            info.request(), info.response().status(), info.duration());
                })
                .withIdleTimeout(idleTimeout, TimeUnit.MILLISECONDS)
                .withMaxRequestSize(100_000_000) // 100MB max request size
                .addHandler(context(contextPath)
                        .addHandler(createRestHandler(appContext))
                );

        MuServer muServer = muServerBuilder.start();
        LOGGER.info("MuServer started in {} ms", System.currentTimeMillis() - start);
        LOGGER.info("DAG Task Agent server running at {}", muServer.uri().resolve(contextPath));
        if (contextPath.length() > 1) {
            LOGGER.info("OpenAPI at {}{}/openapi.json", muServer.uri(), contextPath);
        } else {
            LOGGER.info("OpenAPI at {}openapi.json", muServer.uri());
        }
        return muServer;
    }

    private static JacksonJsonProvider createJacksonJsonProvider() {
        return new JacksonJsonProvider(JacksonUtil.MAPPER);
    }

    private static RestHandlerBuilder createRestHandler(AppContext appContext) {
        TaskAgentResource resource = appContext.getDagTaskAgent().getResource();

        return RestHandlerBuilder
                .restHandler(resource)
                .addCustomReader(createJacksonJsonProvider())
                .addCustomWriter(createJacksonJsonProvider())
                .withCollectionParameterStrategy(CollectionParameterStrategy.NO_TRANSFORM)
                .withOpenApiHtmlUrl("/api.html")
                .withOpenApiJsonUrl("/openapi.json")
                .addExceptionMapper(ClientErrorException.class, e -> Response
                        .status(Response.Status.BAD_REQUEST.getStatusCode())
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("message", e.getMessage()))
                        .build())
                .addExceptionMapper(InternalServerErrorException.class, e -> Response
                        .status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("message", e.getMessage()))
                        .build())
                .withOpenApiDocument(
                        openAPIObject()
                                .withInfo(
                                        infoObject()
                                                .withTitle("DAG Task Agent API")
                                                .withDescription("REST API for DAG Task Agent execution and status")
                                                .withVersion("1.0")
                                                .build())
                );
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServerBootstrap.class);
}
