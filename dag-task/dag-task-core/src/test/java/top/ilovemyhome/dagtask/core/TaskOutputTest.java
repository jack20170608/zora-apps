package top.ilovemyhome.dagtask.core;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.core.si.Bar;
import top.ilovemyhome.dagtask.core.si.Foo;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class TaskOutputTest {

    @Test
    public void testStringTaskOutput() {
        TaskOutput<String> output = TaskOutput.success(100L, "success generated");
        assertThat(output.isSuccess()).isTrue();
        assertThat(output.output()).isEqualTo("success generated");
        assertThat(output.toString()).isEqualTo("TaskOutput[taskId=100, isSuccess=true, message=null, output=success generated]");
    }

    @Test
    public void testStringOutputFailure(){
        TaskOutput<String> output = TaskOutput.fail(100L, "failed to generated", "DB error");
        System.out.println(JacksonUtil.toJson(output));
        assertThat(output.isSuccess()).isFalse();
        assertThat(output.output()).isEqualTo("failed to generated");
        assertThat(output.toString()).isEqualTo("TaskOutput[taskId=100, isSuccess=false, message=DB error, output=failed to generated]");
    }

    @Test
    public void testFooDeserialize(){
        TaskOutput<Foo> taskOutput = new TaskOutput<>(100L
            , true
            , "success"
            , new Foo(1L, List.of(new Bar(2L, "t1"), new Bar(3L, "t3"))
            , LocalDate.of(2025,6,1))
            );
        assertThat(taskOutput.taskId()).isEqualTo(100L);
        assertThat(taskOutput.output().id()).isEqualTo(1L);
        assertThat(taskOutput.output().barList()).isEqualTo(List.of(new Bar(2L, "t1"), new Bar(3L, "t3")));
        String jsonPayload = JacksonUtil.toJson(taskOutput);

        TaskOutput<Foo> taskOutput2= JacksonUtil.fromJson(jsonPayload, new TypeReference<>() {});
        assertThat(taskOutput).isEqualTo(taskOutput2);
    }

}
