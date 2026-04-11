# Simplify TaskTemplateApi Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Simplify TaskTemplateApi to only expose endpoints matching existing methods from TaskTemplateService by removing 5 non-working endpoints and adding 2 missing search endpoints.

**Architecture:** Only modify the existing `TaskTemplateApi.java` file. Remove the 5 extra endpoints that call non-existent service methods. Add 2 new endpoints that correspond to the existing `findAll` and `find` methods in TaskTemplateService. Keep all existing endpoints that already match service methods.

**Tech Stack:** Java 25, Maven, JAX-RS, Swagger/OpenAPI

---

### Task 1: Remove extra endpoints and add new search endpoints to TaskTemplateApi

**Files:**
- Modify: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/interfaces/TaskTemplateApi.java`

- [ ] **Step 1: Read current file content** (already done in exploration)

Current full content is known from exploration. We need to keep the package declaration, imports, class declaration, and the 4 matching endpoints. Remove 5 extra endpoints and add 2 new endpoints.

- [ ] **Step 2: Keep existing imports are already correct**

Verify these imports already exist:
```java
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;
```
Both are already imported, no new imports needed.

- [ ] **Step 3: Remove these 5 methods: listAllActive(), listAll(), listVersions(), getActive(), getByVersion()**

Delete the entire method definitions starting from line 69 (listAllActive) through line 187 (getByVersion).

- [ ] **Step 4: Add the two new search endpoints after the delete method**

Add this code after the `delete` method and before the closing class brace:

```java
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
     * @param page the page request containing page number and page size
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
        @Parameter(description = "Pagination information (page number, page size)", required = true)
        Pageable page) {
        Page<TaskTemplate> result = taskTemplateService.find(searchCriteria, page);
        LOGGER.debug("Found {} total templates matching search criteria on page {}",
            result.getTotalElements(), result.getNumber());
        return Response.ok().entity(ResEntityHelper.ok("Templates paged search completed", result)).build();
    }
```

- [ ] **Step 5: Update JavaDoc class comment to reflect simplified API**

Update the class JavaDoc to:

```java
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
```

- [ ] **Step 6: Compile to verify no errors**

Run: `cd D:/project/nas_gogs/zora-apps/dag-task && mvn compile -pl dag-scheduler -am

Expected: Compilation success (note: dag-si module may fail due to unrelated AgentRegistration issue, but dag-scheduler should compile if our changes are correct)

- [ ] **Step 7: Commit changes**

```bash
cd D:/project/nas_gogs/zora-apps
git add dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/interfaces/TaskTemplateApi.java
git commit -m "refactor: simplify TaskTemplateApi - remove extra endpoints, add search endpoints"
```

---

### Task 2: Final verification

**Files:** None to modify

- [ ] **Step 1: Verify only the expected changes were made**

Check that after changes:
- 5 extra endpoints removed: listAllActive, listAll, listVersions, getActive, getByVersion ✓
- 2 new endpoints added: search, searchPaged ✓
- 4 existing endpoints kept: create, update, deactivate, delete ✓
- All endpoints correspond to existing service methods ✓

- [ ] **Step 2: Verify no references to missing service methods remain**

Run: `grep -n "getAllActive\|getAll\|getVersions\|getActive\|getByVersion" dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/interfaces/TaskTemplateApi.java`
Expected: No output (no matches found)

- [ ] **Step 3: Done**

---
