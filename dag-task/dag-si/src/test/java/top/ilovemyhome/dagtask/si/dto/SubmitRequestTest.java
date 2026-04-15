package top.ilovemyhome.dagtask.si.dto;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.si.enums.OpsType;
import top.ilovemyhome.dagtask.si.enums.PriorityType;
import top.ilovemyhome.dagtask.si.enums.TaskType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmitRequestTest {

    @Test
    void testConstructor_withAllParameters() {
        // Given
        Long taskId = 123L;
        TaskType taskType = TaskType.JAVA_CLASS_NAME;
        PriorityType priority = PriorityType.HIGH;
        String executionClass = "com.example.etl.MyETLTask";
        String input = "{\"source\":\"jdbc://db\",\"table\":\"sales\"}";
        String dealer = "admin";
        Instant requestDt = Instant.now().minusSeconds(60);

        // When - even though we pass different opsType, it should be forced to SUBMIT
        SubmitRequest request = new SubmitRequest(taskId, taskType, OpsType.KILL, priority, executionClass, input, dealer, requestDt);

        // Then
        assertThat(request.taskId()).isEqualTo(123L);
        assertThat(request.taskType()).isEqualTo(TaskType.JAVA_CLASS_NAME);
        assertThat(request.opsType()).isEqualTo(OpsType.SUBMIT); // should always be SUBMIT
        assertThat(request.priorityType()).isEqualTo(PriorityType.HIGH);
        assertThat(request.executionClass()).isEqualTo("com.example.etl.MyETLTask");
        assertThat(request.input()).isEqualTo("{\"source\":\"jdbc://db\",\"table\":\"sales\"}");
        assertThat(request.dealer()).isEqualTo("admin");
        assertThat(request.requestDt()).isEqualTo(requestDt);
    }

    @Test
    void testConstructor_withDefaultValues() {
        // Given
        Long taskId = 456L;
        TaskType taskType = TaskType.BASH_SCRIPTS;
        String executionClass = "#!/bin/bash\necho hello";
        String dealer = "system";

        // When
        SubmitRequest request = new SubmitRequest(taskId, taskType, null, null, executionClass, null, dealer, null);

        // Then
        assertThat(request.taskId()).isEqualTo(456L);
        assertThat(request.taskType()).isEqualTo(TaskType.BASH_SCRIPTS);
        assertThat(request.opsType()).isEqualTo(OpsType.SUBMIT);
        assertThat(request.priorityType()).isNull();
        assertThat(request.executionClass()).isEqualTo("#!/bin/bash\necho hello");
        assertThat(request.input()).isNull();
        assertThat(request.dealer()).isEqualTo("system");
        assertThat(request.requestDt()).isNotNull();
        assertThat(request.requestDt()).isBetween(Instant.now().minusSeconds(5), Instant.now());
    }

    @Test
    void testConstructor_alwaysForcesOpsTypeToSubmit() {
        // Given various ops types
        Long taskId = 789L;
        TaskType taskType = TaskType.PYTHON_SCRIPTS;

        // When passing different ops types
        SubmitRequest request2 = new SubmitRequest(taskId, taskType, OpsType.KILL, null, "print('hello')", null, "tester", null);
        SubmitRequest request3 = new SubmitRequest(taskId, taskType, OpsType.FORCE_OK, null, "print('hello')", null, "tester", null);
        SubmitRequest request4 = new SubmitRequest(taskId, taskType, OpsType.FORCE_NOK, null, "print('hello')", null, "tester", null);

        // Then all should be forced to SUBMIT
        assertThat(request2.opsType()).isEqualTo(OpsType.SUBMIT);
        assertThat(request3.opsType()).isEqualTo(OpsType.SUBMIT);
        assertThat(request4.opsType()).isEqualTo(OpsType.SUBMIT);
    }

    @Test
    void testConstructor_nullRequestDt_defaultsToNow() {
        // Given
        Long taskId = 111L;
        TaskType taskType = TaskType.GROOVY_SOURCE_CODE;

        // When requestDt is null
        SubmitRequest request = new SubmitRequest(taskId, taskType, null, null, "def execute() { return true }", "{}", "tester", null);

        // Then
        assertThat(request.requestDt()).isNotNull();
        assertThat(request.requestDt()).isBetween(Instant.now().minusSeconds(5), Instant.now());
    }

    @Test
    void testConstructor_withNullTaskId_throwsNpe() {
        // When taskId is null
        assertThatThrownBy(() -> new SubmitRequest(null, TaskType.JAVA_CLASS_NAME, null, null, "test.Test", null, "tester", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testConstructor_withNullTaskType_throwsNpe() {
        // When taskType is null
        assertThatThrownBy(() -> new SubmitRequest(123L, null, null, null, "test.Test", null, "tester", null))
            .isInstanceOf(NullPointerException.class);
    }
}
