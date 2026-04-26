package top.ilovemyhome.dagtask.server.interfaces.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;
import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API endpoints for managing workflow executions (task orders).
 */
@Path("/api/v1/executions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExecutionApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionApi.class);

    private final TaskOrderDao taskOrderDao;
    private final TaskRecordDao taskRecordDao;

    @Inject
    public ExecutionApi(TaskOrderDao taskOrderDao, TaskRecordDao taskRecordDao) {
        this.taskOrderDao = taskOrderDao;
        this.taskRecordDao = taskRecordDao;
    }

    @GET
    public Response listAll() {
        List<TaskOrder> orders = taskOrderDao.findAll();
        List<Map<String, Object>> executions = orders.stream()
            .map(this::toExecutionMap)
            .collect(Collectors.toList());
        LOGGER.info("Listed executions: count={}", executions.size());
        return Response.ok().entity(ResEntityHelper.ok("Executions retrieved successfully", executions)).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        Optional<TaskOrder> orderOpt = taskOrderDao.findByKey(id);
        if (orderOpt.isEmpty()) {
            LOGGER.warn("Execution not found: id=[{}]", id);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Execution not found: " + id))
                .build();
        }
        Map<String, Object> execution = toExecutionMap(orderOpt.get());
        LOGGER.info("Retrieved execution: id=[{}]", id);
        return Response.ok().entity(ResEntityHelper.ok("Execution retrieved successfully", execution)).build();
    }

    @GET
    @Path("/{id}/logs")
    public Response getLogs(@PathParam("id") String id) {
        LOGGER.info("Retrieved logs for execution: id=[{}]", id);
        return Response.ok().entity(ResEntityHelper.ok("Logs retrieved successfully", new ArrayList<>())).build();
    }

    @POST
    @Path("/{id}/retry")
    public Response retry(@PathParam("id") String id) {
        LOGGER.info("Retry execution requested: id=[{}]", id);
        return Response.ok().entity(ResEntityHelper.ok("Retry is not yet implemented", null)).build();
    }

    @POST
    @Path("/{id}/cancel")
    public Response cancel(@PathParam("id") String id) {
        LOGGER.info("Cancel execution requested: id=[{}]", id);
        return Response.ok().entity(ResEntityHelper.ok("Cancel is not yet implemented", null)).build();
    }

    @GET
    @Path("/{id}/tasks")
    public Response getTasks(@PathParam("id") String id) {
        TaskRecordSearchCriteria criteria = TaskRecordSearchCriteria.builder()
            .withOrderKey(id)
            .build();
        List<TaskRecord> records = taskRecordDao.search(criteria);
        List<Map<String, Object>> tasks = records.stream()
            .map(this::toTaskMap)
            .collect(Collectors.toList());
        LOGGER.info("Retrieved tasks for execution: id=[{}], count={}", id, tasks.size());
        return Response.ok().entity(ResEntityHelper.ok("Tasks retrieved successfully", tasks)).build();
    }

    private Map<String, Object> toExecutionMap(TaskOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getKey());
        map.put("workflowId", order.getId());
        map.put("workflowKey", order.getKey());
        map.put("workflowName", order.getName());
        map.put("version", "1.0");
        map.put("status", "running");
        map.put("triggerType", "manual");
        map.put("startedAt", order.getCreateDt());
        map.put("endedAt", order.getLastUpdateDt());
        map.put("duration", calculateDuration(order.getCreateDt(), order.getLastUpdateDt()));
        map.put("parameters", order.getAttributes());
        map.put("tasks", new ArrayList<>());
        map.put("progress", 0);
        map.put("errorMessage", null);
        return map;
    }

    private Map<String, Object> toTaskMap(TaskRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getId());
        map.put("name", record.getName());
        map.put("status", mapTaskStatus(record.getStatus()));
        map.put("startedAt", record.getStartDt());
        map.put("endedAt", record.getEndDt());
        map.put("duration", calculateDuration(record.getStartDt(), record.getEndDt()));
        map.put("errorMessage", record.getFailReason());
        return map;
    }

    private String mapTaskStatus(TaskStatus status) {
        if (status == null) {
            return "pending";
        }
        return switch (status) {
            case INIT, READY, DISPATCHED, HOLD -> "pending";
            case RUNNING -> "running";
            case SUCCESS -> "success";
            case ERROR, TIMEOUT -> "failed";
            case UNKNOWN -> "failed";
            case SKIPPED -> "skipped";
        };
    }

    private Long calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end).toMillis();
    }
}
