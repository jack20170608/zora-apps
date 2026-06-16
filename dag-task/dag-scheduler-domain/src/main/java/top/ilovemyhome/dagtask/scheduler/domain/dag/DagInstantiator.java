package top.ilovemyhome.dagtask.scheduler.domain.dag;

import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-domain helper that creates a list of {@link TaskRecord}s from a parsed
 * {@link DagDefinition}.
 * <p>
 * Handles parameter substitution, dependency resolution, and task record
 * construction without any infrastructure dependency.
 * Used by both {@code SubmitDagRunService} and {@code InstantiateDagTemplateService}.
 * </p>
 */
public final class DagInstantiator {

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_\\-.]+)\\}\\}");

    private DagInstantiator() {
        // utility class
    }

    /**
     * Build task records from a typed DAG definition, assigning generated IDs
     * and resolving parameter placeholders.
     *
     * @param definition   the parsed DAG definition
     * @param orderKey     the order key
     * @param parameters   substitution parameters (name → value)
     * @param idGenerator  for generating task IDs
     * @return task records with IDs, status=INIT, and successor IDs set
     */
    public static List<TaskRecord> instantiate(DagDefinition definition,
                                               String orderKey,
                                               Map<String, String> parameters,
                                               LongSupplier nextTaskId) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(orderKey, "orderKey must not be null");
        Objects.requireNonNull(nextTaskId, "nextTaskId must not be null");

        if (definition.tasks() == null || definition.tasks().isEmpty()) {
            return List.of();
        }

        Map<String, Long> taskNameToId = new HashMap<>();
        List<BuildInfo> infos = new ArrayList<>();

        // First pass: assign IDs and build records without successor IDs
        for (DagDefinition.TaskDefinition taskDef : definition.tasks()) {
            long id = nextTaskId.getAsLong();
            taskNameToId.put(taskDef.key(), id);

            String resolvedName = substituteParameters(taskDef.key(), parameters);
            String resolvedDesc = substituteParameters(taskDef.description(), parameters);
            String resolvedExecKey = substituteParameters(taskDef.executionKey(), parameters);
            String resolvedInput = substituteParameters(taskDef.input(), parameters);

            TimeUnit timeoutUnit = null;
            if (taskDef.timeoutUnit() != null && !taskDef.timeoutUnit().isBlank()) {
                try {
                    timeoutUnit = TimeUnit.valueOf(taskDef.timeoutUnit());
                } catch (IllegalArgumentException e) {
                    // ignore invalid unit
                }
            }

            TaskRecord record = TaskRecord.builder()
                .withOrderKey(orderKey)
                .withName(resolvedName)
                .withDescription(resolvedDesc)
                .withExecutionKey(resolvedExecKey)
                .withAsync(taskDef.async())
                .withDummy(taskDef.dummy())
                .withInput(resolvedInput)
                .withStatus(TaskStatus.INIT)
                .withTimeout(taskDef.timeout() != null ? taskDef.timeout().longValue() : null)
                .withTimeoutUnit(timeoutUnit)
                .build();
            record.setId(id);

            infos.add(new BuildInfo(record, taskDef, taskDef.dependencies()));
        }

        // Second pass: resolve successor IDs from dependencies and rebuild records
        List<TaskRecord> result = new ArrayList<>(infos.size());
        for (BuildInfo info : infos) {
            if (info.dependencies() == null || info.dependencies().isEmpty()) {
                result.add(info.record());
                continue;
            }
            Set<Long> successorIds = new HashSet<>();
            for (String depName : info.dependencies()) {
                Long depId = taskNameToId.get(depName);
                if (depId != null) {
                    successorIds.add(depId);
                }
            }
            if (successorIds.isEmpty()) {
                result.add(info.record());
            } else {
                TaskRecord updated = TaskRecord.builder()
                    .from(info.record())
                    .withSuccessorIds(successorIds)
                    .build();
                updated.setId(info.record().getId());
                result.add(updated);
            }
        }
        return result;
    }

    /**
     * Substitute parameter placeholders ({{name}}) in a string with resolved values.
     */
    public static String substituteParameters(String input, Map<String, String> parameters) {
        if (input == null || parameters == null || parameters.isEmpty()) {
            return input;
        }
        Matcher matcher = PARAMETER_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String replacement = parameters.get(paramName);
            if (replacement != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement("{{" + paramName + "}}"));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private record BuildInfo(TaskRecord record, DagDefinition.TaskDefinition definition, Set<String> dependencies) {
    }
}