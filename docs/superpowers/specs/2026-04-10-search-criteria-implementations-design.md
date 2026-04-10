# SearchCriteria Implementations for DAG Task Entities

## Date
2026-04-10

## Context
The current DAG task scheduling system has only `TaskSearchCriteria` for searching `TaskRecord` entities. We need additional `SearchCriteria` implementations for other core entities to provide a consistent query interface and avoid having many specialized query methods.

## Scope
Implement `SearchCriteria` for the following entities:
1. `TaskTemplate` - Reusable DAG workflow templates
2. `TaskDispatchRecord` - Task dispatch tracking records 
3. `TaskOrder` - Top-level task order/execution instances
4. `AgentInfo` - Registered agent information (what user referred to as AgentRegistry)

## Architecture/Design

### Pattern
All implementations follow the exact same pattern as the existing `TaskSearchCriteria`:
- Implements `top.ilovemyhome.zora.jdbi.SearchCriteria` interface
- Immutable with all fields `final`
- Uses builder pattern with Jackson JSON annotations (`@JsonDeserialize`, `@JsonPOJOBuilder`)
- Dynamic SQL WHERE clause construction in `prepareQuery()` method called from constructor
- Separates normal parameters (`Map<String, Object>`) and list parameters for IN clauses (`Map<String, List>`)
- Follows existing coding conventions and style

### Package Location
All classes are placed in `top.ilovemyhome.dagtask.core.task` package, consistent with existing `TaskSearchCriteria`.

### Search Criteria Details

#### TaskTemplateSearchCriteria
| Field | Type | Query Type | Description |
|-------|------|-----------|-------------|
| listOfId | List&lt;Long&gt; | IN | Filter by multiple IDs |
| templateKey | String | Equality | Filter by exact template key |
| templateKeyPrefix | String | LIKE | Prefix search on template key |
| templateName | String | Equality | Filter by exact template name |
| descriptionContains | String | LIKE | Contains search on description |
| version | String | Equality | Filter by exact version |
| active | Boolean | Boolean | Filter active/inactive templates |
| versionSeq | Integer | Equality | Filter by version sequence number |

#### TaskDispatchRecordSearchCriteria
| Field | Type | Query Type | Description |
|-------|------|-----------|-------------|
| listOfId | List&lt;Long&gt; | IN | Filter by multiple IDs |
| listOfTaskId | List&lt;Long&gt; | IN | Filter by multiple task IDs |
| agentId | String | Equality | Filter by agent ID |
| agentUrlPrefix | String | LIKE | Prefix search on agent URL |
| statusList | List&lt;DispatchStatus&gt; | IN | Filter by multiple dispatch statuses |
| dispatchTimeAfter | LocalDateTime | Range | Dispatched after this time |
| dispatchTimeBefore | LocalDateTime | Range | Dispatched before this time |

#### TaskOrderSearchCriteria
| Field | Type | Query Type | Description |
|-------|------|-----------|-------------|
| listOfId | List&lt;Long&gt; | IN | Filter by multiple IDs |
| name | String | Equality | Filter by exact name |
| namePrefix | String | LIKE | Prefix search on name |
| key | String | Equality | Filter by exact order key |
| keyPrefix | String | LIKE | Prefix search on order key |
| orderType | OrderType | Equality | Filter by order type |

#### AgentInfoSearchCriteria
| Field | Type | Query Type | Description |
|-------|------|-----------|-------------|
| listOfId | List&lt;Long&gt; | IN | Filter by multiple IDs |
| agentId | String | Equality | Filter by agent ID |
| agentIdPrefix | String | LIKE | Prefix search on agent ID |
| agentUrlPrefix | String | LIKE | Prefix search on agent URL |
| running | Boolean | Boolean | Filter running/disconnected agents |
| supportedExecutionKey | String | Contains | Filter agents that support a specific execution key |

## Dependencies
- All classes depend on:
  - `top.ilovemyhome.zora.jdbi.SearchCriteria`
  - `top.ilovemyhome.zora.jdbi.page.Pageable`
  - Jackson annotations for JSON support
  - Guava for ImmutableMap
  - Apache Commons Lang3 for StringUtils
  - Existing entity/enum classes from `dag-task-si` module

## Success Criteria
- All classes compile successfully
- Each class correctly implements the `SearchCriteria` interface methods
- SQL WHERE clause is built correctly based on non-null/non-empty criteria
- Consistent style with existing `TaskSearchCriteria`
- No code duplication or duplication of the pattern

## Implementation Notes
- All fields are nullable - criteria are only added to WHERE clause if field is non-null/non-empty
- Builder pattern follows the same conventions as existing code
- Uses the same null-checking approach as the existing implementation
