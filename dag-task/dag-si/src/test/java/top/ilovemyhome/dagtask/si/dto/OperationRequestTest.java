package top.ilovemyhome.dagtask.si.dto;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.si.enums.OpsType;
import top.ilovemyhome.dagtask.si.enums.PriorityType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationRequestTest {

    @Test
    void testConstructor_withAllParameters() {
        // Given
        Long taskId = 123L;
        OpsType opsType = OpsType.SUBMIT;
        String input = "{\"param1\":\"value1\"}";
        Boolean force = Boolean.TRUE;
        PriorityType priority = PriorityType.HIGH;
        String reason = "Manual submission";
        String dealer = "admin";
        Instant requestDt = Instant.now().minusSeconds(60);

        // When
        OperationRequest request = new OperationRequest(taskId, opsType, input, force, priority, reason, dealer, requestDt);

        // Then
        assertThat(request.taskId()).isEqualTo(123L);
        assertThat(request.opsType()).isEqualTo(OpsType.SUBMIT);
        assertThat(request.input()).isEqualTo("{\"param1\":\"value1\"}");
        assertThat(request.force()).isTrue();
        assertThat(request.priorityType()).isEqualTo(PriorityType.HIGH);
        assertThat(request.reason()).isEqualTo("Manual submission");
        assertThat(request.dealer()).isEqualTo("admin");
        assertThat(request.requestDt()).isEqualTo(requestDt);
    }

    @Test
    void testConstructor_withDefaultValues() {
        // Given
        Long taskId = 456L;
        OpsType opsType = OpsType.KILL;
        String input = null;
        String reason = "Kill hanging task";
        String dealer = "operator";

        // When
        OperationRequest request = new OperationRequest(taskId, opsType, input, null, null, reason, dealer, null);

        // Then - verify default values are set correctly
        assertThat(request.taskId()).isEqualTo(456L);
        assertThat(request.opsType()).isEqualTo(OpsType.KILL);
        assertThat(request.input()).isNull();
        assertThat(request.force()).isFalse();
        assertThat(request.priorityType()).isEqualTo(PriorityType.NORMAL);
        assertThat(request.reason()).isEqualTo("Kill hanging task");
        assertThat(request.dealer()).isEqualTo("operator");
        assertThat(request.requestDt()).isNotNull();
        // Request time should be recent
        assertThat(request.requestDt()).isBetween(Instant.now().minusSeconds(5), Instant.now());
    }

    @Test
    void testConstructor_forceNull_defaultsToFalse() {
        // Given
        Long taskId = 789L;
        OpsType opsType = OpsType.HOLD;

        // When force is null
        OperationRequest request = new OperationRequest(taskId, opsType, null, null, null, "test", "tester", null);

        // Then force should be false
        assertThat(request.force()).isFalse();
    }

    @Test
    void testConstructor_forceExplicitFalse_staysFalse() {
        // Given
        Long taskId = 789L;
        OpsType opsType = OpsType.HOLD;

        // When force is explicitly false
        OperationRequest request = new OperationRequest(taskId, opsType, null, Boolean.FALSE, null, "test", "tester", null);

        // Then force should still be false
        assertThat(request.force()).isFalse();
    }

    @Test
    void testConstructor_forceExplicitTrue_staysTrue() {
        // Given
        Long taskId = 789L;
        OpsType opsType = OpsType.HOLD;

        // When force is explicitly true
        OperationRequest request = new OperationRequest(taskId, opsType, null, Boolean.TRUE, null, null, "tester", null);

        // Then force should still be true
        assertThat(request.force()).isTrue();
    }

    @Test
    void testConstructor_partialArgs_keepsExplicitValues() {
        // Given
        Long taskId = 789L;
        OpsType opsType = OpsType.HOLD;
        String input = "{\"debug\":true}";
        Boolean force = Boolean.TRUE;
        String reason = "Hold for maintenance";
        String dealer = "devops";

        // When
        OperationRequest request = new OperationRequest(taskId, opsType, input, force, PriorityType.LOW, reason, dealer, null);

        // Then
        assertThat(request.taskId()).isEqualTo(789L);
        assertThat(request.opsType()).isEqualTo(OpsType.HOLD);
        assertThat(request.input()).isEqualTo("{\"debug\":true}");
        assertThat(request.force()).isTrue();
        assertThat(request.priorityType()).isEqualTo(PriorityType.LOW);
        assertThat(request.reason()).isEqualTo("Hold for maintenance");
        assertThat(request.dealer()).isEqualTo("devops");
        assertThat(request.requestDt()).isNotNull();
    }

    @Test
    void testConstructor_withNullTaskId_throwsNpe() {
        // When taskId is null
        assertThatThrownBy(() -> new OperationRequest(null, OpsType.SUBMIT, null, null, null, null, "tester", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testConstructor_withNullOpsType_throwsNpe() {
        // When opsType is null
        assertThatThrownBy(() -> new OperationRequest(123L, null, null, null, null, null, "tester", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testAllOpsTypes_canCreateRequest() {
        Long taskId = 100L;
        for (OpsType opsType : OpsType.values()) {
            OperationRequest request = new OperationRequest(taskId, opsType, null, null, null, "test", "tester", null);
            assertThat(request.opsType()).isEqualTo(opsType);
            assertThat(request.force()).isFalse();
            assertThat(request.priorityType()).isEqualTo(PriorityType.NORMAL);
        }
    }
}
