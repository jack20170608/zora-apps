package top.ilovemyhome.dagtask.si;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

/**
 * Represents the input data for a task execution.
 * <p>
 * The {@code input} field can be any object (String, Map, custom DTO, etc.).
 * Type-safe access methods are provided for common patterns:
 * <ul>
 *   <li>{@link #getInputAs(Class, ObjectMapper)} - convert input to a specific type</li>
 *   <li>{@link #getMap()} / {@link #isMap()} - treat input as a Map</li>
 *   <li>{@link #getString(String)}, {@link #getLong(String)}, {@link #getInt(String)}, {@link #getBoolean(String)} -
 *       type-safe field access when input is a Map</li>
 * </ul>
 */
public record TaskInput(Long taskId, Object input, Map<String, String> attributes) {

    public static TaskInput of(Long taskId, Object input, Map<String, String> attributes) {
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
        return mapper.convertValue(input, type);
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

    /**
     * Returns {@code true} if the input is a {@link Map}.
     */
    public boolean isMap() {
        return input instanceof Map;
    }

    /**
     * Returns the input as a {@code Map<String, Object>} if it is a Map,
     * otherwise returns {@code null}.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap() {
        if (input instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    /**
     * Gets a raw value from the input map by key.
     * Returns {@code null} if input is not a map or the key does not exist.
     *
     * @param key the map key
     * @return the raw value, or null
     */
    public Object get(String key) {
        Map<String, Object> map = getMap();
        return map == null ? null : map.get(key);
    }

    /**
     * Gets a {@link String} value from the input map by key.
     * Returns {@code null} if input is not a map, the key is missing,
     * or the value is not a String.
     *
     * @param key the map key
     * @return the String value, or null
     */
    public String getString(String key) {
        Object value = get(key);
        return value instanceof String s ? s : null;
    }

    /**
     * Gets a {@link Long} value from the input map by key.
     * Safely handles both {@link Integer} and {@link Long} numeric types.
     * Returns {@code null} if input is not a map, the key is missing,
     * or the value is not a number.
     *
     * @param key the map key
     * @return the Long value, or null
     */
    public Long getLong(String key) {
        Object value = get(key);
        if (value instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    /**
     * Gets an {@link Integer} value from the input map by key.
     * Safely handles both {@link Integer} and {@link Long} numeric types.
     * Returns {@code null} if input is not a map, the key is missing,
     * or the value is not a number.
     *
     * @param key the map key
     * @return the Integer value, or null
     */
    public Integer getInt(String key) {
        Object value = get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    /**
     * Gets a {@link Boolean} value from the input map by key.
     * Returns {@code null} if input is not a map, the key is missing,
     * or the value is not a Boolean.
     *
     * @param key the map key
     * @return the Boolean value, or null
     */
    public Boolean getBoolean(String key) {
        Object value = get(key);
        return value instanceof Boolean b ? b : null;
    }

    /**
     * Requires the input to be a map and the specified key to exist.
     * Throws {@link IllegalArgumentException} if the key is missing or input is not a map.
     *
     * @param key the map key
     * @return the raw value associated with the key
     * @throws IllegalArgumentException if the key is missing
     */
    public Object require(String key) {
        Map<String, Object> map = getMap();
        if (map == null) {
            throw new IllegalArgumentException("Task input is not a map, cannot require key '" + key + "'");
        }
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("Required key '" + key + "' is missing from task input");
        }
        return map.get(key);
    }
}
