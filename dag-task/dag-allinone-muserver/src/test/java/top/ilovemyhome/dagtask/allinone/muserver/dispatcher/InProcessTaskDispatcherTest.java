package top.ilovemyhome.dagtask.allinone.muserver.dispatcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.ilovemyhome.dagtask.agent.core.TaskExecutionEngine;
import top.ilovemyhome.dagtask.agent.dto.ForceOkResult;
import top.ilovemyhome.dagtask.agent.dto.KillResult;
import top.ilovemyhome.dagtask.agent.dto.SubmissionResult;
import top.ilovemyhome.dagtask.si.DispatchResult;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.dagtask.si.persistence.TaskDispatchDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InProcessTaskDispatcherTest {

    @Mock
    private TaskExecutionEngine taskExecutionEngine;

    @Mock
    private TaskDispatchDao dispatchDao;

    private InProcessTaskDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new InProcessTaskDispatcher(dispatchDao);
        dispatcher.bindTaskExecutionEngine(taskExecutionEngine);
    }

    @Test
    void dispatch_shouldSubmitToTaskExecutionEngine() {
        // Given
        TaskRecord task = TaskRecord.builder()
            .withId(1L)
            .withOrderKey("ORDER-001")
            .withName("TestTask")
            .withExecutionKey("top.ilovemyhome.dagtask.examples.TestTask")
            .withStatus(TaskStatus.READY)
            .withInput("{}")
            .build();

        when(taskExecutionEngine.submit(eq(1L), eq("TestTask"), eq("top.ilovemyhome.dagtask.examples.TestTask"), eq("{}"), eq(true)))
            .thenReturn(SubmissionResult.accepted(1L, 0));

        // When
        DispatchResult result = dispatcher.dispatch(task);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        verify(taskExecutionEngine).submit(eq(1L), eq("TestTask"), eq("top.ilovemyhome.dagtask.examples.TestTask"), eq("{}"), eq(true));
        verify(dispatchDao).create(any());
    }

    @Test
    void dispatch_shouldReturnFailureWhenSubmissionRejected() {
        // Given
        TaskRecord task = TaskRecord.builder()
            .withId(2L)
            .withOrderKey("ORDER-002")
            .withName("RejectedTask")
            .withExecutionKey("top.ilovemyhome.dagtask.examples.RejectedTask")
            .withStatus(TaskStatus.READY)
            .build();

        when(taskExecutionEngine.submit(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(SubmissionResult.queueFull(10, 10));

        // When
        DispatchResult result = dispatcher.dispatch(task);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isFalse();
    }

    @Test
    void killTask_shouldCallTaskExecutionEngineKill() {
        // Given
        Long taskId = 1L;
        when(taskExecutionEngine.kill(taskId)).thenReturn(KillResult.successFromRunning(taskId));

        // When
        boolean result = dispatcher.killTask(taskId, "admin", "test kill");

        // Then
        assertThat(result).isTrue();
        verify(taskExecutionEngine).kill(taskId);
    }

    @Test
    void forceOkTask_shouldCallTaskExecutionEngineForceOk() {
        // Given
        Long taskId = 1L;
        when(taskExecutionEngine.forceOk(taskId)).thenReturn(ForceOkResult.successFromRunning(taskId));

        // When
        boolean result = dispatcher.forceOkTask(taskId, "admin", "test force ok");

        // Then
        assertThat(result).isTrue();
        verify(taskExecutionEngine).forceOk(taskId);
    }

    @Test
    void countAvailableAgents_shouldReturnOneInProcessMode() {
        assertThat(dispatcher.countAvailableAgents("any-key")).isEqualTo(1);
    }

    @Test
    void findAllActiveAgents_shouldReturnEmptyList() {
        assertThat(dispatcher.findAllActiveAgents()).isEmpty();
    }
}
