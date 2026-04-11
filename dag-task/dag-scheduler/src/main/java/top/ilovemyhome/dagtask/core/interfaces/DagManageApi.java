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
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;
import top.ilovemyhome.dagtask.si.service.DagManageService;
import top.ilovemyhome.dagtask.si.service.TaskTemplateService;

import java.util.Map;
import java.util.Optional;

@Path("/api/v1/dag/manage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DagManageApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(DagManageApi.class);

    private final DagManageService dagManageService;


    @Inject
    public DagManageApi(DagManageService dagManageService) {
        this.dagManageService = dagManageService;
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
        Optional<TaskOrder> orderOpt = dagManageService.instantiateFromTemplate(
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
        Optional<TaskOrder> orderOpt = dagManageService.instantiateFromTemplate(
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
