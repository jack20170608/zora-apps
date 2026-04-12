package top.ilovemyhome.dagtask.si.dto;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.si.enums.OpsType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationRequestTest {

    @Test
    void testConstructor_withAllParameters() {
        // Given
        Long taskId = 123L;
        OpsType opsType = OpsType.KILL;
        Boolean force = Boolean.TRUE;
        String reason = "Kill hanging task";
        String dealer = "admin";
        Instant requestDt = Instant.now().minusSeconds(60);

        // When
        OperationRequest request = new OperationRequest(taskId, opsType, force, reason, dealer, requestDt);

        // Then
        assertThat(request.taskId()).isEqualTo(123L);
        assertThat(request.opsType()).isEqualTo(OpsType.KILL);
        assertThat(request.force()).isTrue();
        assertThat(request.reason()).isEqualTo("Kill hanging task");
        assertThat(request.dealer()).isEqualTo("admin");
        assertThat(request.requestDt()).isEqualTo(requestDt);
    }

    @Test
    void testConstructor_withDefaultValues() {
        // Given
        Long taskId = 456L;
        OpsType opsType = OpsType.KILL;
        String reason = "Kill hanging task";
        String dealer = "operator";

        // When
        OperationRequest request = new OperationRequest(taskId, opsType, null, reason, dealer, null);

        // Then - verify default values are set correctly
        assertThat(request.taskId()).isEqualTo(456L);
        assertThat(request.opsType()).isEqualTo(OpsType.KILL);
        assertThat(request.force()).isFalse();
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
        OperationRequest request = new OperationRequest(taskId, opsType, null, "test", "tester", null);

        // Then force should be false
        assertThat(request.force()).isFalse();
    }

    @Test
    void testConstructor_forceExplicitFalse_staysFalse() {
        // Given
        Long taskId = 789L;
        OpsType opsType = OpsType.HOLD;

        // When force is explicitly false
        OperationRequest request = new OperationRequest(taskId, opsType, Boolean.FALSE, "test", "tester", null);

        // Then force should still be false
        assertThat(request.force()).isFalse();
    }

    @Test
    void testConstructor_forceExplicitTrue_staysTrue() {
        // Given
        Long taskId = 789L;
        OpsType opsType = OpsType.HOLD;

        // When force is explicitly true
        OperationRequest request = new OperationRequest(taskId, opsType, Boolean.TRUE, "test", "tester", null);

        // Then force should still be true
        assertThat(request.force()).isTrue();
    }

    @Test
    void testConstructor_submitOperation_throwsIllegalArgument() {
        // When opsType is SUBMIT
        assertThatThrownBy(() -> new OperationRequest(123L, OpsType.SUBMIT, null, "submit", "tester", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("submit request");
    }

    @Test
    void testConstructor_withNullTaskId_throwsNpe() {
        // When taskId is null
        assertThatThrownBy(() -> new OperationRequest(null, OpsType.KILL, null, "test", "tester", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testConstructor_withNullOpsType_throwsNpe() {
        // When opsType is null
        assertThatThrownBy(() -> new OperationRequest(123L, null, null, "test", "tester", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testAllNonSubmitOpsTypes_canCreateRequest() {
        Long taskId = 100L;
        for (OpsType opsType : OpsType.values()) {
            if (opsType == OpsType.SUBMIT) {
                continue; // SUBMIT is not allowed, tested separately
            }
            OperationRequest request = new OperationRequest(taskId, opsType, null, "test", "tester", null);
            assertThat(request.opsType()).isEqualTo(opsType);
            assertThat(request.force()).isFalse();
        }
    }
}
