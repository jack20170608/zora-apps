# Dag Scheduler Hexagonal Step 4 Cutover Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cut `dag-admin-muserver` and `dag-allinone-muserver` over to the new scheduler hexagonal modules, then remove legacy `dag-scheduler` and `dag-scheduler-muserver` modules.

**Architecture:** Create focused web adapter resources in `dag-scheduler-adapter-web-muserver` that depend only on inbound use cases and repository ports. Create a central `SchedulerContext` in `dag-scheduler-app` that wires persistence adapters, application services, token service, and HTTP dispatcher. Entry modules consume `SchedulerContext` instead of legacy `DagSchedulerBuilder` / `DagSchedulerServer`.

**Tech Stack:** Java 25, Maven, JUnit 5, Mockito, AssertJ, MuServer/JAX-RS, Jdbi, Flyway, Zora utilities, SLF4J.

---

## Design Inputs

- Spec: `dag-task/docs/superpowers/specs/2026-06-11-dag-scheduler-hexagonal-step4-cutover-design.md`
- Architecture doc: `dag-task/docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`
- Existing old APIs to migrate:
  - `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/interfaces/TaskOrderApi.java`
  - `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/interfaces/TaskTemplateApi.java`
  - `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/interfaces/DagManageApi.java`
  - `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/core/interfaces/AgentRegistryApi.java`
- Existing entry points to cut over:
  - `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/application/AppContext.java`
  - `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/application/WebServerBootstrap.java`
  - `dag-task/dag-allinone-muserver/src/main/java/top/ilovemyhome/dagtask/allinone/muserver/application/AllInOneAppContext.java`
  - `dag-task/dag-allinone-muserver/src/main/java/top/ilovemyhome/dagtask/allinone/muserver/application/AllInOneWebServerBootstrap.java`

## Target File Structure

### `dag-scheduler-adapter-web-muserver`

Create:

- `src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/TaskOrderApi.java`
  - REST adapter for `/api/v1/order`.
  - Uses `ManageTaskOrderUseCase`, `QueryTaskOrderUseCase`, and `TaskOrderRepository` for legacy list/get response compatibility.
- `src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/TaskTemplateApi.java`
  - REST adapter for `/api/v1/template`.
  - Uses `ManageTaskTemplateUseCase` and `QueryTaskTemplateUseCase`.
- `src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/DagManageApi.java`
  - REST adapter for `/api/v1/dag/manage` template instantiation endpoints.
  - Uses `InstantiateDagTemplateUseCase`.
- `src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/AgentRegistryApi.java`
  - REST adapter for agent registration/heartbeat/result endpoints.
  - Uses `RegisterAgentUseCase`, `AgentHeartbeatUseCase`, `ReportTaskResultUseCase`.
- `src/test/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/*Test.java`
  - Mockito unit tests for adapter behavior and response mapping.

Modify:

- `dag-scheduler-adapter-web-muserver/pom.xml`
  - Add dependencies needed by migrated old APIs: `dag-si`, swagger annotations, Jakarta inject/ws-rs if not already transitive.

### `dag-scheduler-app`

Create:

- `src/main/java/top/ilovemyhome/dagtask/scheduler/app/HttpAgentDispatcher.java`
  - Replacement for legacy `DefaultTaskDispatcher` AgentDispatcher role.
  - Sends HTTP submit requests to agents.
- `src/main/java/top/ilovemyhome/dagtask/scheduler/app/LegacyTokenIssuer.java`
  - Moves old `core.adapter.LegacyTokenIssuer` into non-legacy app module.
- `src/main/java/top/ilovemyhome/dagtask/scheduler/app/SchedulerContext.java`
  - Central manual DI context.
  - Owns repositories, application services, token service, dispatcher.
- `src/test/java/top/ilovemyhome/dagtask/scheduler/app/HttpAgentDispatcherTest.java`
- `src/test/java/top/ilovemyhome/dagtask/scheduler/app/SchedulerContextTest.java`

Modify:

- `dag-scheduler-app/pom.xml`
  - Add `dag-si`, zora-json/Jackson, JWT/token dependencies as needed.

### Entry modules

Modify:

- `dag-admin-muserver/pom.xml`
- `dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/application/AppContext.java`
- `dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/application/WebServerBootstrap.java`
- `dag-allinone/pom.xml`
- `dag-allinone-muserver/pom.xml`
- `dag-allinone-muserver/src/main/java/top/ilovemyhome/dagtask/allinone/muserver/application/AllInOneAppContext.java`
- `dag-allinone-muserver/src/main/java/top/ilovemyhome/dagtask/allinone/muserver/application/AllInOneWebServerBootstrap.java`

### Removal and docs

Delete only after reference scans are clean:

- `dag-task/dag-scheduler/`
- `dag-task/dag-scheduler-muserver/`

Modify:

- `dag-task/pom.xml`
- `dag-task/CLAUDE.md`
- `dag-task/docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`

---

## Task 1: Add web adapter tests and dependencies

**Files:**
- Modify: `dag-task/dag-scheduler-adapter-web-muserver/pom.xml`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/src/test/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/TaskTemplateApiTest.java`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/src/test/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/DagManageApiTest.java`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/src/test/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/AgentRegistryApiTest.java`

- [ ] **Step 1: Update web adapter pom dependencies**

Add dependencies that the migrated APIs need. Insert after the existing `dag-scheduler-domain` dependency:

```xml
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-si</artifactId>
        </dependency>
```

If compilation later reports missing annotations, add explicit dependencies in the same pom:

```xml
        <dependency>
            <groupId>io.swagger.core.v3</groupId>
            <artifactId>swagger-annotations-jakarta</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.inject</groupId>
            <artifactId>jakarta.inject-api</artifactId>
            <version>2.0.1</version>
        </dependency>
