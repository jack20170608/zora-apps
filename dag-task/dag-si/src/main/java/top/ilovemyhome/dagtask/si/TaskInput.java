package top.ilovemyhome.dagtask.si;

import java.util.Map;

public record TaskInput(Long taskId, String name, String input, Map<String, String> attributes) {

    public static TaskInput of(Long taskId, String name, String input, Map<String, String> attributes) {
        return new TaskInput(taskId, name, input, attributes);
    }
}//:~)
