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
        TaskInput taskInput = JacksonUtil.fromJson(jsonPayload, TaskInput.class);
        assertThat(taskInput.taskId()).isEqualTo(100L);
        assertThat(taskInput.input()).isEqualTo("t1Input");
        assertThat(taskInput.attributes().get("p1")).isEqualTo("v1");
        assertThat(taskInput.attributes().get("p2")).isEqualTo("v2");
    }


    @Test
    public void testFooDeserialize(){
        TaskInput taskInput = new TaskInput(100L
            , new Foo(1L, List.of(new Bar(2L, "t1"), new Bar(3L, "t3"))
            , LocalDate.of(2025,6,1))
            , Map.of("p1", "v1", "p2", "v2"));
        assertThat(taskInput.taskId()).isEqualTo(100L);
        Foo input = (Foo) taskInput.input();
        assertThat(input.id()).isEqualTo(1L);
        assertThat(input.barList()).isEqualTo(List.of(new Bar(2L, "t1"), new Bar(3L, "t3")));
        assertThat(input.someDate()).isEqualTo(LocalDate.of(2025,6,1));
        assertThat(taskInput.attributes().get("p1")).isEqualTo("v1");
        assertThat(taskInput.attributes().get("p2")).isEqualTo("v2");
        String jsonPayload = JacksonUtil.toJson(taskInput);
        // Read the json tree and parse input separately to preserve type information
        TaskInput taskInput2 = JacksonUtil.fromJson(jsonPayload, TaskInput.class);
        assertThat(taskInput.taskId()).isEqualTo(taskInput2.taskId());
        // Parse the input field again with the correct type
        Foo input2 = JacksonUtil.fromJson(JacksonUtil.toJson(taskInput2.input()), Foo.class);
        assertThat(input.id()).isEqualTo(input2.id());
    }






}