```

- [ ] **Step 2: Write TaskTemplateApi unit test**

Create `TaskTemplateApiTest.java`:

```java
package top.ilovemyhome.dagtask.scheduler.adapter.web.muserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.scheduler.port.in.ManageTaskTemplateUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.QueryTaskTemplateUseCase;
import top.ilovemyhome.dagtask.si.TaskTemplate;

class TaskTemplateApiTest {

    @Test
    void create_whenUseCaseReturnsFalse_returnsBadRequest() {
        ManageTaskTemplateUseCase manage = mock(ManageTaskTemplateUseCase.class);
        QueryTaskTemplateUseCase query = mock(QueryTaskTemplateUseCase.class);
        TaskTemplate template = new TaskTemplate();
        template.setTemplateKey("daily-job");
        template.setVersion("v1");
        when(manage.createTemplate(template, true)).thenReturn(false);

        Response response = new TaskTemplateApi(query, manage).create(template, true);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(manage).createTemplate(template, true);
    }
}
```

- [ ] **Step 3: Write DagManageApi unit test**

Create `DagManageApiTest.java`:

```java
package top.ilovemyhome.dagtask.scheduler.adapter.web.muserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.scheduler.port.in.InstantiateDagTemplateUseCase;

class DagManageApiTest {

    @Test
    void instantiate_whenTemplateMissing_returnsBadRequest() {
        InstantiateDagTemplateUseCase useCase = mock(InstantiateDagTemplateUseCase.class);
        when(useCase.instantiateFromTemplate("tpl", "order-1", "Order 1", Map.of()))
            .thenReturn(Optional.empty());

        Response response = new DagManageApi(useCase).instantiate("tpl", "order-1", "Order 1", Map.of());

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }
}
```

- [ ] **Step 4: Write AgentRegistryApi unit test**

Create `AgentRegistryApiTest.java`:

```java
package top.ilovemyhome.dagtask.scheduler.adapter.web.muserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.scheduler.port.in.AgentHeartbeatUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.RegisterAgentUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.ReportTaskResultUseCase;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterResponse;

class AgentRegistryApiTest {

