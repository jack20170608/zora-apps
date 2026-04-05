package top.ilovemyhome.dagtask.server.interfaces.api;

import io.muserver.rest.Description;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.server.application.AppContext;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Path("/taskOrder")
@Description(value = "Task Order Management API", details = "REST endpoints for managing task orders (DAG workflow definitions)")
public class TaskOrderHandler {

    private final TaskOrderDao taskOrderDao;

    @Inject
    public TaskOrderHandler(AppContext appContext) {
        this.taskOrderDao = appContext.getBean("taskOrderDao", TaskOrderDao.class);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Get task order by ID", details = "Retrieve a specific task order by its primary key ID")
    public Response getById(@PathParam("id") Long id) {
        Optional<TaskOrder> order = taskOrderDao.findOne(id);
        if (order.isPresent()) {
            return Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(order.get())
                .build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task order not found with id: " + id))
                .build();
        }
    }

    @GET
    @Path("/key/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Get task order by key", details = "Retrieve a specific task order by its unique key")
    public Response getByKey(@PathParam("key") String key) {
        Optional<TaskOrder> order = taskOrderDao.findByKey(key);
        if (order.isPresent()) {
            return Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(order.get())
                .build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task order not found with key: " + key))
                .build();
        }
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "List all task orders", details = "Get a list of all task orders in the system")
    public Response listAll() {
        List<TaskOrder> orders = taskOrderDao.findAll();
        return Response.ok()
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .entity(orders)
            .build();
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Create a new task order", details = "Create a new task order (DAG workflow definition)")
    public Response create(TaskOrder order) {
        if (Objects.isNull(order)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task order cannot be null"))
                .build();
        }
        // Check if key already exists
        Optional<TaskOrder> existing = taskOrderDao.findByKey(order.getKey());
        if (existing.isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task order with key already exists: " + order.getKey()))
                .build();
        }
        taskOrderDao.create(order);
        return Response.status(Response.Status.CREATED)
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .entity(order)
            .build();
    }

    @PUT
    @Path("/update/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Update an existing task order", details = "Update an existing task order by its key")
    public Response update(@PathParam("key") String key, TaskOrder order) {
        if (Objects.isNull(order)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task order cannot be null"))
                .build();
        }
        Optional<TaskOrder> existing = taskOrderDao.findByKey(key);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task order not found with key: " + key))
                .build();
        }
        int updated = taskOrderDao.updateByKey(key, order);
        if (updated > 0) {
            return Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(order)
                .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Failed to update task order"))
                .build();
        }
    }

    @DELETE
    @Path("/delete/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Delete a task order", details = "Delete a task order by its key")
    public Response delete(@PathParam("key") String key) {
        Optional<TaskOrder> existing = taskOrderDao.findByKey(key);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task order not found with key: " + key))
                .build();
        }
        int deleted = taskOrderDao.deleteByKey(key);
        if (deleted > 0) {
            return Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new SuccessResponse("Task order deleted successfully"))
                .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Failed to delete task order"))
                .build();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskOrderHandler.class);

    public record ErrorResponse(String error) {}

    public record SuccessResponse(String message) {}
}
