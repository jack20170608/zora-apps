package top.ilovemyhome.zorasso.muserver.application;

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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;
import top.ilovemyhome.zorasso.muserver.api.HealthHandler;
import top.ilovemyhome.zorasso.muserver.api.SsoHandler;
import top.ilovemyhome.zorasso.muserver.api.dto.RestResponse;
import top.ilovemyhome.zorasso.si.SsoException;

import java.net.URI;
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
            .addHandler(createRestHandler(ssoHandler));

        MuServer server = serverBuilder.start();
        LOGGER.info("MuServer started in {} ms.", System.currentTimeMillis() - start);
        LOGGER.info("Zora SSO started at {}.", server.uri());
        return server;
    }


    private static RestHandlerBuilder createRestHandler(SsoHandler ssoHandler) {

        return RestHandlerBuilder
            .restHandler(
                ssoHandler,
                new HealthHandler()
            )
            .addCustomReader(new JacksonJsonProvider((JsonMapper) JacksonUtil.MAPPER))
            .addCustomWriter(new JacksonJsonProvider((JsonMapper) JacksonUtil.MAPPER))
            .withCollectionParameterStrategy(CollectionParameterStrategy.NO_TRANSFORM)
            .withOpenApiHtmlUrl("/api.html")
            .withOpenApiJsonUrl("/openapi.json")
            .addExceptionMapper(SsoException.class, e -> {
                int status = switch (e.code()) {
                    case LOGIN_REJECTED, INVALID_CLIENT_CREDENTIALS, INVALID_SESSION, INVALID_CODE -> 401;
                    case ACCESS_DENIED -> 403;
                    case RATE_LIMITED -> 429;
                    default -> 400;
                };
                return Response.status(status)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(RestResponse.failure(e.getMessage()))
                    .build();
            })
            .addExceptionMapper(ClientErrorException.class, e -> Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .type(MediaType.APPLICATION_JSON)
                .entity(RestResponse.failure(e.getMessage()))
                .build())
            .addExceptionMapper(InternalServerErrorException.class, e -> Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .type(MediaType.APPLICATION_JSON)
                .entity(RestResponse.failure(e.getMessage()))
                .build())
            .addExceptionMapper(JacksonException.class, e -> Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .type(MediaType.APPLICATION_JSON)
                .entity(RestResponse.failure(e.getMessage()))
                .build())
            .withOpenApiDocument(
                OpenAPIObjectBuilder.openAPIObject()
                    .withInfo(
                        infoObject()
                            .withTitle("Zora SSO API")
                            .withDescription("REST endpoints for the Zora SSO service.")
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
