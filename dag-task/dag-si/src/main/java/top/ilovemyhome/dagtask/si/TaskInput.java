package top.ilovemyhome.dagtask.si;

import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.util.Map;
import java.util.Objects;

public record TaskInput(Long taskId, String name, String input, Map<String, String> attributes) {

    public static TaskInput of(Long taskId, String name, String input, Map<String, String> attributes) {
        return new TaskInput(taskId, name, input, attributes);
    }

    public <T> T getInputAs(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        if (input == null) {
            return null;
        }
        if (type.isInstance(input)) {
            return type.cast(input);
        }
        return JacksonUtil.fromJson(input, type);
    }
}//:~)
