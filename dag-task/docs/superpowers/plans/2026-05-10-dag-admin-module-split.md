# DAG Admin Module Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split Admin management responsibilities from `dag-scheduler-muserver` into new `dag-admin` and `dag-admin-muserver` modules, keeping `dag-scheduler-muserver` as Agent-only communication port.

**Architecture:** New `dag-admin` module holds Admin REST API classes. New `dag-admin-muserver` module provides standalone MuServer for Admin with Cookie JWT auth. `dag-scheduler-muserver` is stripped of Admin APIs. `dag-scheduler-web` is renamed to `dag-admin-web`.

**Tech Stack:** JDK 25, Maven, MuServer, Jdbi, zora framework, JWT (RS256), PostgreSQL.

---

## File Structure

### New: dag-admin (library module)
```
dag-task/dag-admin/
├── pom.xml
├── metadata/metadata.json
├── README.md
└── src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/
    ├── AgentAdminApi.java          (migrated from dag-scheduler-muserver)
    ├── AgentWhitelistAdminApi.java (migrated from dag-scheduler-muserver)
    ├── ExecutionApi.java           (migrated from dag-scheduler-muserver)
    ├── StatsApi.java               (migrated from dag-scheduler-muserver)
    └── WorkflowApi.java            (migrated from dag-scheduler-muserver)
```

### New: dag-admin-muserver (runnable module)
```
dag-task/dag-admin-muserver/
├── pom.xml
├── metadata/metadata.json
├── README.md
└── src/
    ├── main/
    │   ├── java/top/ilovemyhome/dagtask/admin/server/
    │   │   ├── App.java
    │   │   ├── application/
    │   │   │   ├── AppContext.java     (copied from dag-scheduler-muserver)
    │   │   │   └── WebServerBootstrap.java (copied from dag-scheduler-muserver)
    │   │   └── web/
    │   │       ├── LoginHandler.java            (migrated from dag-scheduler-muserver)
    │   │       └── security/
    │   │           ├── SecurityHandler.java     (migrated from dag-scheduler-muserver)
    │   │           └── JwtHelper.java           (migrated from dag-scheduler-muserver)
    │   └── resources/config/
    │       └── application.conf
    └── test/resources/config/
        └── application-local.conf
```

### Modified: dag-scheduler-muserver
- `WebServerBootstrap.java` — Remove Admin API registrations, keep only `AgentRegistryApi`

### Renamed: dag-scheduler-web -> dag-admin-web
- `pom.xml` — Change `artifactId` and `name`

### Modified: dag-task/pom.xml
- Add `dag-admin` and `dag-admin-muserver` to `<modules>`
- Add `dag-admin` and `dag-admin-muserver` to `<dependencyManagement>`
- Rename `dag-scheduler-web` to `dag-admin-web` in `<modules>`

---

## Task 1: Create dag-admin Module Structure

**Files:**
- Create: `dag-task/dag-admin/pom.xml`
- Create: `dag-task/dag-admin/metadata/metadata.json`
- Create: `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/.gitkeep`
- Create: `dag-task/dag-admin/src/test/resources/.gitkeep`

- [ ] **Step 1: Create directory structure**

Run:
```bash
mkdir -p dag-task/dag-admin/{metadata,src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api,src/test/resources}
```

- [ ] **Step 2: Write pom.xml**

Create `dag-task/dag-admin/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>top.ilovemyhome.dagtask</groupId>
        <artifactId>dag-task</artifactId>
        <version>${revision}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <modelVersion>4.0.0</modelVersion>
    <artifactId>dag-admin</artifactId>
    <name>dag-admin - DAG Task Admin API</name>
    <description>Admin management APIs for DAG task scheduling system, including workflow, execution, agent and statistics management</description>

    <dependencies>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-si</artifactId>
        </dependency>

        <!-- SLF4J -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>metadata</directory>
                <filtering>true</filtering>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
        <testResources>
            <testResource>
                <directory>src/test/resources</directory>
            </testResource>
        </testResources>
    </build>
</project>
```

- [ ] **Step 3: Write metadata.json**

Create `dag-task/dag-admin/metadata/metadata.json`:

