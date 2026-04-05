# dag-task-agent-muserver

MuServer-based implementation that hosts the DAG task agent and provides HTTP endpoints for agent management.

## Overview

This module provides the HTTP server hosting for the DAG task agent using MuServer. It:
- Embeds the MuServer HTTP server
- Exposes the REST API endpoints from `dag-task-agent`
- Handles configuration loading
- Manages the agent lifecycle
- Provides OpenAPI documentation

## Structure

```
dag-task-agent-muserver/
├── metadata/
│   └── metadata.json          # Module metadata
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── top/ilovemyhome/dagtask/agent/muserver/
│   │   │       ├── application/
│   │   │       │   ├── AppContext.java       # Application context holding all components
│   │   │       │   └── WebServerBootstrap.java  # MuServer bootstrap
│   │   │       └── starter/
│   │   │           └── AgentMain.java        # Main entry point
│   │   └── resources/
│   │       ├── config/
│   │       │   └── agent-muserver.conf       # Default configuration
│   │       └── logback.xml                   # Logback configuration
│   └── test/
│       ├── java/
│       └── resources/
└── pom.xml
```

## Dependencies

- `dag-task-agent` - Core agent logic (no HTTP server dependency)
- `zora-muserver` - Zora MuServer integration
- `mu-server` - MuServer itself
- `zora-config` - Configuration loading
- `jackson` - JSON serialization

## Running

### Requirements
- JDK 25
- Maven 3.8+

### Build
```bash
cd dag-task-agent-muserver
mvn clean package
```

### Run
```bash
java -Denv=local -cp "target/classes:target/lib/*" top.ilovemyhome.dagtask.agent.muserver.starter.AgentMain
```

## Configuration

Configuration is loaded via Typesafe Config. The main configuration file is `config/agent-muserver.conf`:

```hocon
# MuServer configuration
muserver {
  port = 8080                       # HTTP port
  host = "0.0.0.0"                  # Bind address
  contextPath = "/"                  # Context path
  maxHeaderSize = 8192              # Maximum header size
  idleTimeout = 30000               # Idle timeout in milliseconds
}

# Include the agent configuration from dag-task-agent
include "classpath:config/agent.conf"
```

The `agent.conf` from `dag-task-agent` contains:

```hocon
dag-agent {
  agentUrl = "http://localhost:8080"    # URL accessible by DAG server
  dagServerUrl = "http://localhost:8081" # URL of the DAG scheduling server
  agentId = "agent-local"                # Unique agent identifier
  autoRegister = true                    # Auto-register on startup
  maxConcurrentTasks = 4                 # Maximum concurrent tasks
  maxPendingTasks = 100                  # Maximum pending tasks
  supportedExecutionKeys = [
    "top.ilovemyhome.dagtask.core.DefaultTaskExecution"
  ]
}
```

## API Endpoints

All REST endpoints are provided by `TaskAgentResource` from the `dag-task-agent` module:
- GET /status - Get agent status
- POST /tasks/{taskId}/result - Submit task result
- GET /tasks/{taskId} - Get task status
- And more...

OpenAPI documentation is available at:
- `http://localhost:8080/openapi.json` - OpenAPI JSON
- `http://localhost:8080/api.html` - Swagger UI HTML

## Architecture

This module follows the separation of concerns pattern:
- `dag-task-agent` contains core agent logic with no HTTP server dependency
- `dag-task-agent-muserver` provides the MuServer HTTP embedding
- This makes it easy to embed the agent in different HTTP server implementations

## Development

### Adding custom configuration

Create an environment-specific configuration file at `src/main/resources/config/agent-muserver-{env}.conf` and run with `-Denv={env}`.

### Running tests

```bash
mvn test
```