    @Test
    void register_whenUseCaseRejects_returnsBadRequest() {
        RegisterAgentUseCase register = mock(RegisterAgentUseCase.class);
        AgentHeartbeatUseCase heartbeat = mock(AgentHeartbeatUseCase.class);
        ReportTaskResultUseCase report = mock(ReportTaskResultUseCase.class);
        AgentRegisterRequest request = new AgentRegisterRequest(
            "agent-1", "Agent 1", "http://localhost:8081", 2, java.util.List.of("java"), false);
        when(register.registerAgent(request, null))
            .thenReturn(new AgentRegisterResponse(false, "rejected", null, null, 0L));

        Response response = new AgentRegistryApi(register, heartbeat, report).register(request, null);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }
}
```

If constructors differ, inspect `dag-si/src/main/java/top/ilovemyhome/dagtask/si/agent/*.java` and adjust test construction only; keep the expected behavior unchanged.

- [ ] **Step 5: Run tests and verify they fail because APIs do not exist**

Run:

```bash
mvn -f dag-task/pom.xml -pl dag-scheduler-adapter-web-muserver test -q
```

Expected: compilation fails with `cannot find symbol: class TaskTemplateApi`, `DagManageApi`, and `AgentRegistryApi`.

---

## Task 2: Implement web adapter APIs

**Files:**
- Create: `dag-task/dag-scheduler-adapter-web-muserver/src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/TaskOrderApi.java`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/TaskTemplateApi.java`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/DagManageApi.java`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/AgentRegistryApi.java`

- [ ] **Step 1: Implement TaskTemplateApi**

Copy the old class from `dag-scheduler/core/interfaces/TaskTemplateApi.java`, change package to:

```java
package top.ilovemyhome.dagtask.scheduler.adapter.web.muserver;
```

Replace the field and constructor with:

```java
private final QueryTaskTemplateUseCase queryTemplateUseCase;
private final ManageTaskTemplateUseCase manageTemplateUseCase;

@Inject
public TaskTemplateApi(QueryTaskTemplateUseCase queryTemplateUseCase,
                       ManageTaskTemplateUseCase manageTemplateUseCase) {
    this.queryTemplateUseCase = Objects.requireNonNull(queryTemplateUseCase, "queryTemplateUseCase must not be null");
    this.manageTemplateUseCase = Objects.requireNonNull(manageTemplateUseCase, "manageTemplateUseCase must not be null");
}
```

Update method calls:

```java
manageTemplateUseCase.createTemplate(template, setActive);
manageTemplateUseCase.updateTemplate(template);
manageTemplateUseCase.deactivateVersion(templateKey, version);
manageTemplateUseCase.deleteVersion(templateKey, version);
queryTemplateUseCase.findAll(searchCriteria);
queryTemplateUseCase.find(searchCriteria, pageRequest);
```

Use domain page types, not zora page types:

```java
import top.ilovemyhome.dagtask.scheduler.domain.query.Page;
import top.ilovemyhome.dagtask.scheduler.domain.query.Pageable;
import top.ilovemyhome.dagtask.scheduler.domain.query.PageRequest;
```

- [ ] **Step 2: Implement DagManageApi**

Copy the old class from `dag-scheduler/core/interfaces/DagManageApi.java`, change package to web adapter package, and replace field/constructor with:

```java
private final InstantiateDagTemplateUseCase instantiateDagTemplateUseCase;

@Inject
public DagManageApi(InstantiateDagTemplateUseCase instantiateDagTemplateUseCase) {
    this.instantiateDagTemplateUseCase = Objects.requireNonNull(
        instantiateDagTemplateUseCase, "instantiateDagTemplateUseCase must not be null");
}
```

Update calls:

```java
Optional<TaskOrder> orderOpt = instantiateDagTemplateUseCase.instantiateFromTemplate(
    templateKey, orderKey, orderName, parameters);

Optional<TaskOrder> orderOpt = instantiateDagTemplateUseCase.instantiateFromTemplate(
    templateKey, version, orderKey, orderName, parameters);
```

- [ ] **Step 3: Implement AgentRegistryApi**

Copy the old class from `dag-scheduler/core/interfaces/AgentRegistryApi.java`, change package to web adapter package, and replace field/constructor with:

```java
private final RegisterAgentUseCase registerAgentUseCase;
private final AgentHeartbeatUseCase agentHeartbeatUseCase;
private final ReportTaskResultUseCase reportTaskResultUseCase;

@Inject
public AgentRegistryApi(RegisterAgentUseCase registerAgentUseCase,
                        AgentHeartbeatUseCase agentHeartbeatUseCase,
                        ReportTaskResultUseCase reportTaskResultUseCase) {
    this.registerAgentUseCase = Objects.requireNonNull(registerAgentUseCase, "registerAgentUseCase must not be null");
    this.agentHeartbeatUseCase = Objects.requireNonNull(agentHeartbeatUseCase, "agentHeartbeatUseCase must not be null");
    this.reportTaskResultUseCase = Objects.requireNonNull(reportTaskResultUseCase, "reportTaskResultUseCase must not be null");
}
```

Update calls:

```java
AgentRegisterResponse response = registerAgentUseCase.registerAgent(registration, clientIp);
boolean success = registerAgentUseCase.unregisterAgent(unregistration);
boolean success = reportTaskResultUseCase.reportTaskResult(taskExecuteResult);
boolean success = agentHeartbeatUseCase.reportAgentStatus(statusReport);
```

- [ ] **Step 4: Implement TaskOrderApi**

Copy the old class from `dag-scheduler/core/interfaces/TaskOrderApi.java`, change package to web adapter package, and replace field/constructor with:

```java
private final TaskOrderRepository taskOrderRepository;
private final QueryTaskOrderUseCase queryTaskOrderUseCase;
private final ManageTaskOrderUseCase manageTaskOrderUseCase;

@Inject
public TaskOrderApi(TaskOrderRepository taskOrderRepository,
                    QueryTaskOrderUseCase queryTaskOrderUseCase,
                    ManageTaskOrderUseCase manageTaskOrderUseCase) {
    this.taskOrderRepository = Objects.requireNonNull(taskOrderRepository, "taskOrderRepository must not be null");
    this.queryTaskOrderUseCase = Objects.requireNonNull(queryTaskOrderUseCase, "queryTaskOrderUseCase must not be null");
    this.manageTaskOrderUseCase = Objects.requireNonNull(manageTaskOrderUseCase, "manageTaskOrderUseCase must not be null");
}
```

Use `taskOrderRepository.findAll()` and `findByKey(key)` for read response payload compatibility. Use use cases for writes:

```java
Long id = manageTaskOrderUseCase.createOrder(taskOrder);
int updated = manageTaskOrderUseCase.updateOrderByKey(key, taskOrder);
int deleted = manageTaskOrderUseCase.deleteOrderByKey(key);
```

When `createOrder` throws `OrderKeyAlreadyExistsException`, map to HTTP 400 with the same response envelope used by old API.

- [ ] **Step 5: Run web adapter tests**

Run:

```bash
mvn -f dag-task/pom.xml -pl dag-scheduler-adapter-web-muserver test -q
```

Expected: BUILD SUCCESS.

---

## Task 3: Implement scheduler app context and HTTP dispatcher

**Files:**
- Modify: `dag-task/dag-scheduler-app/pom.xml`
- Create: `dag-task/dag-scheduler-app/src/main/java/top/ilovemyhome/dagtask/scheduler/app/HttpAgentDispatcher.java`
- Create: `dag-task/dag-scheduler-app/src/main/java/top/ilovemyhome/dagtask/scheduler/app/LegacyTokenIssuer.java`
- Create: `dag-task/dag-scheduler-app/src/main/java/top/ilovemyhome/dagtask/scheduler/app/SchedulerContext.java`
- Create: `dag-task/dag-scheduler-app/src/test/java/top/ilovemyhome/dagtask/scheduler/app/HttpAgentDispatcherTest.java`
- Create: `dag-task/dag-scheduler-app/src/test/java/top/ilovemyhome/dagtask/scheduler/app/SchedulerContextTest.java`

- [ ] **Step 1: Update scheduler app pom**

Add dependencies after the existing scheduler adapter dependencies:

```xml
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-si</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-json</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
```

- [ ] **Step 2: Write HttpAgentDispatcher test**

Create `HttpAgentDispatcherTest.java` with a fake `HttpClient` if feasible. If `HttpClient` is hard to fake because `send` is final in the used JDK API, test helper methods and constructor validation instead:

```java
package top.ilovemyhome.dagtask.scheduler.app;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HttpAgentDispatcherTest {

    @Test
    void constructor_rejectsNullObjectMapper() {
        assertThatThrownBy(() -> new HttpAgentDispatcher(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("objectMapper");
    }
}
```

- [ ] **Step 3: Implement HttpAgentDispatcher**

Move the `AgentDispatcher` HTTP delivery behavior from legacy `DefaultTaskDispatcher.dispatch(AgentStatus, TaskRecord)` into `HttpAgentDispatcher`:

```java
package top.ilovemyhome.dagtask.scheduler.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentDispatcher;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentUnreachableException;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.dagtask.si.dto.SubmitRequest;
import top.ilovemyhome.dagtask.si.enums.TaskType;

import static top.ilovemyhome.dagtask.si.Constants.API_SUBMIT;
import static top.ilovemyhome.dagtask.si.Constants.API_VERSION;

/**
 * HTTP implementation of the AgentDispatcher outbound port.
 */
