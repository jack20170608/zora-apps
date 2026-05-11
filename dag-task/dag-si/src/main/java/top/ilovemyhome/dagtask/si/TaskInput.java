package top.ilovemyhome.dagtask.si;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.util.Map;
import java.util.Objects;

public record TaskInput(Long taskId, String input, Map<String, String> attributes) {

    public static TaskInput of(Long taskId, String input, Map<String, String> attributes) {
        return new TaskInput(taskId, input, attributes);
    }

    /**
     * Converts the input to the specified type using the given ObjectMapper.
     * <p>
     * If the input is already an instance of the target type, it is returned directly.
     * If the input is a Map (e.g. deserialized from JSON), it is converted via
     * {@link ObjectMapper#convertValue(Object, Class)}.
     *
     * @param type   the target class type, must not be null
     * @param mapper the ObjectMapper to use for conversion, must not be null
     * @return the converted input, or null if input itself is null
     * @throws IllegalArgumentException if conversion fails
     */
    public <T> T getInputAs(Class<T> type, ObjectMapper mapper) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        if (input == null) {
            return null;
        }
        if (type.isInstance(input)) {
            return type.cast(input);
        }
        return JacksonUtil.fromJson(input, type);
    }

    /**
     * Converts the input to the specified generic type using the given ObjectMapper
     * and a {@link TypeReference}. Useful for generic types such as {@code List<String>}.
     *
     * @param typeRef the target TypeReference, must not be null
     * @param mapper  the ObjectMapper to use for conversion, must not be null
     * @return the converted input, or null if input itself is null
     * @throws IllegalArgumentException if conversion fails
     */
    public <T> T getInputAs(TypeReference<T> typeRef, ObjectMapper mapper) {
        Objects.requireNonNull(typeRef, "typeRef must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        if (input == null) {
            return null;
        }
        return mapper.convertValue(input, typeRef);
    }
}
