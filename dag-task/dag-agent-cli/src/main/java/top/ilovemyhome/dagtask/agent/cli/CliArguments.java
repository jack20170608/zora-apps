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
        names = {"-I", "--id"},
        description = "The taskId, if not provided, will use the current timestamp",
        required = true
    )
    private Long id;

    @CommandLine.Option(
        names = {"-N", "--name"},
        description = "The task name, if not provided, will use the default name 'cli-task'",
        required = true
    )
    private String name;

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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String name;
        private String executionClass;
        private String inputJson;
        private String serverUrl;
        private String agentId;
        private String taskLogDir;
        private long timeoutMs;

        private Builder() {
        }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withExecutionClass(String executionClass) {
            this.executionClass = executionClass;
            return this;
        }

        public Builder withInputJson(String inputJson) {
            this.inputJson = inputJson;
            return this;
        }

        public Builder withServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder withAgentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder withTaskLogDir(String taskLogDir) {
            this.taskLogDir = taskLogDir;
            return this;
        }

        public Builder withTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public CliArguments build() {
            CliArguments cliArguments = new CliArguments();
            cliArguments.executionClass = this.executionClass;
            cliArguments.id = this.id;
            cliArguments.inputJson = this.inputJson;
            cliArguments.agentId = this.agentId;
            cliArguments.serverUrl = this.serverUrl;
            cliArguments.taskLogDir = this.taskLogDir;
            cliArguments.timeoutMs = this.timeoutMs;
            cliArguments.name = this.name;
            return cliArguments;
        }
    }
}