public class HttpAgentDispatcher implements AgentDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAgentDispatcher.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpAgentDispatcher(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newHttpClient());
    }

    public HttpAgentDispatcher(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public DispatchAck dispatch(AgentStatus targetAgent, TaskRecord task) {
        Objects.requireNonNull(targetAgent, "targetAgent must not be null");
        Objects.requireNonNull(task, "task must not be null");
        String submitUrl = buildAgentUrl(targetAgent.getAgentUrl()) + API_VERSION + API_SUBMIT;
        SubmitRequest submitRequest = new SubmitRequest(
            task.getId(), task.getName(), TaskType.JAVA_CLASS_NAME, task.getExecutionKey(), task.getInput());
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(submitRequest);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize submission request for task {}", task.getId(), e);
            return new DispatchAck(false, e.getMessage());
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(submitUrl))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return toAck(targetAgent, task, response);
        } catch (IOException e) {
            throw new AgentUnreachableException(
                "IOException connecting to agent " + targetAgent.getAgentId() + " at " + targetAgent.getAgentUrl(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentUnreachableException("Interrupted while connecting to agent " + targetAgent.getAgentId(), e);
        }
    }

    private DispatchAck toAck(AgentStatus targetAgent, TaskRecord task, HttpResponse<String> response) {
        int statusCode = response.statusCode();
        if (statusCode == 202) {
            LOGGER.info("Task {} dispatched successfully to agent {}", task.getId(), targetAgent.getAgentId());
            return new DispatchAck(true, "");
        }
        if (statusCode == 429) {
            return new DispatchAck(false, "pending queue is full (429)");
        }
        if (statusCode == 400) {
            return new DispatchAck(false, "bad request (400): " + response.body());
        }
        return new DispatchAck(false, "unexpected status code " + statusCode + ": " + response.body());
    }

    private static String buildAgentUrl(String agentUrl) {
        if (agentUrl.endsWith("/")) {
            return agentUrl.substring(0, agentUrl.length() - 1);
        }
        return agentUrl;
    }
}
```

- [ ] **Step 4: Implement LegacyTokenIssuer in scheduler app**

Create `LegacyTokenIssuer.java`:

```java
package top.ilovemyhome.dagtask.scheduler.app;

import java.util.Objects;
import top.ilovemyhome.dagtask.scheduler.port.out.TokenIssuer;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;

/**
 * TokenIssuer adapter that delegates to the existing TokenService implementation.
 */
public class LegacyTokenIssuer implements TokenIssuer {

    private final TokenService tokenService;

    public LegacyTokenIssuer(TokenService tokenService) {
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService must not be null");
    }

    @Override
    public TokenInfo issueAgentToken(String agentId, String name, String description, int validDays, String issuer) {
        return tokenService.generateToken(agentId, name, description, validDays, issuer);
    }
}
```

If `TokenService` remains in old `dag-scheduler`, move `TokenService` and `TokenManagementApi` in a later task before deleting the module. Do not add a dependency from `dag-scheduler-app` to old `dag-scheduler`.

- [ ] **Step 5: Implement SchedulerContext**

Create `SchedulerContext.java`. It must have final fields for every repository/use case that entry modules need. Use constructor injection from `Jdbi`, `ObjectMapper`, `JwtConfig`, and `LoadBalanceStrategy`:

```java
package top.ilovemyhome.dagtask.scheduler.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.jdbi.v3.core.Jdbi;
import top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc.*;
import top.ilovemyhome.dagtask.scheduler.application.*;
import top.ilovemyhome.dagtask.scheduler.config.JwtConfig;
import top.ilovemyhome.dagtask.scheduler.domain.dispatcher.LoadBalanceStrategy;
import top.ilovemyhome.dagtask.scheduler.port.in.*;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;

/**
 * Manual DI context for scheduler hexagonal modules.
 */
public class SchedulerContext {

    private final AgentDaoJdbiImpl agentRepository;
    private final AgentStatusDaoJdbiImpl agentStatusRepository;
    private final AgentTokenDaoJdbiImpl agentTokenRepository;
    private final AgentWhitelistDaoJdbiImpl agentWhitelistRepository;
    private final TaskDispatchDaoJdbiImpl taskDispatchRepository;
    private final TaskOrderDaoJdbiImpl taskOrderRepository;
    private final TaskRecordDaoJdbiImpl taskRecordRepository;
    private final TaskTemplateDaoJdbiImpl taskTemplateRepository;

    private final TokenService tokenService;
    private final QueryTaskTemplateUseCase queryTaskTemplateUseCase;
    private final ManageTaskTemplateUseCase manageTaskTemplateUseCase;
    private final InstantiateDagTemplateUseCase instantiateDagTemplateUseCase;
    private final QueryTaskOrderUseCase queryTaskOrderUseCase;
    private final ManageTaskOrderUseCase manageTaskOrderUseCase;
    private final RegisterAgentUseCase registerAgentUseCase;
    private final AgentHeartbeatUseCase agentHeartbeatUseCase;
    private final ReportTaskResultUseCase reportTaskResultUseCase;
    private final ScheduleDagRunUseCase scheduleDagRunUseCase;

