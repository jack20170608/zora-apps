# Agent Token Authentication with Auto-Registration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add token-based authentication for DAG agents with automated self-registration and approval workflow.

**Architecture:** This implementation extends the existing token authentication design with an automated distribution flow. Agents self-register on first startup, whitelisted agents are auto-approved, others require manual admin approval. After approval, server pushes token to agent via callback, agent persists token locally. Uses existing JWT/RSA infrastructure and follows existing code patterns.

**Tech Stack:** Java 25, Maven, JUnit 5, Mockito, Spring (assuming existing DI), Flyway for migrations, JWT with RSA signing.

---

## File Structure

### New Files to Create:

**In dag-si:**
- `top.ilovemyhome.dagtask.si.auth/AgentRegistrationRequest.java` - DTO for registration request
- `top.ilovemyhome.dagtask.si.auth/AgentRegistrationResponse.java` - DTO for registration response
- `top.ilovemyhome.dagtask.si.auth/AgentRegistrationInfo.java` - DTO for registration listing
- `top.ilovemyhome.dagtask.si.auth/TokenPushRequest.java` - DTO for token callback

**In dag-scheduler:**
- `top.ilovemyhome.dagtask.scheduler.auth/AgentRegistration.java` - Entity
- `top.ilovemyhome.dagtask.scheduler.auth/AgentRegistrationDao.java` - DAO interface
- `top.ilovemyhome.dagtask.scheduler.auth/JdbcAgentRegistrationDao.java` - JDBC implementation
- `top.ilovemyhome.dagtask.scheduler.auth/RegistrationService.java` - Core service
- `top.ilovemyhome.dagtask.scheduler.config/AutoApproveConfig.java` - Configuration

**In dag-scheduler-muserver:**
- `top.ilovemyhome.dagtask.schedulermuserver.api.PublicRegistrationApi.java` - Public registration endpoint
- `top.ilovemyhome.dagtask.schedulermuserver.api.RegistrationAdminApi.java` - Admin management endpoints
- `top.ilovemyhome.dagtask.schedulermuserver.scheduler.ExpiredRegistrationCleanupJob.java` - Scheduled cleanup

**In dag-agent:**
- `top.ilovmyhome.dagtask.agent.registration.AgentAutoRegistration.java` - Auto-registration coordinator
- `top.ilovmyhome.dagtask.agent.registration.LocalTokenStorage.java` - Local file token persistence

**In dag-agent-muserver:**
- `top.ilovemyhome.dagtask.agentmuserver.TokenCallbackEndpoint.java` - Callback endpoint to receive token

**Database migrations:**
- `src/main/resources/db/migration/VXX__add_agent_registrations_table.sql` - Flyway migration

### Existing Files to Modify:
- `dag-si/src/main/java/.../AgentConfiguration.java` - Add agentToken field
- `dag-agent/src/main/java/.../DefaultAgentSchedulerClient.java` - Add Authorization header
- `dag-scheduler-muserver/src/main/java/.../...FilterConfiguration.java` - Add AgentTokenAuthFilter
- `dag-agent-muserver/src/main/java/.../AgentServer.java` - Add callback endpoint registration

---

## Tasks

### Task 1: Add DTOs to dag-si

**Files:**
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/auth/AgentRegistrationRequest.java`
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/auth/AgentRegistrationResponse.java`
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/auth/AgentRegistrationInfo.java`
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/auth/TokenPushRequest.java`
- Test: `dag-task/dag-si/src/test/java/top/ilovemyhome/dagtask/si/auth/`

- [ ] **Step 1: Create AgentRegistrationRequest.java**

```java
package top.ilovemyhome.dagtask.si.auth;

import java.util.Map;

public record AgentRegistrationRequest(
    String name,
    String description,
    Map<String, String> labels,
    String callbackUrl
) {}
```

- [ ] **Step 2: Create AgentRegistrationResponse.java**

```java
package top.ilovemyhome.dagtask.si.auth;

public record AgentRegistrationResponse(
    boolean success,
    Data data,
    String message
) {
    public record Data(
        String registrationId,
        String status,
        String message
    ) {}
}
```

- [ ] **Step 3: Create AgentRegistrationInfo.java**

