package top.ilovemyhome.hosthelper.muserver.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zora.jdbi.page.Direction;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.impl.PageRequest;
import top.ilovemyhome.hosthelper.muserver.application.AppContext;
import top.ilovemyhome.hosthelper.si.domain.FileSearchCriteria;
import top.ilovemyhome.hosthelper.si.domain.FileSearchResult;
import top.ilovemyhome.hosthelper.muserver.service.QueryService;

@Path("/query/api/v1")
public class QueryHandler {

    @GET
    @Path("/allHosts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllHosts(@Context SecurityContext securityContext) {
        if (!securityContext.isUserInRole("read")) {
            throw new ClientErrorException("This requires a User role", 403);
        }
        return Response.ok(queryService.getAllHosts())
            .build();
    }

    @POST
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response search(@QueryParam("page") @DefaultValue("0") int page
        , @QueryParam("pageSize") @DefaultValue("20") int pageSize
        , @QueryParam("sortBy") @DefaultValue("name") String sortBy
        , @QueryParam("direction") @DefaultValue("ASC") Direction direction
        , FileSearchCriteria searchCriteria) {
        PageRequest pageRequest = new PageRequest(page, pageSize, direction, sortBy);
        Page<FileSearchResult> result = queryService.search(searchCriteria, pageRequest);
        return Response.ok().entity(result)
           .build();
    }

    public QueryHandler(AppContext appContext) {
        this.queryService = appContext.getBean("queryService", QueryService.class);
    }

    private final QueryService queryService;

    private static final Logger logger = LoggerFactory.getLogger(QueryHandler.class);
}
