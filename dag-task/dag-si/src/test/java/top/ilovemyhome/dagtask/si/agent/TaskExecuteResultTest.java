package top.ilovemyhome.dagtask.si.agent;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskExecuteResultTest {

    @Test
    void testFullConstructor_allFieldsSetCorrectly() {
        // Given
        String agentId = "agent-001";
        Long taskId = 12345L;
        boolean success = true;
        String output = "Task completed successfully\nOutput: 100 records processed";
        Instant endTime = Instant.now().minusSeconds(10);

        // When
        TaskExecuteResult report = new TaskExecuteResult(agentId, taskId, success, output, endTime);

        // Then
        assertThat(report.agentId()).isEqualTo("agent-001");
        assertThat(report.taskId()).isEqualTo(12345L);
        assertThat(report.success()).isTrue();
        assertThat(report.output()).isEqualTo("Task completed successfully\nOutput: 100 records processed");
        assertThat(report.endTime()).isEqualTo(endTime);
    }

    @Test
    void testShortConstructor_setsEndTimeToNow() {
        // Given
        String agentId = "agent-002";
        Long taskId = 67890L;
        boolean success = false;
        String output = "Task failed with timeout";

        // When
        TaskExecuteResult report = new TaskExecuteResult(agentId, taskId, success, output);

        // Then
        assertThat(report.agentId()).isEqualTo("agent-002");
        assertThat(report.taskId()).isEqualTo(67890L);
        assertThat(report.success()).isFalse();
        assertThat(report.output()).isEqualTo("Task failed with timeout");
        assertThat(report.endTime()).isNotNull();
        // End time should be recent
        assertThat(report.endTime()).isBetween(Instant.now().minusSeconds(5), Instant.now());
    }

    @Test
    void testFullConstructor_nullEndTime_defaultsToNow() {
        // Given
        String agentId = "agent-003";
        Long taskId = 11111L;
        boolean success = true;
        String output = "null end time test";

        // When
        TaskExecuteResult report = new TaskExecuteResult(agentId, taskId, success, output, null);

        // Then
        assertThat(report.agentId()).isEqualTo("agent-003");
        assertThat(report.taskId()).isEqualTo(11111L);
        assertThat(report.success()).isTrue();
        assertThat(report.output()).isEqualTo("null end time test");
        assertThat(report.endTime()).isNotNull();
        assertThat(report.endTime()).isBetween(Instant.now().minusSeconds(5), Instant.now());
    }

    @Test
    void testNullAgentId_throwsNpe() {
        // When agentId is null
        assertThatThrownBy(() -> new TaskExecuteResult(null, 123L, true, "output", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testNullTaskId_throwsNpe() {
        // When taskId is null
        assertThatThrownBy(() -> new TaskExecuteResult("agent-001", null, true, "output", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testNullOutput_allowed() {
        // output can be null (no validation)
        TaskExecuteResult report = new TaskExecuteResult("agent-004", 22222L, false, null, null);

        assertThat(report.agentId()).isEqualTo("agent-004");
        assertThat(report.taskId()).isEqualTo(22222L);
        assertThat(report.success()).isFalse();
        assertThat(report.output()).isNull();
        assertThat(report.endTime()).isNotNull();
    }
}
