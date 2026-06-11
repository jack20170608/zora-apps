package top.ilovemyhome.dagtask.scheduler.adapter.web.muserver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import top.ilovemyhome.dagtask.scheduler.application.OrderKeyAlreadyExistsException;
import top.ilovemyhome.dagtask.scheduler.port.in.ManageTaskOrderUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.QueryTaskOrderUseCase;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskOrderRepository;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;

import java.util.List;
import java.util.Objects;

/**
 * REST API endpoints for managing DAG task orders (workflow definitions).
 * <p>
 * This API provides CRUD operations for managing complete DAG workflow definitions (task orders).
 * A task order represents a complete DAG workflow consisting of multiple tasks with dependencies.
 * </p>
 * <p>
 * Read operations use {@link TaskOrderRepository} directly for backward compatibility with
 * legacy response format. Write operations use {@link ManageTaskOrderUseCase}.
 * </p>
 *
 * @see TaskOrder TaskOrder entity that represents a DAG workflow definition
 * @see TaskOrderRepository Repository for read operations
 * @see ManageTaskOrderUseCase Use case for write operations
 */
@Path("/api/v1/order")
@Produces(MediaType.APPLICATION_JSON)
public class TaskOrderApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskOrderApi.class);

    private final TaskOrderRepository taskOrderRepository;
    private final QueryTaskOrderUseCase queryTaskOrderUseCase;
    private final ManageTaskOrderUseCase manageTaskOrderUseCase;

    /**
     * Creates a new TaskOrderApi with injected dependencies.
     *
     * @param taskOrderRepository the repository for task order read operations
     * @param queryTaskOrderUseCase the use case for task order queries
     * @param manageTaskOrderUseCase the use case for task order modifications
     */
    @Inject
    public TaskOrderApi(TaskOrderRepository taskOrderRepository,
                        QueryTaskOrderUseCase queryTaskOrderUseCase,
                        ManageTaskOrderUseCase manageTaskOrderUseCase) {
        this.taskOrderRepository = Objects.requireNonNull(taskOrderRepository, "taskOrderRepository must not be null");
        this.queryTaskOrderUseCase = Objects.requireNonNull(queryTaskOrderUseCase, "queryTaskOrderUseCase must not be null");
        this.manageTaskOrderUseCase = Objects.requireNonNull(manageTaskOrderUseCase, "manageTaskOrderUseCase must not be null");
    }

    /**
     * List all task orders (DAG workflow definitions) in the system.
     *
     * @return HTTP 200 OK with a list of all task orders
     */
    @GET
    @Operation(summary = "List all task orders", description = "Returns all DAG workflow definitions stored in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = List.class)))
    })
    public Response listAll() {
        // Note: Using TaskOrderRepository directly because QueryTaskOrderUseCase only has isOrdered()
        // This maintains compatibility with legacy API behavior
        List<TaskOrder> allOrders = ((top.ilovemyhome.dagtask.si.persistence.TaskOrderDao) taskOrderRepository).findAll();
        LOGGER.debug("Retrieved {} task orders", allOrders.size());
        return Response.ok().entity(ResEntityHelper.ok("List retrieved successfully", allOrders)).build();
    }

    /**
     * Get a specific task order by its unique business key.
     *
     * @param key the unique business key of the task order
     * @return HTTP 200 OK with the task order if found, HTTP 400 Bad Request if not found
     */
    @GET
    @Path("/{key}")
    @Operation(summary = "Get task order by key", description = "Returns a specific DAG workflow definition by its unique business key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task order found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TaskOrder.class))),
        @ApiResponse(responseCode = "400", description = "Task order not found", content = @Content)
    })
    public Response getByKey(
        @Parameter(description = "Unique business key of the task order", required = true)
        @PathParam("key") String key) {
        var taskOrderOpt = taskOrderRepository.findByKey(key);
        if (taskOrderOpt.isEmpty()) {
            LOGGER.warn("Task order with key [{}] not found", key);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Task order not found: " + key))
                .build();
        }
        LOGGER.debug("Retrieved task order: {}", key);
        return Response.ok().entity(ResEntityHelper.ok("Task order retrieved successfully", taskOrderOpt.get())).build();
    }

    /**
     * Create a new task order (DAG workflow definition).
     * <p>
     * The task order key must be unique. If a task order with the same key already exists,
     * the creation will fail.
     * </p>
     *
     * @param taskOrder the complete task order definition to create
     * @return HTTP 200 OK if creation succeeded, HTTP 400 Bad Request if key already exists
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create new task order", description = "Creates a new DAG workflow definition with a unique key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task order created successfully", content = @Content),
        @ApiResponse(responseCode = "400", description = "Task order with this key already exists or invalid data", content = @Content)
    })
    public Response create(
        @Parameter(description = "Task order definition to create", required = true)
        TaskOrder taskOrder) {
        try {
            Long id = manageTaskOrderUseCase.createOrder(taskOrder);
            taskOrder.setId(id);
            LOGGER.info("Created new task order: key=[{}], name=[{}], id=[{}]",
                taskOrder.getKey(), taskOrder.getName(), id);
            return Response.ok().entity(ResEntityHelper.ok("Task order created successfully", taskOrder)).build();
        } catch (OrderKeyAlreadyExistsException e) {
            LOGGER.warn("Cannot create task order: key [{}] already exists", taskOrder.getKey());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Task order with key already exists: " + taskOrder.getKey()))
                .build();
        }
    }

    /**
     * Update an existing task order by its key.
     * <p>
     * Updates all mutable fields of the existing task order. The key cannot be changed.
     * </p>
     *
     * @param key the unique business key of the task order to update
     * @param taskOrder the updated task order definition
     * @return HTTP 200 OK if update succeeded, HTTP 400 Bad Request if task order not found
     */
    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update existing task order", description = "Updates an existing DAG workflow definition by its key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task order updated successfully", content = @Content),
        @ApiResponse(responseCode = "400", description = "Task order not found or invalid data", content = @Content)
    })
    public Response update(
        @Parameter(description = "Unique business key of the task order to update", required = true)
        @PathParam("key") String key,
        @Parameter(description = "Updated task order definition", required = true)
        TaskOrder taskOrder) {
        int updated = manageTaskOrderUseCase.updateOrderByKey(key, taskOrder);
        LOGGER.info("Updated task order: key=[{}], {} rows affected", key, updated);
        return Response.ok().entity(ResEntityHelper.ok("Task order updated successfully", taskOrder)).build();
    }

    /**
     * Delete a task order by its unique business key.
     *
     * @param key the unique business key of the task order to delete
     * @return HTTP 200 OK if deletion succeeded, HTTP 400 Bad Request if task order not found
     */
    @DELETE
    @Path("/{key}")
    @Operation(summary = "Delete task order", description = "Deletes a DAG workflow definition by its key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task order deleted successfully", content = @Content),
        @ApiResponse(responseCode = "400", description = "Task order not found", content = @Content)
    })
    public Response delete(
        @Parameter(description = "Unique business key of the task order to delete", required = true)
        @PathParam("key") String key) {
        int deleted = manageTaskOrderUseCase.deleteOrderByKey(key);
        if (deleted == 0) {
            LOGGER.warn("Cannot delete task order: key [{}] not found", key);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Task order not found: " + key))
                .build();
        }
        LOGGER.info("Deleted task order: key=[{}], {} rows affected", key, deleted);
        return Response.ok().entity(ResEntityHelper.ok("Task order deleted successfully", null)).build();
    }
}