    public SchedulerContext(Jdbi jdbi, ObjectMapper objectMapper, JwtConfig jwtConfig,
                            LoadBalanceStrategy loadBalanceStrategy) {
        Objects.requireNonNull(jdbi, "jdbi must not be null");
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(jwtConfig, "jwtConfig must not be null");
        Objects.requireNonNull(loadBalanceStrategy, "loadBalanceStrategy must not be null");

        this.agentRepository = new AgentDaoJdbiImpl(jdbi);
        this.agentStatusRepository = new AgentStatusDaoJdbiImpl(jdbi);
        this.agentTokenRepository = new AgentTokenDaoJdbiImpl(jdbi);
        this.agentWhitelistRepository = new AgentWhitelistDaoJdbiImpl(jdbi);
        this.taskDispatchRepository = new TaskDispatchDaoJdbiImpl(jdbi);
        this.taskOrderRepository = new TaskOrderDaoJdbiImpl(jdbi);
        this.taskRecordRepository = new TaskRecordDaoJdbiImpl(jdbi);
        this.taskTemplateRepository = new TaskTemplateDaoJdbiImpl(jdbi);

        var clock = new SystemClock();
        var unitOfWork = new JdbiUnitOfWork(jdbi);
        var idGenerator = new SequenceIdGenerator(jdbi);
        var parser = new JacksonDagDefinitionParser(objectMapper);
        var dispatcher = new HttpAgentDispatcher(objectMapper);
        this.tokenService = new TokenService(agentTokenRepository, jwtConfig);
        var tokenIssuer = new LegacyTokenIssuer(tokenService);

        var taskTemplateService = new TaskTemplateApplicationService(taskTemplateRepository);
        this.queryTaskTemplateUseCase = taskTemplateService;
        this.manageTaskTemplateUseCase = taskTemplateService;
        this.instantiateDagTemplateUseCase = new InstantiateDagTemplateService(
            taskOrderRepository, taskTemplateRepository, taskRecordRepository, unitOfWork, idGenerator, parser);

        var taskOrderService = new TaskOrderApplicationService(taskOrderRepository, taskRecordRepository, unitOfWork);
        this.queryTaskOrderUseCase = taskOrderService;
        this.manageTaskOrderUseCase = taskOrderService;

        this.registerAgentUseCase = new RegisterAgentService(
            agentRepository, agentStatusRepository, agentWhitelistRepository, tokenIssuer, unitOfWork, clock);
        this.agentHeartbeatUseCase = new AgentHeartbeatService(agentStatusRepository);
        this.reportTaskResultUseCase = new ReportTaskResultService(taskRecordRepository, clock);
        this.scheduleDagRunUseCase = new ScheduleDagRunService(
            taskRecordRepository, dispatcher, agentStatusRepository, taskDispatchRepository, clock, loadBalanceStrategy);
    }

    public AgentDaoJdbiImpl agentRepository() { return agentRepository; }
    public AgentStatusDaoJdbiImpl agentStatusRepository() { return agentStatusRepository; }
    public AgentTokenDaoJdbiImpl agentTokenRepository() { return agentTokenRepository; }
    public AgentWhitelistDaoJdbiImpl agentWhitelistRepository() { return agentWhitelistRepository; }
    public TaskOrderDaoJdbiImpl taskOrderRepository() { return taskOrderRepository; }
    public TaskRecordDaoJdbiImpl taskRecordRepository() { return taskRecordRepository; }
    public TaskTemplateDaoJdbiImpl taskTemplateRepository() { return taskTemplateRepository; }
    public TokenService tokenService() { return tokenService; }
    public QueryTaskTemplateUseCase queryTaskTemplateUseCase() { return queryTaskTemplateUseCase; }
    public ManageTaskTemplateUseCase manageTaskTemplateUseCase() { return manageTaskTemplateUseCase; }
    public InstantiateDagTemplateUseCase instantiateDagTemplateUseCase() { return instantiateDagTemplateUseCase; }
    public QueryTaskOrderUseCase queryTaskOrderUseCase() { return queryTaskOrderUseCase; }
    public ManageTaskOrderUseCase manageTaskOrderUseCase() { return manageTaskOrderUseCase; }
    public RegisterAgentUseCase registerAgentUseCase() { return registerAgentUseCase; }
    public AgentHeartbeatUseCase agentHeartbeatUseCase() { return agentHeartbeatUseCase; }
    public ReportTaskResultUseCase reportTaskResultUseCase() { return reportTaskResultUseCase; }
    public ScheduleDagRunUseCase scheduleDagRunUseCase() { return scheduleDagRunUseCase; }
}
```

If constructors differ from the snippet, inspect the service class and update only the argument list; keep dependency direction unchanged.

- [ ] **Step 6: Run scheduler app tests**

Run:

```bash
mvn -f dag-task/pom.xml -pl dag-scheduler-app -am test -q
```

Expected: BUILD SUCCESS.

---

## Task 4: Move token API/service out of legacy scheduler if still needed

**Files:**
- Inspect: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/token/TokenService.java`
- Inspect: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/token/TokenManagementApi.java`
- Create/Move to: `dag-task/dag-scheduler-app/src/main/java/top/ilovemyhome/dagtask/scheduler/token/TokenService.java`
- Create/Move to: `dag-task/dag-scheduler-adapter-web-muserver/src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/TokenManagementApi.java` OR keep package only if no old-module dependency remains.

- [ ] **Step 1: Inspect token classes**

Read both old token files and list their dependencies. The key rule: after this task no production code may need `dag-scheduler` just for token issuance or management.

- [ ] **Step 2: Move TokenService to a non-legacy module**

If `TokenService` only depends on `AgentTokenDao`/repository and `JwtConfig`, move it to `dag-scheduler-app` or `dag-scheduler-domain` only if it has no infrastructure dependency. Preferred location: `dag-scheduler-app/src/main/java/top/ilovemyhome/dagtask/scheduler/token/TokenService.java`.

Keep package `top.ilovemyhome.dagtask.scheduler.token` to minimize import churn.

- [ ] **Step 3: Move TokenManagementApi to web adapter**

If `TokenManagementApi` is a JAX-RS resource, move it to web adapter package:

```java
package top.ilovemyhome.dagtask.scheduler.adapter.web.muserver;
```

Constructor should accept `TokenService` or a new token use case if one already exists. Do not introduce a new token use case unless required for compilation.

- [ ] **Step 4: Compile affected modules**

Run:

```bash
mvn -f dag-task/pom.xml -pl dag-scheduler-app,dag-scheduler-adapter-web-muserver -am test -q
```

Expected: BUILD SUCCESS.

---

## Task 5: Cut over dag-admin-muserver AppContext and WebServerBootstrap

**Files:**
- Modify: `dag-task/dag-admin-muserver/pom.xml`
- Modify: `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/application/AppContext.java`
- Modify: `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/application/WebServerBootstrap.java`

- [ ] **Step 1: Update pom dependencies**

Remove:

```xml
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler</artifactId>
        </dependency>
