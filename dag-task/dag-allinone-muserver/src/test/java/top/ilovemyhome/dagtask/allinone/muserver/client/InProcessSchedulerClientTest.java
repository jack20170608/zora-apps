package top.ilovemyhome.dagtask.allinone.muserver.client;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.dagtask.scheduler.port.in.ScheduleDagRunUseCase;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InProcessSchedulerClientTest {

    @Mock
    private ScheduleDagRunUseCase scheduleDagRunUseCase;

    private InProcessSchedulerClient client;

    @BeforeAll
    static void initJaxRs() {
        io.muserver.rest.MuRuntimeDelegate.ensureSet();
    }

    @BeforeEach
    void setUp() {
        client = new InProcessSchedulerClient(scheduleDagRunUseCase);
    }

    @Test
    void reportTaskResult_shouldCallOnTaskCompletedForEachResult() {
        // Given
        TaskExecuteResult result1 = new TaskExecuteResult("local-agent", 1L, true, "output1", Instant.now());
        TaskExecuteResult result2 = new TaskExecuteResult("local-agent", 2L, false, "output2", Instant.now());

        // When
        Response response = client.reportTaskResult(List.of(result1, result2));

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verify(scheduleDagRunUseCase).onTaskCompleted(eq(1L), eq(TaskStatus.SUCCESS), any(TaskOutput.class));
        verify(scheduleDagRunUseCase).onTaskCompleted(eq(2L), eq(TaskStatus.ERROR), any(TaskOutput.class));
    }

    @Test
    void register_shouldReturnOk() {
        // Given
        AgentRegisterRequest request = new AgentRegisterRequest(
            "local-agent", "Local Agent", "http://localhost:8080",
            4, 100, List.of(), false
        );

        // When
        Response response = client.register(request);

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verifyNoInteractions(scheduleDagRunUseCase);
    }

    @Test
    void unregister_shouldReturnOk() {
        // Given
        AgentUnregistration request = new AgentUnregistration("local-agent");

        // When
        Response response = client.unregister(request);

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verifyNoInteractions(scheduleDagRunUseCase);
    }
}
