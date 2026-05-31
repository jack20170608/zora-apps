package top.ilovemyhome.dagtask.allinone.muserver.client;

import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.dagtask.si.service.DagScheduleService;

import java.util.List;

/**
 * In-process scheduler client that replaces HTTP-based result reporting.
 * Directly calls DagScheduleService.onTaskCompleted() instead of POSTing
 * to /api/schedule/agent/result endpoint.
 */
public class InProcessSchedulerClient implements AgentSchedulerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(InProcessSchedulerClient.class);

    private final DagScheduleService dagScheduleService;

    public InProcessSchedulerClient(DagScheduleService dagScheduleService) {
        this.dagScheduleService = dagScheduleService;
    }

    @Override
    public Response register(AgentRegisterRequest registration) {
        LOGGER.debug("Ignoring register for embedded agent: {}", registration.agentId());
        return Response.ok().build();
    }

    @Override
    public Response unregister(AgentUnregistration unregistration) {
        LOGGER.debug("Ignoring unregister for embedded agent: {}", unregistration.agentId());
        return Response.ok().build();
    }

    @Override
    public Response reportTaskResult(List<TaskExecuteResult> results) {
        LOGGER.debug("Reporting {} task results in-process", results.size());

        for (TaskExecuteResult result : results) {
            try {
                TaskStatus status = result.success() ? TaskStatus.SUCCESS : TaskStatus.ERROR;
                TaskOutput output = result.success()
                    ? TaskOutput.success(result.taskId(), result.output())
                    : TaskOutput.fail(result.taskId(), result.output(), "Task execution failed");

                dagScheduleService.onTaskCompleted(result.taskId(), status, output);
                LOGGER.debug("Reported result for task {} in-process: {}", result.taskId(), status);
            } catch (Exception e) {
                LOGGER.error("Failed to report result for task {} in-process", result.taskId(), e);
            }
        }

        return Response.ok().build();
    }
}
