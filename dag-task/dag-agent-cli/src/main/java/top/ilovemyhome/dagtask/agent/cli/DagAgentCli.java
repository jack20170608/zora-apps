package top.ilovemyhome.dagtask.agent.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import top.ilovemyhome.dagtask.agent.client.DefaultAgentSchedulerClient;
import top.ilovemyhome.dagtask.agent.client.NoOpAgentSchedulerClient;
import top.ilovemyhome.dagtask.agent.config.AgentConfiguration;
import top.ilovemyhome.dagtask.agent.core.TaskExecutionEngine;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * Command-line entry point for triggering a single task execution.
 * Supports both local NoOp mode and scheduler server reporting mode.
 */
public class DagAgentCli {

    private static final Logger LOGGER = LoggerFactory.getLogger(DagAgentCli.class);

    public static void main(String[] args) {
        CliArguments arguments = new CliArguments();
        CommandLine cmd = new CommandLine(arguments);
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            LOGGER.error("Execution failed: {}", ex.getMessage(), ex);
            System.err.println("Execution failed: " + ex.getMessage());
            return 1;
        });

        int exitCode = cmd.execute(args);
        if (exitCode != 0 || cmd.isUsageHelpRequested() || cmd.isVersionHelpRequested()) {
            System.exit(exitCode);
        }

        exitCode = execute(arguments);
        System.exit(exitCode);
    }

    static int execute(CliArguments args) {
        String agentId = args.getAgentId() != null ? args.getAgentId() : "cli-" + UUID.randomUUID();
        String serverUrl = args.getServerUrl();
        boolean reportResult = serverUrl != null && !serverUrl.isBlank();

        long now = System.currentTimeMillis();
        AgentConfiguration config = buildConfiguration(agentId, serverUrl, args.getTaskLogDir());
        AgentSchedulerClient client = buildClient(config, serverUrl);
        ObjectMapper objectMapper = new ObjectMapper();

        var executor = Executors.newFixedThreadPool(1);
        TaskExecutionEngine engine = new TaskExecutionEngine(config, client, executor, objectMapper);

        var taskId = Objects.isNull(args.getId()) ? now : args.getId();
        var startTime = now;
        var taskName = StringUtils.isNotBlank(args.getName()) ? args.getName() : "cli-task";

        try {
            engine.start();
            TaskExecuteResult result = engine.submitAndWait(
                taskId,
                taskName,
                args.getExecutionClass(),
                args.getInputJson(),
                reportResult,
                args.getTimeoutMs()
            );
            long duration = System.currentTimeMillis() - startTime;
            printResult(result, duration);
            return result.success() ? 0 : 1;
        } catch (TimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("Task execution timed out after " + duration + "ms");
            return 1;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during task execution", e);
            System.err.println("Task execution failed: " + e.getMessage());
            return 1;
        } finally {
            engine.stop();
            executor.shutdown();
        }
    }

    private static AgentConfiguration buildConfiguration(String agentId, String serverUrl, String taskLogDir) {
        AgentConfiguration.Builder builder = AgentConfiguration.builder()
            .agentId(agentId)
            .agentUrl("http://localhost:0")
            .dagServerUrl(serverUrl != null ? serverUrl : "http://localhost:0")
            .autoRegister(false);

        if (taskLogDir != null && !taskLogDir.isBlank()) {
            builder.taskLogDir(taskLogDir);
        }

        return builder.build();
    }

    private static AgentSchedulerClient buildClient(AgentConfiguration config, String serverUrl) {
        if (serverUrl != null && !serverUrl.isBlank()) {
            return new DefaultAgentSchedulerClient(config, new ObjectMapper());
        }
        return new NoOpAgentSchedulerClient();
    }

    private static void printResult(TaskExecuteResult result, long durationMs) {
        String status = result.success() ? "SUCCESS" : "FAILED";
        System.out.println("Task execution completed:");
        System.out.println("  Task ID: " + result.taskId());
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + result.output());
        System.out.println("  Duration: " + durationMs + "ms");
    }
}
