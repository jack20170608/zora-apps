# dag-allinone-muserver

All-in-one server that combines the DAG task scheduler, admin center, and agent into a single JVM process with a unified HTTP server.

## Overview

This module provides a consolidated deployment option for the DAG task system. Instead of running three separate services (scheduler server, admin server, and agent server), the all-in-one mode runs everything in a single process:

- **Scheduler**: DAG scheduling engine with task orchestration and agent registry
- **Admin**: User management, workflow administration, execution monitoring, and token management
- **Agent**: Embedded task execution engine that runs tasks locally within the same JVM

### Benefits

- Simplified deployment for single-node or development environments
- Eliminates network overhead between scheduler and agent via in-process communication
- Single HTTP endpoint for all APIs
- Shared database connection pool

### Trade-offs

- Not suitable for distributed multi-node agent deployments
- All components share the same JVM heap and GC cycles
- Single point of failure for the entire stack

## Structure

```
dag-allinone-muserver/
├── metadata/
│   └── metadata.json          # Module metadata with Maven placeholders
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── top/ilovemyhome/dagtask/allinone/muserver/
│   │   │       ├── AllInOneApp.java                    # Main entry point
│   │   │       ├── application/
│   │   │       │   ├── AllInOneAppContext.java         # Application context wiring
│   │   │       │   └── AllInOneWebServerBootstrap.java # MuServer bootstrap
│   │   │       ├── agent/
│   │   │       │   └── EmbeddedAgentBootstrap.java     # Embedded agent lifecycle
│   │   │       ├── client/
│   │   │       │   └── InProcessSchedulerClient.java   # In-process result reporting
│   │   │       ├── database/
│   │   │       │   └── DatabaseBootstrap.java          # Shared database pool
│   │   │       ├── dispatcher/
│   │   │       │   └── InProcessTaskDispatcher.java    # Direct task dispatching
│   │   │       └── security/
│   │   │           └── AllInOneSecurityHandler.java    # Unified security handler
│   │   └── resources/
│   │       ├── config/
│   │       │   ├── application.conf      # Base configuration
│   │       │   ├── application-dev.conf  # Development overrides
│   │       │   └── application-prod.conf # Production overrides
│   │       └── logback.xml               # Logback configuration
│   └── test/
│       ├── java/                         # Unit tests
│       └── resources/
│           ├── config/
│           │   └── application-test.conf # Test configuration (H2)
│           └── simplelogger.properties   # Test logger configuration
└── pom.xml
```

## Dependencies

- `dag-allinone` - Aggregator POM for all-in-one dependencies
- `dag-si` - Shared domain models and service interfaces
- `dag-scheduler` / `dag-scheduler-muserver` - Scheduler core and REST APIs
- `dag-admin` / `dag-admin-muserver` - Admin core and REST APIs
- `dag-agent` / `dag-agent-muserver` - Agent core and REST APIs
- `mu-server` - MuServer HTTP framework
- `zora-config` - Typesafe Config loading utilities
- `HikariCP` - Connection pooling
- `JDBI` - Database access layer
- `Flyway` - Database migrations

## Building

### Requirements

- JDK 25
- Maven 3.8+
- PostgreSQL 14+ (for runtime; H2 can be used for testing)

### Build

```bash
cd dag-task
mvn clean package -pl dag-allinone-muserver -am
```

The shaded JAR is produced at:
```
dag-allinone-muserver/target/dag-allinone-muserver-${version}.jar
```

## Running

### Development

```bash
java -jar dag-allinone-muserver/target/dag-allinone-muserver-*.jar -Denv=dev
```

The server will load `application.conf` overlaid with `application-dev.conf`.

### Production

```bash
java -jar dag-allinone-muserver/target/dag-allinone-muserver-*.jar -Denv=prod
```

Ensure `application-prod.conf` contains production-grade database credentials and JWT key paths.

### Environment Selection

The `env` system property determines which overlay configuration is loaded:

| Environment | Config file loaded |
|-------------|-------------------|
| dev         | `application-dev.conf` |
| prod        | `application-prod.conf` |
| test        | `application-test.conf` |
| custom      | `application-{custom}.conf` |

## Unified API Endpoints

All APIs are exposed under a single context path (default: `/dag-task`).

