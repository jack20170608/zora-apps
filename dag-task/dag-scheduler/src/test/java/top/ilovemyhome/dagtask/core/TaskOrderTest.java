package top.ilovemyhome.dagtask.core;

import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.enums.OrderType;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TaskOrderTest {

    @Test
    public void testToJson(){
        TaskOrder taskOrder = TaskOrder.builder()
            .withId(1L)
            .withKey("ICBCMonthyPay_MONTHLY_202406_PAY")
            .withName("ICBCMonthyPay")
            .withOrderType(OrderType.Monthly)
            .withAttributes(Map.of("client_country", "CHINA"))
            .withCreateDt(LocalDateTime.of(2024,8,1,11,12,54))
            .withLastUpdateDt(LocalDateTime.of(2024,8,1,8,1,18))
            .build();
        assertThat(taskOrder.getKey()).isEqualTo("ICBCMonthyPay_MONTHLY_202406_PAY");
        TaskOrder dTaskOrder = JacksonUtil.fromJson(JacksonUtil.toJson(taskOrder), TaskOrder.class);
        assertThat(taskOrder).isEqualTo(dTaskOrder);
    }


}