```

Add if absent:

```xml
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler-adapter-web-muserver</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler-app</artifactId>
        </dependency>
```

- [ ] **Step 2: Replace AppContext scheduler startup**

In `AppContext.java`, remove imports:

```java
import top.ilovemyhome.dagtask.core.DagSchedulerServer;
import top.ilovemyhome.dagtask.core.server.DagSchedulerBuilder;
```

Add imports:

```java
import top.ilovemyhome.dagtask.scheduler.app.SchedulerContext;
import top.ilovemyhome.dagtask.scheduler.domain.dispatcher.RandomLoadBalance;
```

Replace the constructor lines:

```java
DagSchedulerServer dagServer = startDagServer(jwtConfig);
initApplicationServices(dagServer);
```

with:

```java
SchedulerContext schedulerContext = new SchedulerContext(
    this.jdbi, JacksonUtil.MAPPER, jwtConfig, new RandomLoadBalance());
registerBean(SchedulerContext.class, "schedulerContext", schedulerContext);
initApplicationServices(schedulerContext);
```

Delete `startDagServer(JwtConfig jwtConfig)` entirely.

Replace `initApplicationServices(DagSchedulerServer dagServer)` with:

```java
private void initApplicationServices(SchedulerContext schedulerContext) {
    registerBean(QueryTaskTemplateUseCase.class, "queryTaskTemplateUseCase",
        schedulerContext.queryTaskTemplateUseCase());
    registerBean(ManageTaskTemplateUseCase.class, "manageTaskTemplateUseCase",
        schedulerContext.manageTaskTemplateUseCase());
    registerBean(InstantiateDagTemplateUseCase.class, "instantiateDagTemplateUseCase",
        schedulerContext.instantiateDagTemplateUseCase());
}
```

- [ ] **Step 3: Replace WebServerBootstrap old API imports**

Remove imports:

```java
import top.ilovemyhome.dagtask.core.DagSchedulerServer;
import top.ilovemyhome.dagtask.core.interfaces.DagManageApi;
import top.ilovemyhome.dagtask.core.interfaces.TaskOrderApi;
import top.ilovemyhome.dagtask.core.interfaces.TaskTemplateApi;
```

Add imports:

```java
import top.ilovemyhome.dagtask.scheduler.adapter.web.muserver.DagManageApi;
import top.ilovemyhome.dagtask.scheduler.adapter.web.muserver.TaskOrderApi;
import top.ilovemyhome.dagtask.scheduler.adapter.web.muserver.TaskTemplateApi;
import top.ilovemyhome.dagtask.scheduler.app.SchedulerContext;
```

If `TokenManagementApi` moved, update that import to `top.ilovemyhome.dagtask.scheduler.adapter.web.muserver.TokenManagementApi`.

- [ ] **Step 4: Replace createRestHandler scheduler access**

Replace:

```java
DagSchedulerServer schedulerServer = appContext.getBean("dagSchedulerServer", DagSchedulerServer.class);
```

with:

```java
SchedulerContext schedulerContext = appContext.getBean("schedulerContext", SchedulerContext.class);
```

Replace API construction:

```java
TaskOrderApi taskOrderApi = new TaskOrderApi(
    schedulerContext.taskOrderRepository(),
    schedulerContext.queryTaskOrderUseCase(),
    schedulerContext.manageTaskOrderUseCase());
TaskTemplateApi taskTemplateApi = new TaskTemplateApi(
    schedulerContext.queryTaskTemplateUseCase(),
    schedulerContext.manageTaskTemplateUseCase());
AgentWhitelistAdminApi agentWhitelistAdminApi = new AgentWhitelistAdminApi(
    schedulerContext.agentWhitelistRepository());
TokenManagementApi tokenManagementApi = new TokenManagementApi(schedulerContext.tokenService());
DagManageApi dagManageApi = new DagManageApi(schedulerContext.instantiateDagTemplateUseCase());
WorkflowApi workflowApi = new WorkflowApi(
    schedulerContext.queryTaskTemplateUseCase(),
    schedulerContext.manageTaskTemplateUseCase(),
    schedulerContext.instantiateDagTemplateUseCase());
ExecutionApi executionApi = new ExecutionApi(
    schedulerContext.taskOrderRepository(), schedulerContext.taskRecordRepository());
AgentAdminApi agentAdminApi = new AgentAdminApi(
    schedulerContext.agentRepository(), schedulerContext.agentStatusRepository());
