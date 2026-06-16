package top.ilovemyhome.dagtask.agent.client;

import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;

import java.util.List;

/**
 * No-op implementation of {@link AgentSchedulerClient} for local CLI execution mode.
 * Logs operations but does not communicate with any real scheduling server.
 */
public class NoOpAgentSchedulerClient implements AgentSchedulerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpAgentSchedulerClient.class);

    @Override
    public Response register(AgentRegisterRequest registration) {
        LOGGER.info("NoOp register for agent {}", registration.agentId());
        return SimpleAgentResponse.okResponse();
    }

    @Override
    public Response unregister(AgentUnregistration unregistration) {
        LOGGER.info("NoOp unregister for agent {}", unregistration.agentId());
        return SimpleAgentResponse.okResponse();
    }

    @Override
    public Response reportTaskResult(List<TaskExecuteResult> results) {
        LOGGER.debug("NoOp reportTaskResult for {} tasks", results.size());
        return SimpleAgentResponse.okResponse();
    }
}