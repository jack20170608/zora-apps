package top.ilovemyhome.dagtask.si;

import java.util.Map;

public record TaskInput(Long taskId, Object input, Map<String, String> attributes) {

    public static TaskInput of(Long taskId, Object input, Map<String, String> attributes) {
        return new TaskInput(taskId, input, attributes);
    }

}
