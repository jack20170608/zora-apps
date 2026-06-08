package top.ilovemyhome.dagtask.core.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import top.ilovemyhome.dagtask.scheduler.domain.dag.DagDefinition;
import top.ilovemyhome.dagtask.scheduler.domain.dag.DagDefinition.TaskDefinition;
import top.ilovemyhome.dagtask.scheduler.port.out.DagDefinitionParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Temporary adapter: domain {@link DagDefinitionParser} backed by Jackson.
 * Will be moved to a proper adapter module in step 3.
 */
public class JacksonDagDefinitionParser implements DagDefinitionParser {

    private final ObjectMapper objectMapper;

    public JacksonDagDefinitionParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public DagDefinition parseDag(String json) {
        Objects.requireNonNull(json, "json must not be null");
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode tasksNode = root.get("tasks");
            if (tasksNode == null || !tasksNode.isArray()) {
                return new DagDefinition(root.has("name") ? root.get("name").asText() : "", java.util.List.of());
            }

            java.util.List<TaskDefinition> tasks = new java.util.ArrayList<>();
            for (JsonNode taskNode : (ArrayNode) tasksNode) {
                Set<String> deps = new HashSet<>();
                JsonNode dependsOnNode = taskNode.get("dependsOn");
                if (dependsOnNode != null && dependsOnNode.isArray()) {
                    for (JsonNode dep : (ArrayNode) dependsOnNode) {
                        deps.add(dep.asText());
                    }
                }

                tasks.add(new TaskDefinition(
                    getText(taskNode, "name", ""),
                    getText(taskNode, "description", null),
                    getText(taskNode, "executionKey", null),
                    getBoolean(taskNode, "async", false),
                    getBoolean(taskNode, "dummy", false),
                    getText(taskNode, "input", null),
                    getInt(taskNode, "timeout"),
                    getText(taskNode, "timeoutUnit", null),
                    deps
                ));
            }
            return new DagDefinition(root.has("name") ? root.get("name").asText() : "", tasks);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse DAG definition", e);
        }
    }

    @Override
    public Map<String, String> parseParameterDefaults(String parameterSchemaJson) {
        if (parameterSchemaJson == null || parameterSchemaJson.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(parameterSchemaJson);
            Map<String, String> defaults = new HashMap<>();
            if (root.isObject()) {
                root.fields().forEachRemaining(entry -> {
                    JsonNode paramDef = entry.getValue();
                    if (paramDef.has("default")) {
                        defaults.put(entry.getKey(), paramDef.get("default").asText());
                    }
                });
            }
            return defaults;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse parameter schema", e);
        }
    }

    private static String getText(JsonNode node, String field, String defaultValue) {
        JsonNode f = node.get(field);
        return f != null && !f.isNull() ? f.asText() : defaultValue;
    }

    private static boolean getBoolean(JsonNode node, String field, boolean defaultValue) {
        JsonNode f = node.get(field);
        return f != null && !f.isNull() ? f.asBoolean() : defaultValue;
    }

    private static Integer getInt(JsonNode node, String field) {
        JsonNode f = node.get(field);
        return f != null && f.isInt() ? f.asInt() : null;
    }
}