```json
{
  "groupId": "@project.groupId@",
  "artifactId": "@project.artifactId@",
  "description": "@project.description@",
  "version": "@project.version@",
  "scmUrl": "@project.scmUrl@"
}
```

- [ ] **Step 4: Commit**

```bash
git add dag-task/dag-admin/
git commit -m "feat: create dag-admin module structure"
```

---

## Task 2: Migrate Admin API Classes to dag-admin

**Files:**
- Create: `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/WorkflowApi.java`
- Create: `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/ExecutionApi.java`
- Create: `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/StatsApi.java`
- Create: `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/AgentAdminApi.java`
- Create: `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/AgentWhitelistAdminApi.java`

- [ ] **Step 1: Migrate WorkflowApi**

Copy `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/interfaces/api/WorkflowApi.java` to `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/WorkflowApi.java`.

Change the package declaration from:
```java
package top.ilovemyhome.dagtask.server.interfaces.api;
```
to:
```java
package top.ilovemyhome.dagtask.admin.interfaces.api;
```

All other imports remain the same (they reference `dag-si` classes).

- [ ] **Step 2: Migrate ExecutionApi**

Copy `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/interfaces/api/ExecutionApi.java` to `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/ExecutionApi.java`.

Change package to `top.ilovemyhome.dagtask.admin.interfaces.api`.

- [ ] **Step 3: Migrate StatsApi**

Copy `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/interfaces/api/StatsApi.java` to `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/StatsApi.java`.

Change package to `top.ilovemyhome.dagtask.admin.interfaces.api`.

- [ ] **Step 4: Migrate AgentAdminApi**

Copy `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/interfaces/api/AgentAdminApi.java` to `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/AgentAdminApi.java`.

Change package to `top.ilovemyhome.dagtask.admin.interfaces.api`.

- [ ] **Step 5: Migrate AgentWhitelistAdminApi**

Copy `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/interfaces/api/AgentWhitelistAdminApi.java` to `dag-task/dag-admin/src/main/java/top/ilovemyhome/dagtask/admin/interfaces/api/AgentWhitelistAdminApi.java`.

Change package to `top.ilovemyhome.dagtask.admin.interfaces.api`.

- [ ] **Step 6: Verify dag-admin compiles**

Run:
```bash
cd dag-task/dag-admin && mvn compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add dag-task/dag-admin/src/main/java/
git commit -m "feat: migrate admin api classes to dag-admin module"
```

---

## Task 3: Create dag-admin-muserver Module Structure

**Files:**
- Create: `dag-task/dag-admin-muserver/pom.xml`
- Create: `dag-task/dag-admin-muserver/metadata/metadata.json`
- Create: `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/App.java`
- Create: `dag-task/dag-admin-muserver/src/main/resources/config/application.conf`
- Create: `dag-task/dag-admin-muserver/src/test/resources/config/application-local.conf`

- [ ] **Step 1: Create directory structure**

Run:
```bash
mkdir -p dag-task/dag-admin-muserver/{metadata,src/main/java/top/ilovemyhome/dagtask/admin/server/{application,web/security},src/main/resources/config,src/test/resources/config}
```

- [ ] **Step 2: Write pom.xml**

