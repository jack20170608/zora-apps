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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.server.application.AppContext;
import top.ilovemyhome.dagtask.si.TaskDagService;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Path("/task")
@Description(value = "Task Management API", details = "REST endpoints for managing individual task instances (execution records)")
public class TaskRecordHandler {

    private final TaskRecordDao taskRecordDao;
    private final TaskDagService taskDagService;

    @Inject
    public TaskRecordHandler(AppContext appContext) {
        this.taskRecordDao = appContext.getBean("taskRecordDao", TaskRecordDao.class);
        this.taskDagService = appContext.getBean("taskDagService", TaskDagService.class);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Get task by ID", details = "Retrieve a specific task record by its ID")
    public Response getById(@PathParam("id") Long id) {
        Optional<TaskRecord> task = taskRecordDao.findOne(id);
        if (task.isPresent()) {
            return Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(task.get())
                .build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task not found with id: " + id))
                .build();
        }
    }

    @GET
    @Path("/order/{orderKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "List tasks by order key", details = "Get all tasks belonging to a specific task order")
    public Response getByOrderKey(@PathParam("orderKey") String orderKey) {
        List<TaskRecord> tasks = taskRecordDao.findByOrderKey(orderKey);
        return Response.ok()
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .entity(tasks)
            .build();
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "List all tasks", details = "Get all task records with optional status filtering")
    public Response listAll(@QueryParam("status") String status) {
        List<TaskRecord> tasks;
        if (Objects.nonNull(status) && !status.isEmpty()) {
            try {
                TaskStatus taskStatus = TaskStatus.valueOf(status);
                tasks = taskRecordDao.findByStatus(taskStatus);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .entity(new ErrorResponse("Invalid task status: " + status))
                    .build();
            }
        } else {
            tasks = taskRecordDao.findAll();
        }
        return Response.ok()
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .entity(tasks)
            .build();
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Create a new task", details = "Create a new task record for execution")
    public Response create(TaskRecord task) {
        if (Objects.isNull(task)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task cannot be null"))
                .build();
        }
        taskRecordDao.create(task);
        return Response.status(Response.Status.CREATED)
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .entity(task)
            .build();
    }

    @PUT
    @Path("/update/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Update an existing task", details = "Update task information")
    public Response update(@PathParam("id") Long id, TaskRecord task) {
        if (Objects.isNull(task)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task cannot be null"))
                .build();
        }
        Optional<TaskRecord> existing = taskRecordDao.findOne(id);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task not found with id: " + id))
                .build();
        }
        task.setId(id);
        int updated = taskRecordDao.update(id, task);
        if (updated > 0) {
            return Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(task)
                .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Failed to update task"))
                .build();
        }
    }

    @DELETE
    @Path("/delete/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Delete a task", details = "Delete a task record by ID")
    public Response delete(@PathParam("id") Long id) {
        Optional<TaskRecord> existing = taskRecordDao.findOne(id);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task not found with id: " + id))
                .build();
        }
        int deleted = taskRecordDao.delete(id);
        if (deleted > 0) {
            return Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new SuccessResponse("Task deleted successfully"))
                .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Failed to delete task"))
                .build();
        }
    }

    @POST
    @Path("/{id}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Start task execution", details = "Trigger the start of a task execution")
    public Response startTask(@PathParam("id") Long id) {
        return changeTaskStatus(id, TaskStatus.RUNNING, "Task started");
    }

