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
        TaskOutput output = TaskOutput.success(100L, "success generated");
        assertThat(output.isSuccess()).isTrue();
        assertThat(output.output()).isEqualTo("success generated");
        assertThat(output.toString()).isEqualTo("TaskOutput[taskId=100, isSuccess=true, message=null, output=success generated]");
    }

    @Test
    public void testStringOutputFailure(){
        TaskOutput output = TaskOutput.fail(100L, "failed to generated", "DB error");
        System.out.println(JacksonUtil.toJson(output));
        assertThat(output.isSuccess()).isFalse();
        assertThat(output.output()).isEqualTo("failed to generated");
        assertThat(output.toString()).isEqualTo("TaskOutput[taskId=100, isSuccess=false, message=DB error, output=failed to generated]");
    }

    @Test
    public void testFooDeserialize(){
        TaskOutput taskOutput = new TaskOutput(100L
            , true
            , "success"
            , new Foo(1L, List.of(new Bar(2L, "t1"), new Bar(3L, "t3"))
            , LocalDate.of(2025,6,1))
            );
        assertThat(taskOutput.taskId()).isEqualTo(100L);
        Foo output = (Foo) taskOutput.output();
        assertThat(output.id()).isEqualTo(1L);
        assertThat(output.barList()).isEqualTo(List.of(new Bar(2L, "t1"), new Bar(3L, "t3")));
        String jsonPayload = JacksonUtil.toJson(taskOutput);

        // Read the json tree and parse output separately to preserve type information
        TaskOutput taskOutput2= JacksonUtil.fromJson(jsonPayload, TaskOutput.class);
        assertThat(taskOutput.taskId()).isEqualTo(taskOutput2.taskId());
        // Parse the output field again with the correct type
        Foo output2 = JacksonUtil.fromJson(JacksonUtil.toJson(taskOutput2.output()), Foo.class);
        assertThat(output.id()).isEqualTo(output2.id());
    }

}
