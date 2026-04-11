package top.ilovemyhome.dagtask.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.dag.DagHelper;
import top.ilovemyhome.dagtask.core.dag.DagNode;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.service.DagManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DagManagerImpl implements DagManager {

    private final TaskRecordDao taskRecordDao;
    private final TaskOrderDao taskOrderDao;
    private final top.ilovemyhome.dagtask.si.persistence.TaskTemplateDao taskTemplateDao;
    private final Jdbi jdbi;
    private final ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(DagManagerImpl.class);
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_\\-.]+)\\}\\}");

    public DagManagerImpl(Jdbi jdbi, TaskOrderDao taskOrderDao, TaskRecordDao taskRecordDao,
                          top.ilovemyhome.dagtask.si.persistence.TaskTemplateDao taskTemplateDao,
                          ObjectMapper objectMapper) {
        this.jdbi = jdbi;
        this.taskOrderDao = taskOrderDao;
        this.taskRecordDao = taskRecordDao;
        this.taskTemplateDao = taskTemplateDao;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Long> getNextTaskIds(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("The count must be greater than 0");
        }
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(taskRecordDao.getNextId());
        }
        return ids;
    }

    @Override
    public List<Long> createTasks(List<TaskRecord> records) {
        Objects.requireNonNull(records);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("The records list must not be empty");
        }

        // Check all tasks have the same order key
        String orderKey = records.getFirst().getOrderKey();
        if (StringUtils.isBlank(orderKey)) {
            throw new IllegalArgumentException("The record order key must not be empty");
        }
        records.forEach(r -> {
            if (!StringUtils.equals(orderKey, r.getOrderKey())) {
                throw new IllegalArgumentException("All records must have the same order key");
            }
        });

        return jdbi.inTransaction(h -> {
            Optional<TaskOrder> taskOrderOptional = taskOrderDao.findByKey(orderKey);
            final List<Long> result = new ArrayList<>();

            if (taskOrderOptional.isPresent()) {
                Map<Long, TaskRecord> existingMap = findTasksByOrderKey(orderKey).stream()
                    .collect(Collectors.toMap(TaskRecord::getId, r -> r));

                // Check for duplicates
                records.forEach(newRecord -> {
                    if (existingMap.containsKey(newRecord.getId())) {
                        throw new IllegalArgumentException("Already existing task record with id: " + newRecord.getId());
                    }
                });

                // Build complete DAG and check for cycles
                List<DagNode> dagNodes = existingMap.values().stream()
                    .map(this::toDagNode)
                    .collect(Collectors.toList());

                List<String> path = new ArrayList<>();
                logger.info("================================================");
                DagHelper.visitDAG(dagNodes, path);
                logger.info("{}", path);

                // Add new tasks and recheck
                records.forEach(newRecord -> {
                    dagNodes.add(toDagNode(newRecord));
                });

                path.clear();
                logger.info("After adding new tasks:");
                DagHelper.visitDAG(dagNodes, path);
                logger.info("{}", path);
                logger.info("================================================");

                // Create all new tasks
                records.forEach(newRecord -> {
                    result.add(taskRecordDao.create(newRecord));
                });
            } else {
                // No existing order, just create all tasks
                records.forEach(newRecord -> {
                    result.add(taskRecordDao.create(newRecord));
                });
                logger.info("Created new DAG for order: {}", orderKey);
            }

            return result;
        });
    }

    @Override
    public void createTasksFromDagDefinition(String dagDefinitionJson, String orderKey,
                                             Map<String, String> parameters) throws JsonProcessingException {

        JsonNode dagRoot = objectMapper.readTree(dagDefinitionJson);
        ArrayNode tasksNode = (ArrayNode) dagRoot.get("tasks");

        if (tasksNode == null || tasksNode.isEmpty()) {
            logger.warn("DAG definition is empty for order [{}], no tasks created", orderKey);
            return;
        }

        // First pass: create all tasks and store mappings from task name to generated ID
        List<TaskBuildInfo> taskInfos = new ArrayList<>();
        Map<String, Long> taskNameToId = new HashMap<>();

        for (JsonNode taskNode : tasksNode) {
            TaskBuildInfo buildInfo = buildTaskFromNode(taskNode, orderKey, parameters);
            TaskRecord task = buildInfo.taskRecord();
            taskRecordDao.create(task);
            taskNameToId.put(buildInfo.taskName(), task.getId());
            taskInfos.add(buildInfo);
            logger.debug("Created task '{}' for order '{}' with ID {}",
                buildInfo.taskName(), orderKey, task.getId());
        }

        // Second pass: update successor IDs based on dependency relationships
        for (TaskBuildInfo taskInfo : taskInfos) {
            Set<String> dependsOn = taskInfo.dependencies();
            if (dependsOn == null || dependsOn.isEmpty()) {
                continue;
            }

            // Convert dependency task names to their generated IDs and set as predecessors
            // A dependsOn B means B should run after A, so B is a successor of A
            Set<Long> successorIds = new HashSet<>();
            for (String dependencyName : dependsOn) {
                Long successorId = taskNameToId.get(dependencyName);
                if (successorId != null) {
                    successorIds.add(successorId);
                } else {
                    logger.warn("Dependency '{}' not found in DAG definition for task '{}'",
                        dependencyName, taskInfo.taskName());
                }
            }

            if (!successorIds.isEmpty()) {
                TaskRecord existingTask = taskRecordDao.findOne(taskNameToId.get(taskInfo.taskName())).get();
                // Update the existing task with successor IDs
                TaskRecord updatedTask = TaskRecord.builder()
                    .from(existingTask)
                    .withSuccessorIds(successorIds)
                    .build();
                taskRecordDao.update(existingTask.getId(), updatedTask);
            }
        }

        logger.info("Completed creating {} tasks from DAG definition for order '{}'",
            taskInfos.size(), orderKey);
    }

    @Override
    public List<TaskRecord> findTasksByOrderKey(String orderKey) {
        return taskRecordDao.findByOrderKey(orderKey);
    }

    @Override
    public boolean exists(String orderKey) {
        return taskRecordDao.isOrdered(orderKey);
    }

    @Override
    public boolean isAllSuccess(String orderKey) {
        return taskRecordDao.isSuccess(orderKey);
    }

    @Override
    public Map<TaskStatus, Long> countByStatus(String orderKey) {
        List<TaskRecord> allTasks = findTasksByOrderKey(orderKey);
        Map<TaskStatus, Long> result = new HashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            result.put(status, 0L);
        }
        for (TaskRecord task : allTasks) {
            result.computeIfPresent(task.getStatus(), (k, v) -> v + 1);
        }
        return result;
    }

    /**
     * Builds a TaskRecord from a JSON task node in the DAG definition,
     * applying parameter substitution to all string fields.
     */
    private TaskBuildInfo buildTaskFromNode(JsonNode taskNode, String orderKey,
                                            Map<String, String> parameters) {

        String taskName = getRequiredText(taskNode, "name");
        String resolvedName = substituteParameters(taskName, parameters);

        String description = getOptionalText(taskNode, "description");
        if (description != null) {
            description = substituteParameters(description, parameters);
        }

        String executionKey = getOptionalText(taskNode, "executionKey");
        if (executionKey != null) {
            executionKey = substituteParameters(executionKey, parameters);
        }

        boolean async = getOptionalBoolean(taskNode, "async", false);
        boolean dummy = getOptionalBoolean(taskNode, "dummy", false);

        String input = getOptionalText(taskNode, "input");
        if (input != null) {
            input = substituteParameters(input, parameters);
        }

        Integer timeout = getOptionalInteger(taskNode, "timeout");
        String timeoutUnitStr = getOptionalText(taskNode, "timeoutUnit");
        TimeUnit timeoutUnit = null;
        if (timeoutUnitStr != null && !timeoutUnitStr.isBlank()) {
            try {
                timeoutUnit = TimeUnit.valueOf(timeoutUnitStr);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid timeout unit '{}', ignoring", timeoutUnitStr);
            }
        }

        // Collect dependencies (task names that this task depends on)
        Set<String> dependencies = new HashSet<>();
        JsonNode dependsOnNode = taskNode.get("dependsOn");
        if (dependsOnNode != null && dependsOnNode.isArray()) {
            for (JsonNode dep : (ArrayNode) dependsOnNode) {
                dependencies.add(dep.asText());
            }
        }

        // Build the task record
        TaskRecord task = TaskRecord.builder()
            .withOrderKey(orderKey)
            .withName(resolvedName)
            .withDescription(description)
            .withExecutionKey(executionKey)
            .withAsync(async)
            .withDummy(dummy)
            .withInput(input)
            .withStatus(TaskStatus.INIT)
            .withTimeout(timeout != null ? timeout.longValue() : null)
            .withTimeoutUnit(timeoutUnit)
            .build();

        return new TaskBuildInfo(task, taskName, dependencies);
    }

    /**
     * Substitutes parameter placeholders in a string with resolved parameter values.
     * Placeholder format: {{parameterName}}
     */
    private String substituteParameters(String input, Map<String, String> parameters) {
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
                // Leave placeholder unchanged if parameter not provided
                matcher.appendReplacement(sb, Matcher.quoteReplacement("{{" + paramName + "}}"));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String getRequiredText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asText() : "";
    }

    private String getOptionalText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asText() : null;
    }

    private boolean getOptionalBoolean(JsonNode node, String fieldName, boolean defaultValue) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asBoolean() : defaultValue;
    }

    private Integer getOptionalInteger(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && field.isInt() ? field.asInt() : null;
    }

    @Override
    public Optional<TaskOrder> instantiateFromTemplate(String templateKey, String orderKey,
                                                      String orderName, Map<String, String> parameters) {
        return instantiateFromTemplate(templateKey, null, orderKey, orderName, parameters);
    }

    @Override
    public Optional<TaskOrder> instantiateFromTemplate(String templateKey, String version,
                                                      String orderKey, String orderName,
                                                      Map<String, String> parameters) {
        Objects.requireNonNull(orderKey, "orderKey must not be null");
        Objects.requireNonNull(orderName, "orderName must not be null");

        if (orderKey.isBlank()) {
            logger.warn("Cannot instantiate: orderKey is blank");
            return Optional.empty();
        }

        // Check if order key already exists
        if (taskOrderDao.findByKey(orderKey).isPresent()) {
            logger.warn("Cannot instantiate: task order with key [{}] already exists", orderKey);
            return Optional.empty();
        }

        // Get template from template service
        Optional<TaskTemplate> templateOpt;
        if (version == null || version.isBlank()) {
            templateOpt = taskTemplateDao.findActiveByTemplateKey(templateKey);
        } else {
            templateOpt = taskTemplateDao.findByKeyAndVersion(templateKey, version);
        }

        if (templateOpt.isEmpty()) {
            logger.warn("Cannot instantiate: template not found key=[{}], version=[{}]",
                templateKey, version);
            return Optional.empty();
        }

        TaskTemplate template = templateOpt.get();
        String actualVersion = version != null ? version : template.getVersion();

        // Merge provided parameters with defaults from parameter schema
        Map<String, String> resolvedParameters = resolveParameters(template, parameters);

        // Apply parameter substitution to template attributes
        Map<String, String> attributes = new HashMap<>();
        attributes.put("sourceTemplateKey", templateKey);
        attributes.put("sourceTemplateVersion", actualVersion);
        if (resolvedParameters != null) {
            attributes.putAll(resolvedParameters);
        }

        // Create the concrete TaskOrder
        TaskOrder order = TaskOrder.builder()
            .withKey(orderKey)
            .withName(orderName)
            .withOrderType(top.ilovemyhome.dagtask.si.enums.OrderType.INSTANTIATED_FROM_TEMPLATE)
            .withAttributes(attributes)
            .build();

        taskOrderDao.create(order);

        // Create all task records from DAG definition
        try {
            createTasksFromDagDefinition(template.getDagDefinition(), orderKey, resolvedParameters);
            logger.info("Created all task records from template DAG definition for order [{}]", orderKey);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse DAG definition from template: key=[{}], version=[{}]",
                templateKey, actualVersion, e);
            return Optional.empty();
        }

        logger.info("Instantiated new task order from template: templateKey=[{}], templateVersion=[{}], " +
            "orderKey=[{}], orderName=[{}]", templateKey, actualVersion, orderKey, orderName);

        return Optional.of(order);
    }

    /**
     * Resolves parameters by merging user-provided values with defaults from the template's
     * parameter schema.
     */
    private Map<String, String> resolveParameters(TaskTemplate template, Map<String, String> userParameters) {
        String schemaJson = template.getParameterSchema();
        if (schemaJson == null || schemaJson.isBlank()) {
            // No parameter schema defined - just return user parameters as-is
            return userParameters;
        }

        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            Map<String, String> resolved = new HashMap<>();

            // Apply defaults from schema, then override with user-provided values
            if (schemaNode.isObject()) {
                schemaNode.fields().forEachRemaining(entry -> {
                    String paramName = entry.getKey();
                    JsonNode paramDef = entry.getValue();
                    if (paramDef.has("default")) {
                        resolved.put(paramName, paramDef.get("default").asText());
                    }
                });
            }

            // Override with user-provided values
            if (userParameters != null) {
                resolved.putAll(userParameters);
            }

            return resolved;

        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse parameter schema for template key=[{}], version=[{}], " +
                "using user parameters as-is: {}", template.getTemplateKey(), template.getVersion(),
                e.getMessage());
            return userParameters;
        }
    }

    private DagNode toDagNode(TaskRecord record) {
        Objects.requireNonNull(record);
        return new DagNode(record.getId(), record.getName(),
            Objects.nonNull(record.getSuccessorIds()) ? Sets.newHashSet(record.getSuccessorIds()) : null);
    }

    /**
     * Internal record to hold task build information during the two-pass creation process.
     */
    private record TaskBuildInfo(TaskRecord taskRecord, String taskName, Set<String> dependencies) {}
}
