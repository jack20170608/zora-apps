package top.ilovemyhome.dagtask.admin.interfaces.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;
import top.ilovemyhome.dagtask.scheduler.port.in.InstantiateDagTemplateUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.ManageTaskTemplateUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.QueryTaskTemplateUseCase;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;
import top.ilovemyhome.zora.jdbi.page.impl.PageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API endpoints for managing workflows (task templates).
 */
@Path("/api/v1/workflows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowApi.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final QueryTaskTemplateUseCase queryTemplate;
    private final ManageTaskTemplateUseCase manageTemplate;
    private final InstantiateDagTemplateUseCase instantiateUseCase;

    @Inject
    public WorkflowApi(QueryTaskTemplateUseCase queryTemplate,
                       ManageTaskTemplateUseCase manageTemplate,
                       InstantiateDagTemplateUseCase instantiateUseCase) {
        this.queryTemplate = queryTemplate;
        this.manageTemplate = manageTemplate;
        this.instantiateUseCase = instantiateUseCase;
    }

    @GET
    public Response list(
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        TaskTemplateSearchCriteria criteria = TaskTemplateSearchCriteria.builder().build();
        Pageable pageRequest = new PageRequest(page, pageSize);
        Page<TaskTemplate> result = queryTemplate.find(criteria, pageRequest);
        List<Map<String, Object>> workflows = result.getContent().stream()
            .map(this::toWorkflowMap)
            .collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("data", workflows);
        response.put("page", result.getNumber());
        response.put("pageSize", result.getSize());
        response.put("total", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        LOGGER.info("Listed workflows: page={}, pageSize={}, total={}", page, pageSize, result.getTotalElements());
        return Response.ok().entity(ResEntityHelper.ok("Workflows retrieved successfully", response)).build();
    }

    @GET
    @Path("/{key}")
    public Response getByKey(@PathParam("key") String key) {
        TaskTemplateSearchCriteria criteria = TaskTemplateSearchCriteria.builder()
            .withTemplateKey(key)
            .build();
        List<TaskTemplate> templates = queryTemplate.findAll(criteria);
        if (templates.isEmpty()) {
            LOGGER.warn("Workflow not found: key=[{}]", key);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Workflow not found: " + key))
                .build();
        }
        Map<String, Object> workflow = toWorkflowMap(templates.get(0));
        LOGGER.info("Retrieved workflow: key=[{}]", key);
        return Response.ok().entity(ResEntityHelper.ok("Workflow retrieved successfully", workflow)).build();
    }

    @POST
    public Response create(TaskTemplate template) {
        boolean success = manageTemplate.createTemplate(template, template.isActive());
        if (!success) {
            LOGGER.warn("Failed to create workflow: key=[{}], version=[{}]", template.getTemplateKey(), template.getVersion());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Failed to create workflow: version already exists or invalid data"))
                .build();
        }
        LOGGER.info("Created workflow: key=[{}], version=[{}]", template.getTemplateKey(), template.getVersion());
        return Response.ok().entity(ResEntityHelper.ok("Workflow created successfully", toWorkflowMap(template))).build();
    }

    @PUT
    @Path("/{key}")
    public Response update(@PathParam("key") String key, TaskTemplate template) {
        if (!key.equals(template.getTemplateKey())) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Path key does not match template key in body"))
                .build();
        }
        boolean success = manageTemplate.updateTemplate(template);
        if (!success) {
            LOGGER.warn("Failed to update workflow: key=[{}], version=[{}]", template.getTemplateKey(), template.getVersion());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Failed to update workflow: template version not found"))
                .build();
        }
        LOGGER.info("Updated workflow: key=[{}], version=[{}]", key, template.getVersion());
        return Response.ok().entity(ResEntityHelper.ok("Workflow updated successfully", toWorkflowMap(template))).build();
    }

    @DELETE
    @Path("/{key}")
    public Response delete(@PathParam("key") String key, @QueryParam("version") String version) {
        String versionToDelete = version != null ? version : "latest";
        boolean success = manageTemplate.deleteVersion(key, versionToDelete);
        if (!success) {
            LOGGER.warn("Failed to delete workflow: key=[{}], version=[{}]", key, versionToDelete);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Workflow not found: " + key + "/" + versionToDelete))
                .build();
        }
        LOGGER.info("Deleted workflow: key=[{}], version=[{}]", key, versionToDelete);
        return Response.ok().entity(ResEntityHelper.ok("Workflow deleted successfully", null)).build();
    }

    @GET
    @Path("/{key}/versions")
    public Response listVersions(@PathParam("key") String key) {
        TaskTemplateSearchCriteria criteria = TaskTemplateSearchCriteria.builder()
            .withTemplateKey(key)
            .build();
        List<TaskTemplate> templates = queryTemplate.findAll(criteria);
        List<Map<String, Object>> versions = templates.stream()
            .map(this::toWorkflowMap)
            .collect(Collectors.toList());
        LOGGER.info("Listed workflow versions: key=[{}], count={}", key, versions.size());
        return Response.ok().entity(ResEntityHelper.ok("Workflow versions retrieved successfully", versions)).build();
    }

    @POST
    @Path("/{key}/versions/{version}/activate")
    public Response activateVersion(@PathParam("key") String key, @PathParam("version") String version) {
        LOGGER.info("Activating workflow version: key=[{}], version=[{}]", key, version);
        return Response.ok().entity(ResEntityHelper.ok("Version activation is not yet implemented", null)).build();
    }

    @POST
    @Path("/{key}/execute")
    public Response execute(@PathParam("key") String key,
                            @QueryParam("orderKey") String orderKey,
                            @QueryParam("orderName") String orderName,
                            Map<String, String> parameters) {
        if (orderKey == null || orderKey.isBlank()) {
            orderKey = key + "-" + System.currentTimeMillis();
        }
        if (orderName == null || orderName.isBlank()) {
            orderName = key;
        }
        Optional<TaskOrder> orderOpt = instantiateUseCase.instantiateFromTemplate(key, orderKey, orderName, parameters);
        if (orderOpt.isEmpty()) {
            LOGGER.warn("Failed to execute workflow: key=[{}], orderKey=[{}]", key, orderKey);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Failed to execute workflow: template not found or order key already exists"))
                .build();
        }
        LOGGER.info("Executed workflow: key=[{}], orderKey=[{}]", key, orderKey);
        return Response.ok().entity(ResEntityHelper.ok("Workflow execution triggered successfully", orderOpt.get())).build();
    }

    private Map<String, Object> toWorkflowMap(TaskTemplate template) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", template.getId());
        map.put("key", template.getTemplateKey());
        map.put("name", template.getTemplateName());
        map.put("description", template.getDescription());
        map.put("version", template.getVersion());
        map.put("active", template.isActive());
        map.put("dagDefinition", parseJson(template.getDagDefinition()));
        map.put("parameterSchema", parseJson(template.getParameterSchema()));
        map.put("tags", new ArrayList<String>());
        map.put("createdAt", template.getCreateDt());
        map.put("updatedAt", template.getLastUpdateDt());
        map.put("lastExecution", null);
        map.put("executionCount", 0);
        map.put("successRate", 0.0);
        return map;
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return OBJECT_MAPPER.createObjectNode();
        }
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            return OBJECT_MAPPER.createObjectNode();
        }
    }
}
