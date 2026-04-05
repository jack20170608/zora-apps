package top.ilovemyhome.dagtask.core.interfaces;

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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.template.TaskTemplateService;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.ResEntityHelper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API endpoints for managing DAG task templates.
 * <p>
 * This API provides complete template management including:
 * <ul>
 *     <li>CRUD operations for template versions</li>
 *     <li>Version management and activation control</li>
 *     <li>Instantiation to create concrete {@link TaskOrder} from templates</li>
 * </ul>
 * <p>
 * A DAG task template is a reusable, versioned workflow definition that can be
 * instantiated multiple times with different parameter values.
 * </p>
 *
 * @see TaskTemplate TaskTemplate entity
 * @see TaskTemplateService TaskTemplateService business logic
 * @see TaskOrder TaskOrder instantiated from template
 */
@Path("/api/v1/template")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskTemplateApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskTemplateApi.class);

    private final TaskTemplateService taskTemplateService;

    /**
     * Creates a new TaskTemplateApi with injected dependency.
     *
     * @param taskTemplateService the service handling template business logic
     */
    @Inject
    public TaskTemplateApi(TaskTemplateService taskTemplateService) {
        this.taskTemplateService = taskTemplateService;
    }

    /**
     * Get all active templates across all template keys.
     * Returns only the active version for each template.
     *
     * @return HTTP 200 OK with list of active templates
     */
    @GET
    @Operation(summary = "List all active templates", description = "Returns all currently active template versions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved active templates",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = List.class)))
    })
    public Response listAllActive() {
        List<TaskTemplate> templates = taskTemplateService.getAllActive();
        LOGGER.debug("Retrieved {} active templates", templates.size());
        return Response.ok().entity(ResEntityHelper.ok("Active templates retrieved successfully", templates)).build();
    }

    /**
     * Get all templates including all versions (including inactive).
     *
     * @return HTTP 200 OK with list of all template versions
     */
    @GET
    @Path("/all")
    @Operation(summary = "List all templates with all versions", description = "Returns all template versions including inactive ones")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all templates",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = List.class)))
    })
    public Response listAll() {
        List<TaskTemplate> templates = taskTemplateService.getAll();
        LOGGER.debug("Retrieved {} template versions total", templates.size());
        return Response.ok().entity(ResEntityHelper.ok("All templates retrieved successfully", templates)).build();
    }

    /**
     * Get all versions of a specific template by key.
     *
     * @param templateKey the template business key
     * @return HTTP 200 OK with list of versions ordered descending
     */
    @GET
    @Path("/{templateKey}/versions")
    @Operation(summary = "List all versions of a template", description = "Returns all versions for a specific template key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved versions",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = List.class)))
    })
    public Response listVersions(
        @Parameter(description = "Template business key", required = true)
        @PathParam("templateKey") String templateKey) {
        List<TaskTemplate> versions = taskTemplateService.getVersions(templateKey);
        LOGGER.debug("Retrieved {} versions for template [{}]", versions.size(), templateKey);
        return Response.ok().entity(ResEntityHelper.ok("Versions retrieved successfully", versions)).build();
    }

    /**
     * Get the currently active version of a template.
     *
     * @param templateKey the template business key
     * @return HTTP 200 OK with the active template, HTTP 400 if not found
     */
    @GET
    @Path("/{templateKey}/active")
    @Operation(summary = "Get active version of a template", description = "Returns the currently active version of the template")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TaskTemplate.class))),
        @ApiResponse(responseCode = "400", description = "No active template found", content = @Content)
    })
    public Response getActive(
        @Parameter(description = "Template business key", required = true)
        @PathParam("templateKey") String templateKey) {
        Optional<TaskTemplate> template = taskTemplateService.getActive(templateKey);
        if (template.isEmpty()) {
            LOGGER.warn("No active template found for key [{}]", templateKey);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("No active template found for key: " + templateKey))
                .build();
        }
        return Response.ok().entity(ResEntityHelper.ok("Active template retrieved successfully", template.get())).build();
    }

    /**
     * Get a specific version of a template.
     *
     * @param templateKey the template business key
     * @param version the version string
     * @return HTTP 200 OK with the template, HTTP 400 if not found
     */
    @GET
    @Path("/{templateKey}/v/{version}")
    @Operation(summary = "Get specific template version", description = "Returns a specific version of a template")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TaskTemplate.class))),
        @ApiResponse(responseCode = "400", description = "Template version not found", content = @Content)
    })
    public Response getByVersion(
        @Parameter(description = "Template business key", required = true)
        @PathParam("templateKey") String templateKey,
        @Parameter(description = "Template version", required = true)
        @PathParam("version") String version) {
        Optional<TaskTemplate> template = taskTemplateService.getByVersion(templateKey, version);
        if (template.isEmpty()) {
            LOGGER.warn("Template version not found: key=[{}], version=[{}]", templateKey, version);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest(String.format(
                    "Template version not found: key=%s, version=%s", templateKey, version)))
                .build();
        }
        return Response.ok().entity(ResEntityHelper.ok("Template retrieved successfully", template.get())).build();
    }

    /**
     * Create a new template version.
     *
     * @param template the template version to create
     * @param setActive whether to set this as the only active version
     * @return HTTP 200 OK if created, HTTP 400 if version already exists
     */
    @POST
    @Operation(summary = "Create new template version", description = "Creates a new version of a template")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template created successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid template data or version already exists",
            content = @Content)
    })
    public Response create(
        @Parameter(description = "Template definition to create", required = true)
        TaskTemplate template,
        @Parameter(description = "Whether to set this as the only active version", required = false)
        @QueryParam("setActive") boolean setActive) {
        boolean success = taskTemplateService.createTemplate(template, setActive);
        if (!success) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest(
                    "Failed to create template: version already exists or invalid data"))
                .build();
        }
        LOGGER.info("Created new template version: key=[{}], version=[{}]",
            template.getTemplateKey(), template.getVersion());
        return Response.ok().entity(ResEntityHelper.ok("Template created successfully", template)).build();
    }

    /**
     * Update an existing template version.
     *
     * @param templateKey the template business key
     * @param version the template version
     * @param template the updated template definition
     * @return HTTP 200 OK if updated, HTTP 400 if not found
     */
    @PUT
    @Path("/{templateKey}/v/{version}")
    @Operation(summary = "Update existing template version", description = "Updates an existing template version")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template updated successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Template version not found or invalid data",
            content = @Content)
    })
    public Response update(
        @Parameter(description = "Template business key", required = true)
        @PathParam("templateKey") String templateKey,
        @Parameter(description = "Template version", required = true)
        @PathParam("version") String version,
        @Parameter(description = "Updated template definition", required = true)
        TaskTemplate template) {
        // Ensure path matches body
        if (!templateKey.equals(template.getTemplateKey()) || !version.equals(template.getVersion())) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest(
                    "Path parameters don't match template key/version in body"))
                .build();
        }

        boolean success = taskTemplateService.updateTemplate(template);
        if (!success) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest("Failed to update template: template version not found"))
                .build();
        }
        LOGGER.info("Updated template version: key=[{}], version=[{}]", templateKey, version);
        return Response.ok().entity(ResEntityHelper.ok("Template updated successfully", template)).build();
    }

    /**
     * Deactivate a specific template version.
     * Deactivated versions cannot be used for instantiation.
     *
     * @param templateKey the template business key
     * @param version the version to deactivate
     * @return HTTP 200 OK if deactivated, HTTP 400 if not found
     */
    @POST
    @Path("/{templateKey}/v/{version}/deactivate")
    @Operation(summary = "Deactivate template version", description = "Deactivates a specific template version")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template deactivated successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Template version not found", content = @Content)
    })
    public Response deactivate(
        @Parameter(description = "Template business key", required = true)
        @PathParam("templateKey") String templateKey,
        @Parameter(description = "Template version to deactivate", required = true)
        @PathParam("version") String version) {
        boolean success = taskTemplateService.deactivateVersion(templateKey, version);
        if (!success) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest(
                    String.format("Failed to deactivate: template version not found: key=%s, version=%s",
                        templateKey, version)))
                .build();
        }
        LOGGER.info("Deactivated template version: key=[{}], version=[{}]", templateKey, version);
        return Response.ok().entity(ResEntityHelper.ok("Template version deactivated successfully", null)).build();
    }

    /**
     * Delete a specific template version.
     *
     * @param templateKey the template business key
     * @param version the version to delete
     * @return HTTP 200 OK if deleted, HTTP 400 if not found
     */
    @DELETE
    @Path("/{templateKey}/v/{version}")
    @Operation(summary = "Delete template version", description = "Deletes a specific template version")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template deleted successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Template version not found", content = @Content)
    })
    public Response delete(
        @Parameter(description = "Template business key", required = true)
        @PathParam("templateKey") String templateKey,
        @Parameter(description = "Template version to delete", required = true)
        @PathParam("version") String version) {
        boolean success = taskTemplateService.deleteVersion(templateKey, version);
        if (!success) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest(
                    String.format("Template version not found: key=%s, version=%s: not found",
                        templateKey, version)))
                .build();
        }
        LOGGER.info("Deleted template version: key=[{}], version=[{}]", templateKey, version);
        return Response.ok().entity(ResEntityHelper.ok("Template version deleted successfully", null)).build();
    }

    /**
     * Instantiate a concrete {@link TaskOrder} from the active version of a template.
     * <p>
     * Creates a new task order based on the template with the provided parameters
     * injected into the definition.
     * </p>
     *
     * @param templateKey the template business key (uses active version)
     * @param orderKey the unique business key for the new task order
     * @param orderName the name for the new task order
     * @param parameters parameter values to inject into the template
     * @return HTTP 200 OK with the instantiated task order, HTTP 400 if template not found
     */
    @POST
    @Path("/{templateKey}/instantiate")
    @Operation(summary = "Instantiate task order from template",
        description = "Creates a concrete TaskOrder from the active template version with provided parameters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task order instantiated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TaskOrder.class))),
        @ApiResponse(responseCode = "400", description = "Template not found or order key already exists",
            content = @Content)
    })
    public Response instantiate(
        @Parameter(description = "Template business key", required = true)
        @PathParam("templateKey") String templateKey,
        @Parameter(description = "Unique business key for the new task order", required = true)
        @QueryParam("orderKey") String orderKey,
        @Parameter(description = "Name for the new task order", required = true)
        @QueryParam("orderName") String orderName,
        @Parameter(description = "Parameter values to inject into the template", required = false)
        Map<String, String> parameters) {
        Optional<TaskOrder> orderOpt = taskTemplateService.instantiateFromTemplate(
            templateKey, orderKey, orderName, parameters);
        if (orderOpt.isEmpty()) {
            LOGGER.warn("Failed to instantiate from template: template not found or order key exists: {}", templateKey);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest(
                    String.format("Failed to instantiate: template '%s' not found or order key '%s' already exists",
                        templateKey, orderKey)))
                .build();
        }
        TaskOrder order = orderOpt.get();
        LOGGER.info("Instantiated task order from template: template=[{}], order=[{}]",
            templateKey, orderKey);
        return Response.ok().entity(ResEntityHelper.ok(
            String.format("Task order instantiated successfully from template %s", templateKey), order))
            .build();
    }

    /**
     * Instantiate a concrete {@link TaskOrder} from a specific template version.
     *
     * @param templateKey the template business key
     * @param version the specific template version to use
     * @param orderKey the unique business key for the new task order
     * @param orderName the name for the new task order
     * @param parameters parameter values to inject into the template
     * @return HTTP 200 OK with the instantiated task order, HTTP 400 if template not found
     */
    @POST
    @Path("/{templateKey}/v/{version}/instantiate")
    @Operation(summary = "Instantiate task order from specific template version",
        description = "Creates a concrete TaskOrder from a specific template version with provided parameters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task order instantiated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TaskOrder.class))),
        @ApiResponse(responseCode = "400", description = "Template not found or order key already exists",
            content = @Content)
    })
    public Response instantiateWithVersion(
        @Parameter(description = "Template business key", required = true)
        @PathParam("templateKey") String templateKey,
        @Parameter(description = "Template version to use", required = true)
        @PathParam("version") String version,
        @Parameter(description = "Unique business key for the new task order", required = true)
        @QueryParam("orderKey") String orderKey,
        @Parameter(description = "Name for the new task order", required = true)
        @QueryParam("orderName") String orderName,
        @Parameter(description = "Parameter values to inject into the template", required = false)
        Map<String, String> parameters) {
        Optional<TaskOrder> orderOpt = taskTemplateService.instantiateFromTemplate(
            templateKey, version, orderKey, orderName, parameters);
        if (orderOpt.isEmpty()) {
            LOGGER.warn("Failed to instantiate from template version: key=[{}], version=[{}]",
                templateKey, version);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResEntityHelper.badRequest(
                    String.format(
                        "Failed to instantiate: template version '%s/%s' not found or order key '%s' already exists",
                        templateKey, version, orderKey)))
                .build();
        }
        TaskOrder order = orderOpt.get();
        LOGGER.info("Instantiated task order from template version: template=[{}]/[{}], order=[{}]",
            templateKey, version, orderKey);
        return Response.ok().entity(ResEntityHelper.ok(
            String.format("Task order instantiated successfully from template %s/%s", templateKey, version), order))
            .build();
    }
}