package top.ilovemyhome.dagtask.scheduler.adapter.web.muserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.scheduler.port.in.AgentHeartbeatUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.RegisterAgentUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.ReportTaskResultUseCase;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterResponse;

class AgentRegistryApiTest {

    @Test
    void register_whenUseCaseRejects_returnsBadRequest() {
        RegisterAgentUseCase register = mock(RegisterAgentUseCase.class);
        AgentHeartbeatUseCase heartbeat = mock(AgentHeartbeatUseCase.class);
        ReportTaskResultUseCase report = mock(ReportTaskResultUseCase.class);
        AgentRegisterRequest request = new AgentRegisterRequest(
            "agent-1", "Agent 1", "http://localhost:8081", 2, 10, java.util.List.of("java"), false);
        when(register.registerAgent(request, null))
            .thenReturn(AgentRegisterResponse.failure("rejected"));

        Response response = new AgentRegistryApi(register, heartbeat, report).register(request, null);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }
}
