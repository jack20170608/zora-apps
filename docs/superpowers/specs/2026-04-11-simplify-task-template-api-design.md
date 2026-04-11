# Simplify TaskTemplateApi Design

## Overview

Simplify `TaskTemplateApi` to only expose endpoints that correspond to methods existing in `TaskTemplateService` interface. Remove extra API endpoints that call non-existent methods on the service. Add missing search endpoints that correspond to the existing `findAll` and `find` methods.

## Background

- `TaskTemplateService` interface has exactly 6 methods: `createTemplate`, `updateTemplate`, `deactivateVersion`, `deleteVersion`, `findAll`, `find`
- `TaskTemplateApi` currently exposes 10 endpoints, 5 of which call methods that don't exist in `TaskTemplateService`
- The extra endpoints cannot work correctly since the required methods are missing from the service layer
- The requirement is to simplify the API so it only exposes functionality that is actually implemented in the service

## Design Decision

**Approach:** Simplify by removing non-working extra endpoints, add missing endpoints for existing service methods.

### After Simplification

`TaskTemplateApi` will have exactly 6 endpoints, one for each method in `TaskTemplateService`:

| Method | Endpoint | Corresponding Service Method | Description |
|--------|----------|--------------------------------|-------------|
| POST | `/api/v1/template` | `createTemplate` | Create new template version |
| PUT | `/api/v1/template/{templateKey}/v/{version}` | `updateTemplate` | Update existing template version |
| POST | `/api/v1/template/{templateKey}/v/{version}/deactivate` | `deactivateVersion` | Deactivate a template version |
| DELETE | `/api/v1/template/{templateKey}/v/{version}` | `deleteVersion` | Delete a template version |
| POST | `/api/v1/template/search` | `findAll` | Search templates by criteria returns list |
| POST | `/api/v1/template/search/page` | `find` | Search templates by criteria returns paged results |

### Deletions

Remove these 5 endpoints that call non-existent service methods:
- `GET /api/v1/template` (listAllActive - calls `getAllActive`)
- `GET /api/v1/template/all` (listAll - calls `getAll`)
- `GET /api/v1/template/{templateKey}/versions` (listVersions - calls `getVersions`)
- `GET /api/v1/template/{templateKey}/active` (getActive - calls `getActive`)
- `GET /api/v1/template/{templateKey}/v/{version}` (getByVersion - calls `getByVersion`)

### Additions

Add these 2 new endpoints to cover the existing service methods:
- `POST /api/v1/template/search` - searches with criteria returns full list
- `POST /api/v1/template/search/page` - searches with criteria returns paginated result

## Implementation Details

### Method Signatures for New Endpoints

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
Response search(TaskTemplateSearchCriteria searchCriteria);

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
Response searchPaged(TaskTemplateSearchCriteria searchCriteria, Pageable page);
```

### Implementation Logic

Both new endpoints simply delegate to the corresponding service methods:
- `search` → `taskTemplateService.findAll(searchCriteria)`
- `searchPaged` → `taskTemplateService.find(searchCriteria, page)`

### Dependencies

- No new dependencies needed
- `TaskTemplateSearchCriteria` and `Pageable` are already imported and available
- The service already implements the required methods

## Testing

- Compile verification after changes
- No existing tests to update since the removed endpoints weren't working anyway
- The new endpoints use existing service implementation which is already in place

## Success Criteria

- Code compiles successfully
- All API endpoints correspond to existing service methods
- No compile errors from missing method calls
- Exactly 6 endpoints, one per service method
