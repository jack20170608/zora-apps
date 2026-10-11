package top.ilovemyhome.zorasso.muserver.application;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.muserver.Method;
import io.muserver.MuRequest;
import io.muserver.MuResponse;
import io.muserver.MuServer;
import io.muserver.MuServerBuilder;
import io.muserver.openapi.OpenAPIObjectBuilder;
import io.muserver.rest.CollectionParameterStrategy;
import io.muserver.rest.RestHandlerBuilder;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;
import top.ilovemyhome.zorasso.muserver.api.SsoHandler;
import top.ilovemyhome.zorasso.muserver.config.SsoConfiguration;
import top.ilovemyhome.zorasso.si.SsoService;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.muserver.openapi.ExternalDocumentationObjectBuilder.externalDocumentationObject;
import static io.muserver.openapi.InfoObjectBuilder.infoObject;

public final class SsoWebServer {

    private SsoWebServer() {
    }

    public static MuServer start(AppContext appContext) {
        return startMuServer(appContext);
    }

    private static MuServer startMuServer(AppContext appContext) {
        var configuration = appContext.configuration();
        LOGGER.info("Start MuServer on {}:{}.", configuration.host(), configuration.port());
        long start = System.currentTimeMillis();
        SsoHandler ssoHandler = new SsoHandler(appContext);
        MuServerBuilder serverBuilder = MuServerBuilder.httpServer()
            .withHttpPort(configuration.port())
            .withInterface(configuration.host())
            .addResponseCompleteListener(info -> {
                MuRequest request = info.request();
                MuResponse response = info.response();
                LOGGER.info(
                    "Response completed: success={}, remoteAddr={}, clientAddress={}, req={}, status={}, duration={}.",
                    info.completedSuccessfully(),
                    request.remoteAddress(),
                    request.clientIP(),
                    request,
                    response.status(),
                    info.duration()
                );
            })
            .withIdleTimeout(30, TimeUnit.SECONDS)
            .withMaxRequestSize(16_384)
            .addHandler(Method.GET, "/authorize", ssoHandler::authorize)
            .addHandler(Method.GET, "/login", ssoHandler::loginPage)
            .addHandler(Method.POST, "/login", ssoHandler::login)
            .addHandler(createRestHandler(appContext));

        MuServer server = serverBuilder.start();
        LOGGER.info("MuServer started in {} ms.", System.currentTimeMillis() - start);
        LOGGER.info("Zora SSO started at {}.", server.uri());
        return server;
    }


    private static RestHandlerBuilder createRestHandler(AppContext appContext) {

        return RestHandlerBuilder
            .restHandler(new SsoHandler(appContext))
            .addCustomReader(new JacksonJsonProvider(JacksonUtil.MAPPER))
            .addCustomWriter(new JacksonJsonProvider(JacksonUtil.MAPPER))
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

    private static final Logger LOGGER = LoggerFactory.getLogger(SsoWebServer.class);
}