Create `dag-task/dag-admin-muserver/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>top.ilovemyhome.dagtask</groupId>
        <artifactId>dag-task</artifactId>
        <version>${revision}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <modelVersion>4.0.0</modelVersion>
    <artifactId>dag-admin-muserver</artifactId>
    <name>dag-admin-muserver - DAG Task Admin Server</name>
    <description>MuServer-based admin management server for DAG task scheduling system</description>

    <dependencies>
        <!-- DAG modules -->
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-si</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-admin</artifactId>
        </dependency>

        <!-- zora -->
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-common</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-static</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-rdb</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-json</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-config</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-muserver</artifactId>
        </dependency>

        <!-- muserver -->
        <dependency>
            <groupId>io.muserver</groupId>
            <artifactId>mu-server</artifactId>
        </dependency>

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <!-- jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.jakarta.rs</groupId>
            <artifactId>jackson-jakarta-rs-json-provider</artifactId>
        </dependency>
        <dependency>
            <groupId>org.json</groupId>
            <artifactId>json</artifactId>
            <version>20250517</version>
        </dependency>

        <!-- jakarta inject -->
        <dependency>
            <groupId>jakarta.inject</groupId>
            <artifactId>jakarta.inject-api</artifactId>
            <version>2.0.1</version>
        </dependency>

        <!-- SLF4J -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.zonky.test</groupId>
            <artifactId>embedded-postgres</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>metadata</directory>
                <filtering>true</filtering>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
        <testResources>
            <testResource>
                <directory>src/test/resources</directory>
            </testResource>
        </testResources>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>3.8.1</version>
                <executions>
                    <execution>
                        <id>copy-dependencies</id>
                        <phase>package</phase>
                        <goals>
                            <goal>copy-dependencies</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>target/lib</outputDirectory>
                            <overWriteReleases>false</overWriteReleases>
                            <overWriteSnapshots>false</overWriteSnapshots>
                            <overWriteIfNewer>true</overWriteIfNewer>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Write metadata.json**

Create `dag-task/dag-admin-muserver/metadata/metadata.json`:

```json
{
  "groupId": "@project.groupId@",
  "artifactId": "@project.artifactId@",
  "description": "@project.description@",
  "version": "@project.version@",
  "scmUrl": "@project.scmUrl@"
}
```

- [ ] **Step 4: Write App.java**

Create `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/App.java`:

```java
package top.ilovemyhome.dagtask.admin.server;

import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.admin.server.application.AppContext;
import top.ilovemyhome.dagtask.admin.server.application.WebServerBootstrap;
import top.ilovemyhome.zora.config.ConfigLoader;

public class App {