```

- [ ] **Step 5: Compile admin server**

Run:

```bash
mvn -f dag-task/pom.xml -pl dag-admin-muserver -am test -q
```

Expected: BUILD SUCCESS.

---

## Task 6: Cut over dag-allinone-muserver

**Files:**
- Modify: `dag-task/dag-allinone/pom.xml`
- Modify: `dag-task/dag-allinone-muserver/pom.xml`
- Modify: `dag-task/dag-allinone-muserver/src/main/java/top/ilovemyhome/dagtask/allinone/muserver/application/AllInOneAppContext.java`
- Modify: `dag-task/dag-allinone-muserver/src/main/java/top/ilovemyhome/dagtask/allinone/muserver/application/AllInOneWebServerBootstrap.java`

- [ ] **Step 1: Update allinone pom**

In `dag-allinone/pom.xml`, remove dependencies on:

```xml
<artifactId>dag-scheduler</artifactId>
<artifactId>dag-scheduler-muserver</artifactId>
```

Add dependencies on:

```xml
<artifactId>dag-scheduler-domain</artifactId>
<artifactId>dag-scheduler-adapter-persistence-jdbc</artifactId>
<artifactId>dag-scheduler-adapter-web-muserver</artifactId>
<artifactId>dag-scheduler-app</artifactId>
```

- [ ] **Step 2: Update dag-allinone-muserver pom**

Ensure `dag-allinone-muserver/pom.xml` has direct dependencies on:

```xml
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler-adapter-web-muserver</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler-app</artifactId>
        </dependency>
```

Do not add old `dag-scheduler` or `dag-scheduler-muserver`.

- [ ] **Step 3: Simplify AllInOneAppContext**

Remove imports for old core classes:

```java
import top.ilovemyhome.dagtask.core.DagSchedulerServer;
import top.ilovemyhome.dagtask.core.dispatcher.DefaultTaskDispatcher;
import top.ilovemyhome.dagtask.si.service.DagScheduleService;
```

Add:

```java
import top.ilovemyhome.dagtask.scheduler.app.SchedulerContext;
```

Replace use of `DagSchedulerServer` with getting `SchedulerContext` from admin app context:

```java
SchedulerContext schedulerContext = this.adminAppContext.getBean("schedulerContext", SchedulerContext.class);
this.scheduleDagRunUseCase = schedulerContext.scheduleDagRunUseCase();
this.inProcessSchedulerClient = new InProcessSchedulerClient(this.scheduleDagRunUseCase);
```

Replace `TaskDispatchDaoJdbiImpl taskDispatchDao = new TaskDispatchDaoJdbiImpl(sharedJdbi);` with reuse from context if the embedded dispatcher needs the same repository:

```java
TaskDispatchDaoJdbiImpl taskDispatchDao = schedulerContext.taskDispatchRepository();
```

If `SchedulerContext` does not expose `taskDispatchRepository()`, add that getter in Task 3 before this step.

Remove the warning block that says scheduler dispatch still uses old `DefaultTaskDispatcher`.

- [ ] **Step 4: Update AllInOneWebServerBootstrap imports and API construction**

Replace old API imports with web adapter imports:

```java
import top.ilovemyhome.dagtask.scheduler.adapter.web.muserver.AgentRegistryApi;
import top.ilovemyhome.dagtask.scheduler.adapter.web.muserver.DagManageApi;
import top.ilovemyhome.dagtask.scheduler.adapter.web.muserver.TaskOrderApi;
import top.ilovemyhome.dagtask.scheduler.adapter.web.muserver.TaskTemplateApi;
import top.ilovemyhome.dagtask.scheduler.app.SchedulerContext;
```

Replace `DagSchedulerServer schedulerServer = ...` with:

```java
SchedulerContext schedulerContext = adminAppContext.getBean("schedulerContext", SchedulerContext.class);
```

Construct scheduler APIs from `schedulerContext` exactly as in Task 5, plus:

```java
AgentRegistryApi agentRegistryApi = new AgentRegistryApi(
    schedulerContext.registerAgentUseCase(),
    schedulerContext.agentHeartbeatUseCase(),
    schedulerContext.reportTaskResultUseCase());
```

- [ ] **Step 5: Compile allinone server**

Run:

```bash
mvn -f dag-task/pom.xml -pl dag-allinone-muserver -am test -q
```

Expected: BUILD SUCCESS.

---

## Task 7: Remove legacy scheduler references from source before deletion

**Files:**
- Modify any file found by scans under production/test source, excluding docs.

- [ ] **Step 1: Scan production/test source for old scheduler classes**

Run:

```bash
rg "DagSchedulerServer|DagSchedulerBuilder|top\.ilovemyhome\.dagtask\.core\.|top\.ilovemyhome\.dagtask\.si\.service\." dag-task \
  --glob '*.java' \
  --glob '!dag-scheduler/**' \
  --glob '!dag-scheduler-muserver/**'
```

Expected: no matches outside old modules. Matches in docs are allowed and handled in Task 10.

- [ ] **Step 2: Scan poms for old artifacts**

Run:

```bash
rg "<artifactId>dag-scheduler</artifactId>|<artifactId>dag-scheduler-muserver</artifactId>" dag-task --glob 'pom.xml'
```

Expected before Task 8: only root `dag-task/pom.xml` dependencyManagement/module declarations and old modules' own poms. No entry module dependency may remain.

- [ ] **Step 3: Compile non-old entry modules without old modules selected**

Run:

```bash
mvn -f dag-task/pom.xml -pl dag-admin-muserver,dag-allinone-muserver -am test -q
```

Expected: BUILD SUCCESS.

---

## Task 8: Remove legacy modules from Maven and filesystem

**Files:**
- Modify: `dag-task/pom.xml`
- Delete: `dag-task/dag-scheduler/`
- Delete: `dag-task/dag-scheduler-muserver/`

- [ ] **Step 1: Remove root module declarations**

In `dag-task/pom.xml`, remove:

```xml
        <module>dag-scheduler</module>
        <module>dag-scheduler-muserver</module>
