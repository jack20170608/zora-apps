package top.ilovemyhome.dagtask.si;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TaskRecordTest {

    @Test
    void testTaskRecordJsonSerialization() {
        TaskRecord r1 = TaskRecord.builder()
            .withId(1L)
            .withOrderKey("FOO_DAILY_20240601")
            .withName("Foo Task")
            .withDescription("A foo task for unit test purpose only!")
            .withExecutionKey("top.ilovemyhome.peanotes.backend.common.task.impl.execution.ConditionExceptionalExecution")
            .withSuccessorIds(Set.of(2L, 3L))
            .withInput("task input")
            .withOutput("task output")
            .withAsync(true)
            .withDummy(true)
            .withCreateDt(LocalDateTime.of(2024,7,1,12,9,19))
            .withLastUpdateDt(LocalDateTime.of(2024,7,2,12,9,19))
            .withStatus(TaskStatus.SUCCESS)
            .withStartDt(LocalDateTime.of(2024,7,1,12,10,19))
            .withEndDt(LocalDateTime.of(2024,7,1,12,20,19))
            .withSuccess(true)
            .withFailReason(null)
            .build();
        TaskRecord r2 = JacksonUtil.fromJson(JacksonUtil.toJson(r1), TaskRecord.class);
        assertThat(r1).isEqualTo(r2);
    }
}