    public static void main(String[] args) {
        LOGGER.info("Starting dag-admin application.");
        String env = System.getProperty("env");
        if (StringUtils.isBlank(env)){
            throw new IllegalStateException("Cannot find env property.");
        }
        App schedulerServer = new App();
        schedulerServer.initAppContext(env);
        schedulerServer.initWebServer(schedulerServer.getAppContext());
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public static App getInstance() {
        return APP;
    }

    private App() {
    }

    private void initAppContext(String env){
        String rootConfig = "config/application.conf";
        String envConfig = "config/application-" + env + ".conf";
        Config config = ConfigLoader.loadConfig(rootConfig, envConfig);
        this.appContext = new AppContext(env, config);
    }

    private void initWebServer(AppContext appContext){
        WebServerBootstrap.start(appContext);
    }

    private AppContext appContext;
    private static App APP;
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

}
```

- [ ] **Step 5: Write application.conf**

Create `dag-task/dag-admin-muserver/src/main/resources/config/application.conf`:

```hocon
name = "dag-admin-server"

server = {
    port = 8000
    contextPath = "dag-admin-server"
}

cookie.domain = [
    "localhost"
    "jack007.top"
]

database = {
    jdbcUrl = NOT-SET
    username = NOT-SET
    password = NO-NEED
    driverClassName = "org.postgresql.Driver"
    maximumPoolSize = 10
    minimumIdle = 1
    autoCommit = false
    readOnly = false
}

flyway = {
    locations = ["classpath:db/migration"]
    baselineVersion = "1"
    baselineDescription = "Baseline"
    baselineOnMigrate = true
    table = "flyway_schema_history"
    defaultSchema = "public"
    placeholders = {
        owner_suffix = app_ro
    }
}

cookie = {
    name = "dag-admin-server-jwt"
    valueType = "JWT"
}

security = {
    whiteList = [
        "/**/openapi.json",
        "/**/api.html"
    ]
}

jwt = {
    issuer = "dag-task-scheduler"
    publicKeyLocation = "file:NOT-SET"
    privateKeyLocation = "file:NOT-SET"
    ttl = "7d"
}

users = []

query = {
    dateFormat = "yyyy-MM-dd"
    timestampFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    timeFormat = "HH:mm:ss.SSS"
}
```

- [ ] **Step 6: Write application-local.conf**

Create `dag-task/dag-admin-muserver/src/test/resources/config/application-local.conf`:

```hocon
jwt = {
    cookieName = "dag-admin-jwt"
    publicKeyLocation = "classpath:key/public.key"
    privateKeyLocation = "classpath:key/private.key"
}

database {
    jdbcUrl = "jdbc:postgresql://192.168.0.188:15432/postgres"
    username = "postgres"
    password = ""
    driverClassName = "org.postgresql.Driver"
    maximumPoolSize = 5
    minimumIdle = 1
    autoCommit = true
    readOnly = false
}

users = [
    {
        id = "0", name = "jack"
        , displayName = "administrator"
        , roles = ["admin", "read", "write"]
        , passwordHashVal = "6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b"
        , attributes = {
        email = "jack@localhost"
        phone = "1234567890"
    }
    }
]
```

- [ ] **Step 7: Commit**

```bash
git add dag-task/dag-admin-muserver/
git commit -m "feat: create dag-admin-muserver module structure"
```

---

## Task 4: Migrate Web Infrastructure to dag-admin-muserver

**Files:**
- Create: `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/application/AppContext.java`
- Create: `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/application/WebServerBootstrap.java`
- Create: `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/web/LoginHandler.java`
- Create: `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/web/security/SecurityHandler.java`
- Create: `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/web/security/JwtHelper.java`

- [ ] **Step 1: Copy and adapt AppContext**

Copy `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/application/AppContext.java` to `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/application/AppContext.java`.

Change the package declaration to:
```java
package top.ilovemyhome.dagtask.admin.server.application;
```

All other code remains identical.

- [ ] **Step 2: Copy and adapt WebServerBootstrap**

Copy `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/application/WebServerBootstrap.java` to `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/application/WebServerBootstrap.java`.

Change package to `top.ilovemyhome.dagtask.admin.server.application`.

Change the imports:
- Remove: `import top.ilovemyhome.dagtask.core.interfaces.AgentRegistryApi;`
- Add:
```java
import top.ilovemyhome.dagtask.admin.interfaces.api.WorkflowApi;
import top.ilovemyhome.dagtask.admin.interfaces.api.ExecutionApi;
import top.ilovemyhome.dagtask.admin.interfaces.api.StatsApi;
import top.ilovemyhome.dagtask.admin.interfaces.api.AgentAdminApi;
import top.ilovemyhome.dagtask.admin.interfaces.api.AgentWhitelistAdminApi;
import top.ilovemyhome.dagtask.admin.server.web.LoginHandler;
import top.ilovemyhome.dagtask.admin.server.web.security.SecurityHandler;
```

In `createRestHandler()`, replace the bean creation section with:

```java
DagSchedulerServer schedulerServer = appContext.getBean("dagSchedulerServer", DagSchedulerServer.class);
Config config = appContext.getConfig();
AppSecurityContext appSecurityContext = appContext.getBean("appSecurityContext", AppSecurityContext.class);

TaskOrderApi taskOrderApi = new TaskOrderApi(schedulerServer.getTaskOrderDao());
TaskTemplateApi taskTemplateApi = new TaskTemplateApi(schedulerServer.getTaskTemplateService());
AgentWhitelistAdminApi agentWhitelistAdminApi = new AgentWhitelistAdminApi(schedulerServer.getAgentWhitelistDao());

JwtConfig jwtConfig = appContext.getBean("jwtConfig", JwtConfig.class);
TokenService tokenService = new TokenService(schedulerServer.getAgentTokenDao(), jwtConfig);
TokenManagementApi tokenManagementApi = new TokenManagementApi(tokenService);

DagManageApi dagManageApi = new DagManageApi(schedulerServer.getDagManageService());
WorkflowApi workflowApi = new WorkflowApi(schedulerServer.getTaskTemplateService(), schedulerServer.getDagManageService());
ExecutionApi executionApi = new ExecutionApi(schedulerServer.getTaskOrderDao(), schedulerServer.getTaskRecordDao());
AgentAdminApi agentAdminApi = new AgentAdminApi(schedulerServer.getAgentDao(), schedulerServer.getAgentStatusDao());
StatsApi statsApi = new StatsApi();

return RestHandlerBuilder
    .restHandler(
        taskOrderApi,
        taskTemplateApi,
        agentWhitelistAdminApi,
        tokenManagementApi,
        dagManageApi,
        workflowApi,
        executionApi,
        agentAdminApi,
        statsApi
    )
    .addRequestFilter(appSecurityContext.getFacetFilter())
    // ... rest unchanged
```

Remove `AgentRegistryApi` from the REST handler registration.

- [ ] **Step 3: Copy LoginHandler**

Copy `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/web/LoginHandler.java` to `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/web/LoginHandler.java`.

Change package to `top.ilovemyhome.dagtask.admin.server.web`.

Change import:
```java
import top.ilovemyhome.dagtask.admin.server.application.AppContext;
```

- [ ] **Step 4: Copy SecurityHandler**

Copy `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/web/security/SecurityHandler.java` to `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/web/security/SecurityHandler.java`.

Change package to `top.ilovemyhome.dagtask.admin.server.web.security`.

Change import:
```java
import top.ilovemyhome.dagtask.admin.server.application.AppContext;
```

- [ ] **Step 5: Copy JwtHelper**

Copy `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/web/security/JwtHelper.java` to `dag-task/dag-admin-muserver/src/main/java/top/ilovemyhome/dagtask/admin/server/web/security/JwtHelper.java`.

Change package to `top.ilovemyhome.dagtask.admin.server.web.security`.

- [ ] **Step 6: Verify dag-admin-muserver compiles**

Run:
```bash
cd dag-task && mvn compile -pl dag-admin,dag-admin-muserver -am -q
```

Expected: BUILD SUCCESS. If compilation fails due to missing classes in `dag-admin`, fix imports.

- [ ] **Step 7: Commit**

```bash
git add dag-task/dag-admin-muserver/src/main/java/
git commit -m "feat: migrate web infrastructure to dag-admin-muserver"
```

---

## Task 5: Update dag-scheduler-muserver to Remove Admin APIs

**Files:**
- Modify: `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/application/WebServerBootstrap.java`

- [ ] **Step 1: Simplify WebServerBootstrap**

In `dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/application/WebServerBootstrap.java`, modify `createRestHandler()`:

Replace the current method body with:

```java
DagSchedulerServer schedulerServer = appContext.getBean("dagSchedulerServer", DagSchedulerServer.class);
Config config = appContext.getConfig();
AppSecurityContext appSecurityContext = appContext.getBean("appSecurityContext", AppSecurityContext.class);

JwtConfig jwtConfig = appContext.getBean("jwtConfig", JwtConfig.class);
TokenService tokenService = new TokenService(schedulerServer.getAgentTokenDao(), jwtConfig);

AgentRegistryApi agentRegistryApi = new AgentRegistryApi(schedulerServer.getAgentRegistryService());

return RestHandlerBuilder
    .restHandler(agentRegistryApi)
    .addRequestFilter(appSecurityContext.getFacetFilter())
    .addCustomReader(createJacksonJsonProvider())
    .addCustomWriter(createJacksonJsonProvider())
    .withCollectionParameterStrategy(CollectionParameterStrategy.NO_TRANSFORM)
    .withOpenApiHtmlUrl("/api.html")
    .withOpenApiJsonUrl("/openapi.json")
    .addExceptionMapper(ClientErrorException.class, e -> Response.status(Response.Status.BAD_REQUEST.getStatusCode())
        .type(MediaType.APPLICATION_JSON)
        .entity(Map.of("message", e.getMessage()))
        .build())
    .addExceptionMapper(InternalServerErrorException.class, e -> Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .type(MediaType.APPLICATION_JSON)
        .entity(Map.of("message", e.getMessage()))
        .build())
    .addExceptionMapper(JsonMappingException.class, e -> Response.status(Response.Status.BAD_REQUEST.getStatusCode())
        .type(MediaType.APPLICATION_JSON)
        .entity(Map.of("message", e.getMessage()))
        .build())
    .withOpenApiDocument(
        OpenAPIObjectBuilder.openAPIObject()
            .withInfo(
                infoObject()
                    .withTitle("Dag Task Agent API")
                    .withDescription("DAG-based task scheduling system Agent communication API")
                    .withVersion("1.0")
                    .build())
            .withExternalDocs(
                externalDocumentationObject()
                    .withDescription("Documentation docs")
                    .withUrl(URI.create("https//muserver.io/jaxrs"))
                    .build())
    );
```

Remove unused imports:
- `TaskOrderApi`
- `TaskTemplateApi`
- `AgentWhitelistAdminApi`
- `TokenManagementApi`
- `FooUserHandler`

- [ ] **Step 2: Remove migrated classes from dag-scheduler-muserver**

Delete the following files (they are now in `dag-admin`):
```bash
rm dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/interfaces/api/WorkflowApi.java
rm dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/interfaces/api/ExecutionApi.java
rm dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/interfaces/api/StatsApi.java
rm dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/interfaces/api/AgentAdminApi.java
rm dag-task/dag-scheduler-muserver/src/main/java/top/ilovemyhome/dagtask/server/interfaces/api/AgentWhitelistAdminApi.java
```

- [ ] **Step 3: Verify dag-scheduler-muserver compiles**

Run:
```bash
cd dag-task && mvn compile -pl dag-scheduler-muserver -am -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add dag-task/dag-scheduler-muserver/
git commit -m "refactor: remove admin apis from dag-scheduler-muserver, keep only agent registry"
```

---

## Task 6: Rename dag-scheduler-web to dag-admin-web

**Files:**
- Modify: `dag-task/dag-scheduler-web/pom.xml`

- [ ] **Step 1: Update pom.xml**

In `dag-task/dag-scheduler-web/pom.xml`, change:
```xml
<artifactId>dag-scheduler-web</artifactId>
<name>dag-scheduler-web - DAG Scheduler Web UI</name>
```
to:
```xml
<artifactId>dag-admin-web</artifactId>
<name>dag-admin-web - DAG Admin Web UI</name>
```

- [ ] **Step 2: Rename directory**

Run:
```bash
cd dag-task && git mv dag-scheduler-web dag-admin-web
```

- [ ] **Step 3: Commit**

```bash
git add dag-task/dag-admin-web/pom.xml
git commit -m "refactor: rename dag-scheduler-web to dag-admin-web"
```

---

## Task 7: Update Root pom.xml

**Files:**
- Modify: `dag-task/pom.xml`

- [ ] **Step 1: Update modules list**

In `dag-task/pom.xml`, change:
```xml
<modules>
    <module>dag-si</module>
    <module>dag-scheduler</module>
    <module>dag-scheduler-muserver</module>
    <module>dag-agent</module>
    <module>dag-agent-muserver</module>
    <module>dag-scheduler-web</module>
</modules>
```
to:
```xml
<modules>
    <module>dag-si</module>
    <module>dag-scheduler</module>
    <module>dag-scheduler-muserver</module>
    <module>dag-admin</module>
    <module>dag-admin-muserver</module>
    <module>dag-agent</module>
    <module>dag-agent-muserver</module>
    <module>dag-admin-web</module>
</modules>
```

- [ ] **Step 2: Add dependency management entries**

In `dag-task/pom.xml` `<dependencyManagement>`, add after `dag-scheduler-muserver` entry:

```xml
<dependency>
    <groupId>top.ilovemyhome.dagtask</groupId>
    <artifactId>dag-admin</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>top.ilovemyhome.dagtask</groupId>
    <artifactId>dag-admin-muserver</artifactId>
    <version>${revision}</version>
</dependency>
```

- [ ] **Step 3: Full build verification**

Run:
```bash
cd dag-task && mvn compile -q
```

Expected: BUILD SUCCESS for all modules.

- [ ] **Step 4: Commit**

```bash
git add dag-task/pom.xml
git commit -m "build: add dag-admin and dag-admin-muserver modules to root pom"
```

---

## Self-Review Checklist

- [ ] Spec coverage: All design decisions have corresponding tasks.
- [ ] Placeholder scan: No TBD, TODO, or vague requirements in the plan.
- [ ] Type consistency: All class names and method signatures match between tasks.
- [ ] Import paths: All migrated classes have correct package declarations.
- [ ] Build verification: Every task has a compile step.