```java
package top.ilovemyhome.dagtask.si.auth;

import java.time.Instant;

public record AgentRegistrationInfo(
    String registrationId,
    String agentName,
    String description,
    String status,
    String clientAddress,
    Instant createdAt,
    Instant expiresAt,
    String processedBy,
    Instant processedAt,
    String notes
) {}
```

- [ ] **Step 4: Create TokenPushRequest.java**

```java
package top.ilovemyhome.dagtask.si.auth;

import java.time.Instant;

public record TokenPushRequest(
    String registrationId,
    String token,
    String tokenId,
    Instant expiresAt,
    String name
) {}
```

- [ ] **Step 5: Compile and verify**

```bash
cd dag-task/dag-si
mvn compile
```

Expected: Compiles successfully.

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: add DTOs for agent auto-registration"
```

### Task 2: Add database migration for agent_registrations table

**Files:**
- Create: `dag-task/dag-scheduler/src/main/resources/db/migration/V2__create_agent_registrations_table.sql`
- (Assumes V1 is already created for agent_tokens table)

- [ ] **Step 1: Create migration file**

```sql
CREATE TABLE agent_registrations (
    id BIGSERIAL PRIMARY KEY,
    registration_id VARCHAR(64) NOT NULL UNIQUE,
    agent_name VARCHAR(255) NOT NULL,
    description TEXT,
    labels JSONB,
    callback_url TEXT NOT NULL,
    nonce VARCHAR(64) NOT NULL,
    client_address VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    processed_by VARCHAR(100),
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_agent_registrations_status ON agent_registrations(status);
CREATE INDEX idx_agent_registrations_expires_at ON agent_registrations(expires_at);
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: add database migration for agent_registrations table"
```

### Task 3: Add AgentRegistration Entity and DAO

**Files:**
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/auth/AgentRegistration.java`
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/auth/AgentRegistrationDao.java`
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/auth/JdbcAgentRegistrationDao.java`
- Test: `dag-task/dag-scheduler/src/test/java/top/ilovemyhome/dagtask/scheduler/auth/JdbcAgentRegistrationDaoTest.java`

- [ ] **Step 1: Create AgentRegistration entity**

```java
package top.ilovemyhome.dagtask.scheduler.auth;

import java.time.Instant;

public record AgentRegistration(
    Long id,
    String registrationId,
    String agentName,
    String description,
    String labelsJson,
    String callbackUrl,
    String nonce,
    String clientAddress,
    Status status,
    String notes,
    String processedBy,
    Instant processedAt,
    Instant createdAt,
    Instant expiresAt
) {
    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }
}
```

- [ ] **Step 2: Create AgentRegistrationDao interface**

```java
package top.ilovemyhome.dagtask.scheduler.auth;

import java.util.List;
import java.util.Optional;

public interface AgentRegistrationDao {

    AgentRegistration insert(AgentRegistration registration);

    Optional<AgentRegistration> findByRegistrationId(String registrationId);

    List<AgentRegistration> findByStatus(AgentRegistration.Status status, int limit);

    List<AgentRegistration> findExpiredPending(Instant now, int limit);

    int deleteExpiredPending(Instant now);

    void updateStatus(String registrationId, AgentRegistration.Status status, String processedBy, String notes);
}
```

- [ ] **Step 3: Write failing DAO test**

```java
package top.ilovemyhome.dagtask.scheduler.auth;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class JdbcAgentRegistrationDaoTest {

    @Test
    void shouldInsertAndFindById() {
        // Given
        JdbcTemplate jdbcTemplate = null; // Configure test DB
        JdbcAgentRegistrationDao dao = new JdbcAgentRegistrationDao(jdbcTemplate);
        // When - Then: implement in next step
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

- [ ] **Step 5: Implement JdbcAgentRegistrationDao**

```java
package top.ilovemyhome.dagtask.scheduler.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcAgentRegistrationDao implements AgentRegistrationDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AgentRegistration> rowMapper = (rs, rowNum) -> mapRow(rs);

    public JdbcAgentRegistrationDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private AgentRegistration mapRow(ResultSet rs) throws SQLException {
        return new AgentRegistration(
            rs.getLong("id"),
            rs.getString("registration_id"),
            rs.getString("agent_name"),
            rs.getString("description"),
            rs.getString("labels"),
            rs.getString("callback_url"),
            rs.getString("nonce"),
            rs.getString("client_address"),
            AgentRegistration.Status.valueOf(rs.getString("status")),
            rs.getString("notes"),
            rs.getString("processed_by"),
            rs.getTimestamp("processed_at") != null
                ? rs.getTimestamp("processed_at").toInstant()
                : null,
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("expires_at").toInstant()
        );
    }

    @Override
    public AgentRegistration insert(AgentRegistration registration) {
        String sql = """
            INSERT INTO agent_registrations (
                registration_id, agent_name, description, labels, callback_url,
                nonce, client_address, status, notes, processed_by, processed_at,
                created_at, expires_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
            registration.registrationId(),
            registration.agentName(),
            registration.description(),
            registration.labelsJson(),
            registration.callbackUrl(),
            registration.nonce(),
            registration.clientAddress(),
            registration.status().name(),
            registration.notes(),
            registration.processedBy(),
            registration.processedAt() != null
                ? java.sql.Timestamp.from(registration.processedAt())
                : null,
            java.sql.Timestamp.from(registration.createdAt()),
            java.sql.Timestamp.from(registration.expiresAt())
        );
        return registration;
    }

    @Override
    public Optional<AgentRegistration> findByRegistrationId(String registrationId) {
        List<AgentRegistration> results = jdbcTemplate.query(
            "SELECT * FROM agent_registrations WHERE registration_id = ?",
            rowMapper,
            registrationId
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<AgentRegistration> findByStatus(AgentRegistration.Status status, int limit) {
        return jdbcTemplate.query(
            "SELECT * FROM agent_registrations WHERE status = ? ORDER BY created_at DESC LIMIT ?",
            rowMapper,
            status.name(),
            limit
        );
    }

    @Override
    public List<AgentRegistration> findExpiredPending(Instant now, int limit) {
        return jdbcTemplate.query(
            "SELECT * FROM agent_registrations WHERE status = 'PENDING' AND expires_at < ? ORDER BY created_at LIMIT ?",
            rowMapper,
            java.sql.Timestamp.from(now),
            limit
        );
    }

    @Override
    public int deleteExpiredPending(Instant now) {
        return jdbcTemplate.update(
            "DELETE FROM agent_registrations WHERE status = 'PENDING' AND expires_at < ?",
            java.sql.Timestamp.from(now)
        );
    }

    @Override
    public void updateStatus(String registrationId, AgentRegistration.Status status,
                            String processedBy, String notes) {
        jdbcTemplate.update(
            "UPDATE agent_registrations SET status = ?, processed_by = ?, processed_at = NOW(), notes = ? WHERE registration_id = ?",
            status.name(),
            processedBy,
            notes,
            registrationId
        );
    }
}
```

- [ ] **Step 6: Complete and run tests**

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: add AgentRegistration entity and DAO"
```

### Task 4: Add AutoApprove configuration

**Files:**
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/config/AutoApproveConfig.java`
- Test: `dag-task/dag-scheduler/src/test/java/top/ilovemyhome/dagtask/scheduler/config/AutoApproveConfigTest.java`

- [ ] **Step 1: Create configuration class with pattern matching**

```java
package top.ilovemyhome.dagtask.scheduler.config;

import java.util.List;

public record AutoApproveConfig(
    boolean enabled,
    List<String> patterns
) {
    public boolean isMatch(String agentName) {
        if (!enabled || patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (matches(agentName, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String input, String pattern) {
        if (pattern.equals(input)) {
            return true;
        }
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        return input.matches(regex);
    }
}
```

- [ ] **Step 2: Write tests**

```java
package top.ilovemyhome.dagtask.scheduler.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AutoApproveConfigTest {

    @Test
    void shouldMatchExact() {
        var config = new AutoApproveConfig(true, List.of("dev-agent-01"));
        assertThat(config.isMatch("dev-agent-01")).isTrue();
        assertThat(config.isMatch("dev-agent-02")).isFalse();
    }

    @Test
    void shouldMatchPrefix() {
        var config = new AutoApproveConfig(true, List.of("prod-*"));
        assertThat(config.isMatch("prod-")).isTrue();
        assertThat(config.isMatch("prod-worker")).isTrue();
        assertThat(config.isMatch("prod-123")).isTrue();
        assertThat(config.isMatch("pr")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenDisabled() {
        var config = new AutoApproveConfig(false, List.of("prod-*"));
        assertThat(config.isMatch("prod-worker")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenNoPatterns() {
        var config = new AutoApproveConfig(true, null);
        assertThat(config.isMatch("prod-worker")).isFalse();
    }
}
```

- [ ] **Step 3: Run tests**

```bash
cd dag-task/dag-scheduler
mvn test -Dtest=AutoApproveConfigTest
```

Expected: All tests pass.

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add AutoApproveConfig with pattern matching"
```

### Task 5: Add RegistrationService core service

**Files:**
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/auth/RegistrationService.java`
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/auth/TokenPusher.java`
- Test: `dag-task/dag-scheduler/src/test/java/top/ilovemyhome/dagtask/scheduler/auth/RegistrationServiceTest.java`

- [ ] **Step 1: Create TokenPusher interface**

```java
package top.ilovemyhome.dagtask.scheduler.auth;

import top.ilovemyhome.dagtask.si.auth.TokenPushRequest;

public interface TokenPusher {
    boolean pushToken(String callbackUrl, String nonce, TokenPushRequest request);
}
```

- [ ] **Step 2: Create RegistrationService**

```java
package top.ilovemyhome.dagtask.scheduler.auth;

import top.ilovemyhome.dagtask.si.auth.AgentRegistrationRequest;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationResponse;
import top.ilovemyhome.dagtask.si.auth.TokenPushRequest;
import top.ilovemyhome.dagtask.scheduler.config.AutoApproveConfig;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class RegistrationService {

    private final AgentRegistrationDao registrationDao;
    private final TokenService tokenService;
    private final TokenPusher tokenPusher;
    private final AutoApproveConfig autoApproveConfig;
    private final SecureRandom random = new SecureRandom();

    public RegistrationService(AgentRegistrationDao registrationDao,
                              TokenService tokenService,
                              TokenPusher tokenPusher,
                              AutoApproveConfig autoApproveConfig) {
        this.registrationDao = registrationDao;
        this.tokenService = tokenService;
        this.tokenPusher = tokenPusher;
        this.autoApproveConfig = autoApproveConfig;
    }

    private String generateId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public AgentRegistrationResponse createRegistration(AgentRegistrationRequest request, String clientAddress) {
        String registrationId = generateId();
        String nonce = generateId();

        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(24, ChronoUnit.HOURS);

        boolean autoApprove = autoApproveConfig.isMatch(request.name());
        AgentRegistration.Status status = autoApprove
            ? AgentRegistration.Status.APPROVED
            : AgentRegistration.Status.PENDING;

        String labelsJson = null;
        if (request.labels() != null && !request.labels().isEmpty()) {
            // Convert map to JSON - use whatever JSON library is available
            labelsJson = serializeLabels(request.labels());
        }

        AgentRegistration registration = new AgentRegistration(
            null,
            registrationId,
            request.name(),
            request.description(),
            labelsJson,
            request.callbackUrl(),
            nonce,
            clientAddress,
            status,
            null,
            null,
            null,
            createdAt,
            expiresAt
        );

        registrationDao.insert(registration);

        if (autoApprove) {
            approve(registrationId, "system", null);
            return new AgentRegistrationResponse(true, new AgentRegistrationResponse.Data(
                registrationId,
                "APPROVED",
                "Registration auto-approved via whitelist"
            ), null);
        }

        return new AgentRegistrationResponse(true, new AgentRegistrationResponse.Data(
            registrationId,
            "PENDING",
            "Registration submitted, waiting for admin approval"
        ), null);
    }

    public Optional<AgentRegistration> getRegistration(String registrationId) {
        return registrationDao.findByRegistrationId(registrationId);
    }

    public List<AgentRegistration> listByStatus(AgentRegistration.Status status, int limit) {
        return registrationDao.findByStatus(status, limit);
    }

    public void approve(String registrationId, String processedBy, String notes) {
        Optional<AgentRegistration> optReg = registrationDao.findByRegistrationId(registrationId);
        if (optReg.isEmpty()) {
            return;
        }
        AgentRegistration reg = optReg.get();
        if (reg.status() != AgentRegistration.Status.PENDING && reg.status() != AgentRegistration.Status.APPROVED) {
            return;
        }

        // Generate token
        var tokenResult = tokenService.generateToken(reg.agentName(), 365, processedBy);
        String jwt = tokenService.generateJwt(tokenResult);

        // Push token to callback
        TokenPushRequest pushRequest = new TokenPushRequest(
            registrationId,
            jwt,
            tokenResult.tokenId(),
            tokenResult.expiresAt(),
            reg.agentName()
        );

        boolean pushed = tokenPusher.pushToken(reg.callbackUrl(), reg.nonce(), pushRequest);

        if (pushed) {
            registrationDao.updateStatus(registrationId, AgentRegistration.Status.APPROVED, processedBy, notes);
        }
    }

    public void reject(String registrationId, String processedBy, String notes) {
        registrationDao.updateStatus(registrationId, AgentRegistration.Status.REJECTED, processedBy, notes);
    }

    public int cleanupExpired(Instant now) {
        return registrationDao.deleteExpiredPending(now);
    }

    private String serializeLabels(java.util.Map<String, String> labels) {
        // Implementation depends on existing JSON library in project
        // Use Jackson ObjectMapper if available
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(labels);
        } catch (Exception e) {
            return null;
        }
    }
}
```

- [ ] **Step 3: Implement DefaultTokenPusher**

```java
package top.ilovemyhome.dagtask.scheduler.auth;

import top.ilovemyhome.dagtask.si.auth.TokenPushRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DefaultTokenPusher implements TokenPusher {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DefaultTokenPusher() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean pushToken(String callbackUrl, String nonce, TokenPushRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(callbackUrl))
                .header("Content-Type", "application/json")
                .header("X-Registration-Nonce", nonce)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(
                httpRequest, HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 4: Write tests and run**

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add RegistrationService and TokenPusher"
```

### Task 6: Add PublicRegistrationApi endpoint

**Files:**
- Create: `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/schedulermuserver/api/PublicRegistrationApi.java`
- Test: `dag-task/dag-scheduler-muserver/src/test/java/.../PublicRegistrationApiTest.java`

- [ ] **Step 1: Create PublicRegistrationApi**

```java
package top.ilovemyhome.dagtask.schedulermuserver.api;

import top.ilovemyhome.dagtask.si.auth.AgentRegistrationRequest;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationResponse;
import top.ilovemyhome.dagtask.scheduler.auth.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent")
public class PublicRegistrationApi {

    private final RegistrationService registrationService;

    public PublicRegistrationApi(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public AgentRegistrationResponse register(
            @RequestBody AgentRegistrationRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientAddress = getClientIp(httpRequest);
        return registrationService.createRegistration(request, clientAddress);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
```

- [ ] **Step 2: Add Spring bean configuration for RegistrationService**

- [ ] **Step 3: Run tests**

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add PublicRegistrationApi public endpoint"
```

### Task 7: Add RegistrationAdminApi endpoints

**Files:**
- Create: `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/schedulermuserver/api/RegistrationAdminApi.java`

- [ ] **Step 1: Create RegistrationAdminApi**

```java
package top.ilovemyhome.dagtask.schedulermuserver.api;

import top.ilovemyhome.dagtask.si.auth.AgentRegistrationInfo;
import top.ilovemyhome.dagtask.scheduler.auth.AgentRegistration;
import top.ilovemyhome.dagtask.scheduler.auth.RegistrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/registrations")
public class RegistrationAdminApi {

    private final RegistrationService registrationService;

    public RegistrationAdminApi(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public List<AgentRegistrationInfo> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        AgentRegistration.Status filterStatus = status != null
            ? AgentRegistration.Status.valueOf(status.toUpperCase())
            : AgentRegistration.Status.PENDING;
        List<AgentRegistration> registrations = registrationService.listByStatus(filterStatus, limit);
        return registrations.stream()
            .map(r -> new AgentRegistrationInfo(
                r.registrationId(),
                r.agentName(),
                r.description(),
                r.status().name(),
                r.clientAddress(),
                r.createdAt(),
                r.expiresAt(),
                r.processedBy(),
                r.processedAt(),
                r.notes()
            ))
            .toList();
    }

    @PostMapping("/{registrationId}/approve")
    public void approve(
            @PathVariable String registrationId,
            @RequestParam(name = "notes", required = false) String notes,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "admin";
        registrationService.approve(registrationId, username, notes);
    }

    @PostMapping("/{registrationId}/reject")
    public void reject(
            @PathVariable String registrationId,
            @RequestParam(name = "notes", required = false) String notes,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "admin";
        registrationService.reject(registrationId, username, notes);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: add RegistrationAdminApi admin endpoints"
```

### Task 8: Add scheduled cleanup job for expired registrations

**Files:**
- Create: `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/schedulermuserver/job/ExpiredRegistrationCleanupJob.java`

- [ ] **Step 1: Create cleanup job**

```java
package top.ilovemyhome.dagtask.schedulermuserver.job;

import top.ilovemyhome.dagtask.scheduler.auth.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;

public class ExpiredRegistrationCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiredRegistrationCleanupJob.class);

    private final RegistrationService registrationService;

    public ExpiredRegistrationCleanupJob(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    public void cleanupExpired() {
        LOGGER.info("Starting expired pending registration cleanup...");
        int deleted = registrationService.cleanupExpired(Instant.now());
        LOGGER.info("Cleanup completed. Deleted {} expired pending registrations.", deleted);
    }
}
```

- [ ] **Step 2: Enable scheduling if not already enabled**

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add scheduled cleanup job for expired registrations"
```

### Task 9: Add token DTOs and AgentToken table to original token design

> **Note:** This completes the original token authentication design that was in the spec

**Files:**
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/auth/GenerateTokenRequest.java`
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/auth/GenerateTokenResponse.java`
- Create: `dag-task/dag-si/src/main/java/top/ilovemyhome/dagtask/si/auth/TokenInfo.java`
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/token/AgentToken.java`
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/token/AgentTokenDao.java`
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/token/JdbcAgentTokenDao.java`
- Create: `dag-task/dag-scheduler/src/main/java/top/ilovemyhome/dagtask/scheduler/token/TokenService.java`

- [ ] **Step 1: Create DTOs**

- [ ] **Step 2: Create AgentToken entity and DAO**

- [ ] **Step 3: Create TokenService with generate/validate/revoke/list**

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add TokenService and AgentToken DAO"
```

### Task 10: Add TokenManagementApi endpoints

**Files:**
- Create: `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/schedulermuserver/api/TokenManagementApi.java`

- [ ] **Step 1: Create TokenManagementApi with generate/list/revoke endpoints**

```java
package top.ilovemyhome.dagtask.schedulermuserver.api;

import top.ilovemyhome.dagtask.si.auth.GenerateTokenRequest;
import top.ilovemyhome.dagtask.si.auth.GenerateTokenResponse;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tokens")
public class TokenManagementApi {

    private final TokenService tokenService;

    public TokenManagementApi(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/generate")
    public GenerateTokenResponse generate(
            @RequestBody GenerateTokenRequest request,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "admin";
        var result = tokenService.generateToken(request.name(), request.description(),
            request.expiresInDays(), username);
        String jwt = tokenService.generateJwt(result);
        return new GenerateTokenResponse(true, new GenerateTokenResponse.Data(
            jwt,
            result.tokenId(),
            result.expiresAt(),
            result.name()
        ), null);
    }

    @GetMapping
    public List<TokenInfo> listTokens() {
        return tokenService.listTokens();
    }

    @PostMapping("/{tokenId}/revoke")
    public void revoke(
            @PathVariable String tokenId,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "admin";
        tokenService.revokeToken(tokenId, username);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: add TokenManagementApi admin endpoints"
```

### Task 11: Add AgentTokenAuthFilter

**Files:**
- Create: `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/schedulermuserver/filter/AgentTokenAuthFilter.java`

- [ ] **Step 1: Create AgentTokenAuthFilter**

```java
package top.ilovemyhome.dagtask.schedulermuserver.filter;

import top.ilovemyhome.dagtask.scheduler.token.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AgentTokenAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public AgentTokenAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = authorization.substring(7);
        if (!tokenService.validateToken(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or revoked token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Register filter for agent endpoints**

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add AgentTokenAuthFilter for agent endpoint authentication"
```

### Task 12: Update AgentConfiguration in dag-agent

**Files:**
- Modify: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/config/AgentConfiguration.java`

- [ ] **Step 1: Add agentToken field**

```java
// Add this field to existing AgentConfiguration:
private String agentToken;

// Add getter and setter:
public String getAgentToken() {
    return agentToken;
}

public void setAgentToken(String agentToken) {
    this.agentToken = agentToken;
}

public boolean hasToken() {
    return agentToken != null && !agentToken.isBlank();
}
```

- [ ] **Step 2: Compile and commit**

```bash
cd dag-task/dag-agent
mvn compile
git add .
git commit -m "feat: add agentToken field to AgentConfiguration"
```

### Task 13: Update DefaultAgentSchedulerClient to send Authorization header

**Files:**
- Modify: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/client/DefaultAgentSchedulerClient.java`

- [ ] **Step 1: Add Authorization header when token is present**

```java
// In the method that creates requests:
if (configuration.hasToken()) {
    httpRequestBuilder.header("Authorization", "Bearer " + configuration.getAgentToken());
}
```

- [ ] **Step 2: Compile, test, commit**

```bash
git add .
git commit -m "feat: add Authorization header to agent client requests"
```

### Task 14: Add LocalTokenStorage to dag-agent

**Files:**
- Create: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/registration/LocalTokenStorage.java`
- Test: `dag-task/dag-agent/src/test/java/.../LocalTokenStorageTest.java`

- [ ] **Step 1: Create LocalTokenStorage**

```java
package top.ilovemyhome.dagtask.agent.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public class LocalTokenStorage {

    private final Path configFilePath;
    private final ObjectMapper objectMapper;

    public LocalTokenStorage(Path configFilePath) {
        this.configFilePath = configFilePath;
        this.objectMapper = new ObjectMapper();
    }

    public Optional<String> loadToken() {
        if (!Files.exists(configFilePath)) {
            return Optional.empty();
        }
        try {
            StoredToken stored = objectMapper.readValue(configFilePath.toFile(), StoredToken.class);
            return Optional.ofNullable(stored.token());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void saveToken(String token) throws IOException {
        StoredToken stored = new StoredToken(token, java.time.Instant.now());
        Files.createDirectories(configFilePath.getParent());
        objectMapper.writeValue(
            Files.newOutputStream(configFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
            stored
        );
    }

    private record StoredToken(String token, java.time.Instant savedAt) {}
}
```

- [ ] **Step 2: Write tests**

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add LocalTokenStorage for local token persistence"
```

### Task 15: Add AgentAutoRegistration coordinator

**Files:**
- Create: `dag-task/dag-agent/src/main/java/top/ilovemyhome/dagtask/agent/registration/AgentAutoRegistration.java`
- Test: `dag-task/dag-agent/src/test/java/.../AgentAutoRegistrationTest.java`

- [ ] **Step 1: Create AgentAutoRegistration**

```java
package top.ilovemyhome.dagtask.agent.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationRequest;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentAutoRegistration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentAutoRegistration.class);

    private final AgentConfiguration configuration;
    private final LocalTokenStorage tokenStorage;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String callbackBaseUrl;

    public AgentAutoRegistration(AgentConfiguration configuration,
                                LocalTokenStorage tokenStorage,
                                String callbackBaseUrl) {
        this.configuration = configuration;
        this.tokenStorage = tokenStorage;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.callbackBaseUrl = callbackBaseUrl;
    }

    public Optional<String> checkAndAutoRegister() throws InterruptedException {
        // Check if token already in configuration
        if (configuration.hasToken()) {
            LOGGER.info("Agent token already configured, skipping auto-registration");
            return Optional.of(configuration.getAgentToken());
        }

        // Check if token saved locally
        Optional<String> localToken = tokenStorage.loadToken();
        if (localToken.isPresent()) {
            LOGGER.info("Loaded token from local storage");
            configuration.setAgentToken(localToken.get());
            return localToken;
        }

        LOGGER.info("No token found, starting auto-registration...");

        try {
            return initiateRegistration();
        } catch (IOException e) {
            LOGGER.error("Auto-registration failed", e);
            return Optional.empty();
        }
    }

    private Optional<String> initiateRegistration() throws IOException, InterruptedException {
        String callbackUrl = callbackBaseUrl + "/callback/token";

        AgentRegistrationRequest request = new AgentRegistrationRequest(
            configuration.getAgentName(),
            configuration.getDescription(),
            configuration.getLabels(),
            callbackUrl
        );

        String body = objectMapper.writeValueAsString(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(configuration.getSchedulerUrl() + "/api/v1/agent/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(
            httpRequest, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            AgentRegistrationResponse regResponse = objectMapper.readValue(
                response.body(), AgentRegistrationResponse.class
            );
            LOGGER.info("Registration submitted. Status: {}", regResponse.data().status());

            if (regResponse.data().status().equals("APPROVED")) {
                // Auto-approved, token will be pushed to callback
                LOGGER.info("Registration auto-approved, waiting for token push...");
            } else {
                LOGGER.info("Registration pending admin approval. Please wait for approval.");
            }

            // Token will come via callback - the callback endpoint will save it
            return Optional.empty();
        }

        LOGGER.error("Registration failed with status: {}", response.statusCode());
        return Optional.empty();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: add AgentAutoRegistration coordinator"
```

### Task 16: Add TokenCallbackEndpoint to dag-agent-muserver

**Files:**
- Create: `dag-task/dag-agent-muserver/src/main/java/top/ilovemyhome/dagtask/agentmuserver/callback/TokenCallbackEndpoint.java`
- Modify: `dag-task/dag-agent-muserver/src/main/java/.../AgentMuServer.java`

- [ ] **Step 1: Create TokenCallbackEndpoint**

```java
package top.ilovemyhome.dagtask.agentmuserver.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.agent.registration.LocalTokenStorage;
import top.ilovemyhome.dagtask.si.auth.TokenPushRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class TokenCallbackEndpoint extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenCallbackEndpoint.class);

    private final AgentConfiguration configuration;
    private final LocalTokenStorage tokenStorage;
    private final ObjectMapper objectMapper;
    private final String expectedNonce; // Set when registration starts

    public TokenCallbackEndpoint(AgentConfiguration configuration,
                               LocalTokenStorage tokenStorage,
                               String expectedNonce) {
        this.configuration = configuration;
        this.tokenStorage = tokenStorage;
        this.objectMapper = new ObjectMapper();
        this.expectedNonce = expectedNonce;
    }

    @Override
    protected ModelAndView handleRequestInternal(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {
        String nonce = request.getHeader("X-Registration-Nonce");

        if (nonce == null || !nonce.equals(expectedNonce)) {
            LOGGER.warn("Rejected token callback: invalid nonce. Expected: {}, Got: {}",
                expectedNonce, nonce);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        TokenPushRequest pushRequest = objectMapper.readValue(
            request.getInputStream(), TokenPushRequest.class
        );

        // Save token to local storage
        tokenStorage.saveToken(pushRequest.token());

        // Update configuration
        configuration.setAgentToken(pushRequest.token());

        LOGGER.info("Received and saved token from server. TokenId: {}", pushRequest.tokenId());

        response.setContentType("application/json");
        response.getWriter().write("{\"success\":true,\"message\":\"Token saved successfully\"}");
        return null;
    }
}
```

- [ ] **Step 2: Add endpoint to AgentMuServer**

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add TokenCallbackEndpoint to receive pushed token"
```

### Task 17: Update Flyway migration location and verify migrations

**Files:**
- Verify both migrations are present:
  - `V1__create_agent_tokens_table.sql`
  - `V2__create_agent_registrations_table.sql`

- [ ] **Step 1: Create V1 migration for agent_tokens**

```sql
CREATE TABLE agent_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by VARCHAR(100)
);

CREATE INDEX idx_agent_tokens_token_id ON agent_tokens(token_id);
CREATE INDEX idx_agent_tokens_revoked ON agent_tokens(revoked);
```

- [ ] **Step 2: Verify migrations order**

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add Flyway migrations for agent_tokens and agent_registrations tables"
```

### Task 18: Build entire project and run all tests

- [ ] **Step 1: Build project**

```bash
cd dag-task
mvn clean compile
```

- [ ] **Step 2: Run all tests**

```bash
mvn test
```

- [ ] **Step 3: Fix any compilation errors**

- [ ] **Step 4: Final commit**

---
