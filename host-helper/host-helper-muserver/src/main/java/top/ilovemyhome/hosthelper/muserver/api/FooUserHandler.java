package top.ilovemyhome.hosthelper.muserver.api;


import io.muserver.rest.Description;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.hosthelper.muserver.application.AppContext;
import top.ilovemyhome.hosthelper.si.domain.FooUser;
import top.ilovemyhome.hosthelper.muserver.service.QueryService;
import top.ilovemyhome.hosthelper.muserver.service.impl.QueryServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Path("/fooUser")
@Description(value = "Some POC API to prove the web function", details = "POC endpoint only.")
public class FooUserHandler {


    public FooUserHandler(AppContext appContext) {
        QueryService queryService = appContext.getBean("queryService", QueryServiceImpl.class);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "It's a hello world endpoint", details = "hello world")
    public Response getById(@PathParam("id") Long id) {
        Response response;
        Optional<FooUser> user = Optional.ofNullable(USER_DB.getOrDefault(id, null));
        if (user.isPresent()) {
            response = Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(user.get())
                .build();
        } else {
            response = Response.status(Response.Status.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(null)
                .build();
        }
        return response;
    }

    @POST
    @Path("/create")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Description(value = "Create a new user", details = "hello world")
    public Response getById(FooUser user) {
        if (Objects.isNull(user)){
            return Response.status(Response.Status.BAD_REQUEST)
               .header("Content-Type", MediaType.TEXT_PLAIN)
               .entity("User is null")
               .build();
        }else {
            Long id = ID_GENERATOR.getAndIncrement();
            USER_DB.put(id, FooUser.builder(user).id(id).build());
            return Response.status(Response.Status.CREATED)
               .header("Content-Type", MediaType.TEXT_PLAIN)
                .build();
        }
    }

    @GET
    @Path("/listAll")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "It's a hello world endpoint", details = "hello world")
    public Response listAll() {
        Response response;
        List<FooUser> user = USER_DB.values().stream().toList();
            response = Response.status(Response.Status.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(user)
                .build();
        return response;
    }


    private static final Map<Long, FooUser> USER_DB = new HashMap<>(10);
    private static final AtomicLong ID_GENERATOR = new AtomicLong(3);

    static {
        LocalDateTime now = LocalDateTime.now();
        YearMonth thisMonth = YearMonth.now();
        USER_DB.put(1L, new FooUser(1L, "John", 25, LocalDate.of(1997, 1, 1), now, thisMonth));
        USER_DB.put(2L, new FooUser(2L, "Jane", 30, LocalDate.of(1992, 1, 1), now, thisMonth));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FooUserHandler.class);

}

