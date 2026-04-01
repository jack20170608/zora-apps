package top.ilovemyhome.dagtask.core;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.core.si.Bar;
import top.ilovemyhome.dagtask.core.si.Foo;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TaskInputTest {

    @Test
    public void testStringDeserialize(){
        String jsonPayload = """
            {
              "taskId" : 100,
              "input" : "t1Input",
              "attributes" : {
                "p1" : "v1",
                "p2" : "v2"
              }
            }
            """;
        TaskInput<String> taskInput = JacksonUtil.fromJson(jsonPayload, TaskInput.class);
        assertThat(taskInput.taskId()).isEqualTo(100L);
        assertThat(taskInput.input()).isEqualTo("t1Input");
        assertThat(taskInput.attributes().get("p1")).isEqualTo("v1");
        assertThat(taskInput.attributes().get("p2")).isEqualTo("v2");
    }


    @Test
    public void testFooDeserialize(){
        TaskInput<Foo> taskInput = new TaskInput<>(100L
            , new Foo(1L, List.of(new Bar(2L, "t1"), new Bar(3L, "t3"))
            , LocalDate.of(2025,6,1))
            , Map.of("p1", "v1", "p2", "v2"));
        assertThat(taskInput.taskId()).isEqualTo(100L);
        assertThat(taskInput.input().id()).isEqualTo(1L);
        assertThat(taskInput.input().barList()).isEqualTo(List.of(new Bar(2L, "t1"), new Bar(3L, "t3")));
        assertThat(taskInput.input().someDate()).isEqualTo(LocalDate.of(2025,6,1));
        assertThat(taskInput.attributes().get("p1")).isEqualTo("v1");
        assertThat(taskInput.attributes().get("p2")).isEqualTo("v2");
        String jsonPayload = JacksonUtil.toJson(taskInput);
        TaskInput<Foo> taskInput2 = JacksonUtil.fromJson(jsonPayload, new TypeReference<>() {});
        assertThat(taskInput).isEqualTo(taskInput2);
    }






}