### Admin APIs

| Method | Path | Description |
|--------|------|-------------|
| POST   | /api/v1/login | User login (returns JWT cookie) |
| GET    | /api/v1/admin/tasks | Task order management |
| GET    | /api/v1/admin/templates | Task template management |
| POST   | /api/v1/admin/workflows | Workflow (DAG) management |
| GET    | /api/v1/admin/executions | Execution records and monitoring |
| GET    | /api/v1/admin/agents | Agent administration |
| GET    | /api/v1/admin/stats | System statistics |
| POST   | /api/v1/admin/tokens | Token management |
| GET    | /api/v1/admin/whitelist | Agent whitelist management |

### Scheduler APIs

| Method | Path | Description |
|--------|------|-------------|
| POST   | /api/v1/scheduler/register | Agent registration |
| POST   | /api/v1/scheduler/unregister | Agent unregistration |
| GET    | /api/v1/scheduler/agents | List registered agents |

### Agent APIs

| Method | Path | Description |
|--------|------|-------------|
| POST   | /api/v1/agent/tasks/{taskId}/result | Submit task execution result |
| GET    | /api/v1/agent/tasks/{taskId} | Query task execution status |
| POST   | /api/v1/agent/tasks/{taskId}/kill | Kill a running task |
| POST   | /api/v1/agent/tasks/{taskId}/forceOk | Force mark task as success |
| GET    | /api/v1/agent/health | Agent health check |
| GET    | /api/v1/agent/ping | Agent ping endpoint |

### Documentation

- Swagger UI: `http://localhost:8080/dag-task/api.html`
- OpenAPI JSON: `http://localhost:8080/dag-task/openapi.json`

## Configuration

Configuration uses [Typesafe Config](https://github.com/lightbend/config) (HOCON format).

### Key Sections

#### Server

```hocon
server {
    port = 8080
    host = "0.0.0.0"
    contextPath = "dag-task"
}
```

#### Database

Two keys are provided for compatibility between the shared `DatabaseBootstrap` (`database.url`) and the admin `AppContext` (`database.jdbcUrl`):

```hocon
database {
    url = "jdbc:postgresql://localhost:5432/dagtask"
    jdbcUrl = ${database.url}
    username = "postgres"
    password = "postgres"
    driverClassName = "org.postgresql.Driver"
    maximumPoolSize = 10
    minimumIdle = 2
    autoCommit = true
    readOnly = false
    pool {
        maxSize = ${database.maximumPoolSize}
    }
}
```

#### Embedded Agent

```hocon
agent {
    maxConcurrentTasks = 4
    maxPendingTasks = 100
    taskLogDir = "/tmp/dagtask/logs"
}
```

#### Security and JWT

```hocon
security {
    cookie {
        name = "dag_token"
        valueType = "JWT"
    }
    users = [
        {
            id = "1"
            name = "admin"
            displayName = "Administrator"
            roles = ["admin", "read", "write"]
            passwordHashVal = "..."
            attributes = {
                email = "admin@localhost"
            }
        }
    ]
    jwt {
        issuer = "dag-task-allinone"
        audience = "dag-task-client"
        publicKeyLocation = "classpath:key/public.key"
        privateKeyLocation = "classpath:key/private.key"
        ttl = "7d"
    }
}
```

#### Flyway

```hocon
flyway {
    location = "db/migration/postgresql"
    locations = ["db/migration/postgresql"]
    baselineOnMigrate = true
    baselineVersion = "0"
    baselineDescription = "Baseline"
    table = "flyway_schema_history"
    defaultSchema = "public"
}
```

## Development

### Adding a New Environment

Create `src/main/resources/config/application-{env}.conf` and run with `-Denv={env}`.

### Running Tests

```bash
mvn test -pl dag-allinone-muserver
```

The test configuration uses an H2 in-memory database. Note that PostgreSQL-specific Flyway migrations may require H2 compatibility mode for full integration testing.

### Security Notes

- The dev configuration uses classpath RSA keys for JWT signing. **Do not use these in production.**
- The default dev user password hash corresponds to `"1"`. Change this before any non-local deployment.
- The `AllInOneSecurityHandler` whitelists health check, OpenAPI, and login endpoints.
