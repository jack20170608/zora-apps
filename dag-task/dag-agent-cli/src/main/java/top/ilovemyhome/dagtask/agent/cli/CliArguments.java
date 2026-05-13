package top.ilovemyhome.dagtask.agent.cli;

import picocli.CommandLine;

/**
 * Command-line arguments for dag-agent-cli using picocli.
 */
@CommandLine.Command(
    name = "dag-agent-cli",
    description = "Trigger a single task execution from command line",
    mixinStandardHelpOptions = true,
    version = "1.0.0"
)
public class CliArguments implements Runnable {

    @CommandLine.Option(
        names = {"-e", "--execution"},
        description = "TaskExecution implementation class full qualified name",
        required = true
    )
    private String executionClass;

    @CommandLine.Option(
        names = {"-i", "--input"},
        description = "Input JSON for the task",
        defaultValue = "{}"
    )
    private String inputJson;

    @CommandLine.Option(
        names = {"-s", "--server-url"},
        description = "Scheduler server URL (if not set, runs in local NoOp mode)"
    )
    private String serverUrl;

    @CommandLine.Option(
        names = {"-a", "--agent-id"},
        description = "Agent ID (auto-generated if not set)"
    )
    private String agentId;

    @CommandLine.Option(
        names = {"-l", "--task-log-dir"},
        description = "Task log directory"
    )
    private String taskLogDir;

    @CommandLine.Option(
        names = {"-t", "--timeout"},
        description = "Timeout in milliseconds",
        defaultValue = "300000"
    )
    private long timeoutMs;

    public String getExecutionClass() { return executionClass; }
    public String getInputJson() { return inputJson; }
    public String getServerUrl() { return serverUrl; }
    public String getAgentId() { return agentId; }
    public String getTaskLogDir() { return taskLogDir; }
    public long getTimeoutMs() { return timeoutMs; }

    @Override
    public void run() {
        // Execution logic is in DagAgentCli.main()
    }
}