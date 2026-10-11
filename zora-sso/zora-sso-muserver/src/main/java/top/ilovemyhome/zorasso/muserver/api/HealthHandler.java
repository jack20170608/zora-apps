package top.ilovemyhome.zorasso.muserver.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("/health")
public final class HealthHandler {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        return health();
    }

    private Response health() {
        return restResponse(200, "{\"status\":\"UP\",\"localAuthentication\":true}");
    }
}
