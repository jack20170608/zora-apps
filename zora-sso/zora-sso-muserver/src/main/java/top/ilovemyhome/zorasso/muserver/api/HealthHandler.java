package top.ilovemyhome.zorasso.muserver.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import top.ilovemyhome.zorasso.muserver.api.dto.RestResponse;

import java.util.Map;


@Path("/health")
public final class HealthHandler {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        return health();
    }

    private Response health() {
        return Response.ok(RestResponse.success(
                Map.of("status", "UP", "localAuthentication", true)
            ))
            .header("Cache-Control", "no-store")
            .header("Pragma", "no-cache")
            .build();
    }
}