    @POST
    @Path("/{id}/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Complete task execution", details = "Mark task as completed successfully")
    public Response completeTask(@PathParam("id") Long id) {
        Optional<TaskRecord> existing = taskRecordDao.findOne(id);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task not found with id: " + id))
                .build();
        }
        TaskRecord task = existing.get();
        TaskRecord updatedTask = TaskRecord.builder()
            .withId(id)
            .withOrderKey(task.getOrderKey())
            .withName(task.getName())
            .withDescription(task.getDescription())
            .withExecutionKey(task.getExecutionKey())
            .withSuccessorIds(task.getSuccessorIds())
            .withInput(task.getInput())
            .withOutput(task.getOutput())
            .withAsync(task.isAsync())
            .withDummy(task.isDummy())
            .withCreateDt(task.getCreateDt())
            .withLastUpdateDt(LocalDateTime.now())
            .withStatus(TaskStatus.SUCCESS)
            .withStartDt(task.getStartDt())
            .withEndDt(LocalDateTime.now())
            .withSuccess(true)
            .withFailReason(task.getFailReason())
            .withTimeout(task.getTimeout())
            .withTimeoutUnit(task.getTimeoutUnit())
            .build();
        int updated = taskRecordDao.update(id, updatedTask);
        if (updated > 0) {
            return Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new SuccessResponse("Task completed successfully"))
                .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Failed to update task status"))
                .build();
        }
    }

    @POST
    @Path("/{id}/fail")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Mark task as failed", details = "Mark task as failed with a failure reason")
    public Response failTask(@PathParam("id") Long id, FailRequest request) {
        Optional<TaskRecord> existing = taskRecordDao.findOne(id);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task not found with id: " + id))
                .build();
        }
        TaskRecord task = existing.get();
        TaskRecord updatedTask = TaskRecord.builder()
            .withId(id)
            .withOrderKey(task.getOrderKey())
            .withName(task.getName())
            .withDescription(task.getDescription())
            .withExecutionKey(task.getExecutionKey())
            .withSuccessorIds(task.getSuccessorIds())
            .withInput(task.getInput())
            .withOutput(task.getOutput())
            .withAsync(task.isAsync())
            .withDummy(task.isDummy())
            .withCreateDt(task.getCreateDt())
            .withLastUpdateDt(LocalDateTime.now())
            .withStatus(TaskStatus.ERROR)
            .withStartDt(task.getStartDt())
            .withEndDt(LocalDateTime.now())
            .withSuccess(false)
            .withFailReason(request.reason())
            .withTimeout(task.getTimeout())
            .withTimeoutUnit(task.getTimeoutUnit())
            .build();
        int updated = taskRecordDao.update(id, updatedTask);
        if (updated > 0) {
            return Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new SuccessResponse("Task marked as failed"))
                .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Failed to update task status"))
                .build();
        }
    }

    @POST
    @Path("/trigger/{orderKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @Description(value = "Trigger a DAG workflow", details = "Trigger execution of all tasks in a task order (DAG workflow)")
    public Response triggerWorkflow(@PathParam("orderKey") String orderKey) {
        try {
            // Start the DAG execution
            taskDagService.start(orderKey);
            return Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new SuccessResponse("DAG workflow triggered successfully for order: " + orderKey))
                .build();
        } catch (Exception e) {
            LOGGER.error("Failed to trigger DAG workflow for order: {}", orderKey, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Failed to trigger DAG workflow: " + e.getMessage()))
                .build();
        }
    }

    private Response changeTaskStatus(Long id, TaskStatus status, String message) {
        Optional<TaskRecord> existing = taskRecordDao.findOne(id);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Task not found with id: " + id))
                .build();
        }
        TaskRecord task = existing.get();
        TaskRecord updatedTask = TaskRecord.builder()
            .withId(id)
            .withOrderKey(task.getOrderKey())
            .withName(task.getName())
            .withDescription(task.getDescription())
            .withExecutionKey(task.getExecutionKey())
            .withSuccessorIds(task.getSuccessorIds())
            .withInput(task.getInput())
            .withOutput(task.getOutput())
            .withAsync(task.isAsync())
            .withDummy(task.isDummy())
            .withCreateDt(task.getCreateDt())
            .withLastUpdateDt(LocalDateTime.now())
            .withStatus(status)
            .withStartDt(task.getStartDt())
            .withEndDt(task.getEndDt())
            .withSuccess(task.isSuccess())
            .withFailReason(task.getFailReason())
            .withTimeout(task.getTimeout())
            .withTimeoutUnit(task.getTimeoutUnit())
            .build();
        int updated = taskRecordDao.update(id, updatedTask);
        if (updated > 0) {
            return Response.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new SuccessResponse(message))
                .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("Failed to update task status"))
                .build();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRecordHandler.class);

    public record ErrorResponse(String error) {}

    public record SuccessResponse(String message) {}

    public record FailRequest(String reason) {}
}
