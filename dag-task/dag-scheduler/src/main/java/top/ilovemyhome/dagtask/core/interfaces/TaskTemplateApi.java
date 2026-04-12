package top.ilovemyhome.dagtask.core.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.service.TaskTemplateService;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;
import top.ilovemyhome.zora.jdbi.page.impl.PageRequest;

import java.util.List;

/**
 * REST API endpoints for managing DAG task templates.
 * <p>
 * This API provides template management corresponding to all {@link TaskTemplateService} operations:
 * <ul>
 *     <li>Create new template versions</li>
 *     <li>Update existing template versions</li>
 *     <li>Deactivate and delete template versions</li>
 *     <li>Search templates by criteria</li>
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
        return Response.ok().entity(ResEntityHelper.ok("Template deleted successfully", null)).build();
    }

    /**
     * Search templates by criteria, returns all matching results.
     *
     * @param searchCriteria the search criteria to filter templates
     * @return HTTP 200 OK with list of matching templates
     */
    @POST
    @Path("/search")
    @Operation(summary = "Search templates by criteria", description = "Returns all template versions matching the search criteria")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved matching templates",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = List.class)))
    })
    public Response search(
        @Parameter(description = "Search criteria for filtering templates", required = true)
        TaskTemplateSearchCriteria searchCriteria) {
        List<TaskTemplate> templates = taskTemplateService.findAll(searchCriteria);
        LOGGER.debug("Found {} templates matching search criteria", templates.size());
        return Response.ok().entity(ResEntityHelper.ok("Templates search completed", templates)).build();
    }

    /**
     * Search templates by criteria with pagination.
     *
     * @param searchCriteria the search criteria to filter templates
     * @param page page number (0-indexed)
     * @param pageSize number of items per page
     * @return HTTP 200 OK with paginated result of matching templates
     */
    @POST
    @Path("/search/page")
    @Operation(summary = "Search templates by criteria (paged)", description = "Returns paginated template versions matching the search criteria")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved matching templates page",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Page.class)))
    })
    public Response searchPaged(
        @Parameter(description = "Search criteria for filtering templates", required = true)
        TaskTemplateSearchCriteria searchCriteria,
        @Parameter(description = "Page number (0-indexed), default 0", required = false)
        @QueryParam("page") @DefaultValue("0") int page,
        @Parameter(description = "Page size, default 20", required = false)
        @QueryParam("pageSize") @DefaultValue("20") int pageSize) {
        Pageable pageRequest = new PageRequest(page, pageSize);
        Page<TaskTemplate> result = taskTemplateService.find(searchCriteria, pageRequest);
        LOGGER.debug("Found {} total templates matching search criteria on page {}",
            result.getTotalElements(), result.getNumber());
        return Response.ok().entity(ResEntityHelper.ok("Templates paged search completed", result)).build();
    }

}
