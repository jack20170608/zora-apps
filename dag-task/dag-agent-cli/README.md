# dag-agent-cli

Command-line tool for triggering a single task execution to quickly verify `TaskExecution` implementations.

## Purpose

This module provides a lightweight CLI that reuses `dag-agent`'s `TaskExecutionEngine` to execute a single task. It supports both local NoOp mode (no scheduler server needed) and scheduler server reporting mode.

## Usage

### Local Mode (NoOp)

Execute a task locally without connecting to a scheduler server:

```bash
java -jar dag-agent-cli.jar \
  -e top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution \
  -i '{"command":"echo hello"}'
```

### Scheduler Server Mode

Execute a task and report the result to a scheduler server:

```bash
java -jar dag-agent-cli.jar \
  -e top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution \
  -i '{"command":"echo hello"}' \
  -s http://localhost:8080
```

### Command-line Options

| Option | Short | Required | Default | Description |
|--------|-------|----------|---------|-------------|
| `--execution` | `-e` | Yes | - | TaskExecution implementation class full qualified name |
| `--input` | `-i` | No | `{}` | Input JSON for the task |
| `--server-url` | `-s` | No | - | Scheduler server URL |
| `--agent-id` | `-a` | No | auto | Agent ID |
| `--task-log-dir` | `-l` | No | - | Task log directory |
| `--timeout` | `-t` | No | 300000 | Timeout in milliseconds |
| `--help` | `-h` | No | - | Show help |

## Output Format

On success:
```
Task execution completed:
  Task ID: 1
  Status: SUCCESS
  Output: {"exitCode":0,"stdout":"hello\n","stderr":"","timedOut":false}
  Duration: 120ms
```

On failure:
```
Task execution completed:
  Task ID: 1
  Status: FAILED
  Output: script is required and must not be blank
  Duration: 5ms
```

## Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Task executed successfully |
| 1 | Task execution failed or timed out |
