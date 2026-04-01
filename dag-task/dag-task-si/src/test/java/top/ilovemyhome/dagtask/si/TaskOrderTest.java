package top.ilovemyhome.dagtask.si;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;
import top.ilovemyhome.dagtask.si.enums.OrderType;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class TaskOrderTest {

    @Test
    void testTaskOrderToJson() {
        TaskOrder task = TaskOrder.builder()
            .withId(1L)
            .withName("FOO")
            .withKey("FOO_MONTHLY_v1_v2")
            .withOrderType(OrderType.Monthly)
            .withAttributes(Map.of("p1","v1","p2","v2"))
            .withCreateDt(LocalDateTime.of(2024,1,4,8,1,32))
            .withLastUpdateDt(LocalDateTime.of(2024,11,9,8,32,32))
            .build();

        String jsonSerializedTask = JacksonUtil.toJson(task);
        TaskOrder taskFromJson = JacksonUtil.fromJson(jsonSerializedTask, TaskOrder.class);

        assert taskFromJson != null;
        assertThat(taskFromJson.getId()).isEqualTo(1L);
        assertThat(taskFromJson.getName()).isEqualTo("FOO");
        assertThat(taskFromJson.getKey()).isEqualTo("FOO_MONTHLY_v1_v2");
        assertThat(taskFromJson.getOrderType()).isEqualTo(OrderType.Monthly);
        assertThat(taskFromJson.getAttributes()).isEqualTo(Map.of("p1","v1","p2","v2"));
        assertThat(taskFromJson.getCreateDt()).isNotNull();
        assertThat(taskFromJson.getLastUpdateDt()).isNotNull();

    }

    @Test
    void testSimpleTaskOrderToJson2() {
        TaskOrder task = TaskOrder.builder()
            .withId(1L)
            .withName("FOO")
            .withKey("key")
            .withOrderType(OrderType.Monthly)
            .build();

        String jsonSerializedTask = JacksonUtil.toJson(task);
        TaskOrder taskFromJson = JacksonUtil.fromJson(jsonSerializedTask, TaskOrder.class);

        assert taskFromJson != null;
        assertThat(taskFromJson.getId()).isEqualTo(1L);
        assertThat(taskFromJson.getName()).isEqualTo("FOO");
        assertThat(taskFromJson.getKey()).isEqualTo("key");
        assertThat(taskFromJson.getOrderType()).isEqualTo(OrderType.Monthly);
        assertThat(taskFromJson.getAttributes()).isNull();
        assertThat(taskFromJson.getCreateDt()).isNotNull();
        assertThat(taskFromJson.getLastUpdateDt()).isNotNull();

    }
}