```

- [ ] **Step 2: Remove dependencyManagement entries**

In `dag-task/pom.xml`, remove dependencyManagement entries for:

```xml
<artifactId>dag-scheduler</artifactId>
<artifactId>dag-scheduler-muserver</artifactId>
```

- [ ] **Step 3: Delete old module directories**

Before deleting, verify `git status --short` shows only expected new/modified files. Then delete:

```bash
rm -rf dag-task/dag-scheduler dag-task/dag-scheduler-muserver
```

- [ ] **Step 4: Verify Maven no longer knows old modules**

Run:

```bash
mvn -f dag-task/pom.xml -pl dag-scheduler -am validate -q
```

Expected: Maven fails with a message that selected project `dag-scheduler` could not be found. This confirms the old module is removed from the reactor.

- [ ] **Step 5: Verify remaining modules compile**

Run:

```bash
mvn -f dag-task/pom.xml -pl '!dag-agent,!dag-agent-muserver,!dag-agent-cli' -am test -q
```

Expected: BUILD SUCCESS.

---

## Task 9: Full verification and reference scans

**Files:**
- No source changes expected unless verification exposes compile/test failures.

- [ ] **Step 1: Run non-agent clean verify**

Run:

```bash
mvn -f dag-task/pom.xml clean verify -pl '!dag-agent,!dag-agent-muserver,!dag-agent-cli'
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Run final Java source scan**

Run:

```bash
rg "DagSchedulerServer|DagSchedulerBuilder|top\.ilovemyhome\.dagtask\.core\.|top\.ilovemyhome\.dagtask\.si\.service\." dag-task --glob '*.java'
```

Expected: no matches.

- [ ] **Step 3: Run final pom scan**

Run:

```bash
rg "<artifactId>dag-scheduler</artifactId>|<artifactId>dag-scheduler-muserver</artifactId>" dag-task --glob 'pom.xml'
```

Expected: no matches.

- [ ] **Step 4: Optional HTTP verification**

If local allinone startup dependencies are available, run the allinone server and execute scheduler/admin `.http` files under `dag-task/api-test/`. If not available, record that HTTP verification was skipped and why.

---

## Task 10: Update documentation and final stage

**Files:**
- Modify: `dag-task/CLAUDE.md`
- Modify: `dag-task/docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`
- Modify: `dag-task/docs/superpowers/specs/2026-06-11-dag-scheduler-hexagonal-step4-cutover-design.md` only if implementation decisions changed materially.

- [ ] **Step 1: Update dag-task/CLAUDE.md**

Change the hexagonal refactor section table so:

```markdown
| `dag-scheduler-adapter-web-muserver` | zora-muserver 实现 port.in | 步骤 4 ✅ Web adapter migrated |
| `dag-scheduler-app` | 手工 DI 组装 + main/context | 步骤 4 ✅ Cutover completed |
```

Add a short paragraph in Chinese:

```markdown
截至 2026-06-11，dag-scheduler 六边形重构 4 个步骤已完成。旧 `dag-scheduler` / `dag-scheduler-muserver` 模块已退役并从 reactor 删除；运行入口通过 `dag-scheduler-app` 组装 domain、persistence adapter 与 web adapter。
```

- [ ] **Step 2: Update architecture doc status**

In `dag-task/docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`:

- Change status line to `状态：已完成（步骤 4 / 4 已完成）`.
- Change step 4 table row to `✅ 已完成`.
- Update final module structure to remove notes that web API classes remain in old scheduler.
- In summary, record:
  - old modules deleted
  - allinone/admin cutover completed
  - non-agent verify result
  - HTTP verification result or skipped reason

- [ ] **Step 3: Run final git status**

Run:

```bash
git status --short
```

Expected: changes include new/modified scheduler app/web adapter/admin/allinone files, deleted old modules, and docs.

- [ ] **Step 4: Stage changes only if all mandatory verification passed**

Run:

```bash
git add dag-task/
```

Do not commit. Project instructions require human commits.

- [ ] **Step 5: Report suggested commit split**

Prepare these suggested commit messages for the user:

```text
feat(scheduler-web): add muserver adapters for scheduler use cases

feat(scheduler-app): add scheduler context and HTTP agent dispatcher

refactor(admin): cut over admin server to scheduler hexagonal context

refactor(allinone): cut over allinone server to scheduler hexagonal context

refactor(scheduler): remove legacy scheduler modules

docs: mark dag-scheduler hexagonal refactor complete
```

---

## Self-Review Checklist

- Spec coverage:
  - Web adapter migration covered by Tasks 1-2.
  - Scheduler context and token/dispatcher migration covered by Tasks 3-4.
  - Admin and allinone cutover covered by Tasks 5-6.
  - Legacy reference scans and deletion covered by Tasks 7-8.
  - Verification and docs covered by Tasks 9-10.
- No placeholders: every task has concrete file paths, commands, and expected outcomes.
- Type consistency: plan uses current inbound ports: `ManageTaskTemplateUseCase`, `QueryTaskTemplateUseCase`, `InstantiateDagTemplateUseCase`, `RegisterAgentUseCase`, `AgentHeartbeatUseCase`, `ReportTaskResultUseCase`, `ScheduleDagRunUseCase`.
- Safety: deletion is gated behind source scans, pom scans, and compile checks.
