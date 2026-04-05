package top.ilovemyhome.dagtask.core.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.enums.OrderType;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.persistence.TaskTemplateDao;

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

/**
 * Default implementation of {@link top.ilovemyhome.dagtask.si.TaskTemplateService} with database persistence.
 * <p>
 * Handles template version management, activation control, and instantiation
 * of concrete {@link TaskOrder} instances from templates.
 * </p>
 * <p>
 * The instantiation process:
 * <ol>
 *     <li>Load the template definition from database</li>
 *     <li>Apply parameter substitution into the DAG definition</li>
 *     <li>Create and persist a new {@link TaskOrder}</li>
 *     <li>Return the concrete task order ready for task creation</li>
 * </ol>
 * </p>
 */
public class DefaultTaskTemplateService implements top.ilovemyhome.dagtask.si.TaskTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskTemplateService.class);

    private final TaskTemplateDao taskTemplateDao;
    private final TaskOrderDao taskOrderDao;
    private final TaskRecordDao taskRecordDao;
    private final ObjectMapper objectMapper;

    /**
     * Pattern for parameter placeholders in template configuration: {{paramName}}
     */
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_\\-.]+)\\}\\}");

    /**
     * Creates a DefaultTaskTemplateService with required dependencies.
     *
     * @param taskTemplateDao DAO for template persistence
     * @param taskOrderDao DAO for task order persistence
     * @param taskRecordDao DAO for task record persistence
     * @param objectMapper JSON mapper for processing DAG definition
     */
    public DefaultTaskTemplateService(TaskTemplateDao taskTemplateDao,
                                       TaskOrderDao taskOrderDao,
                                       TaskRecordDao taskRecordDao,
                                       ObjectMapper objectMapper) {
        this.taskTemplateDao = Objects.requireNonNull(taskTemplateDao, "taskTemplateDao must not be null");
        this.taskOrderDao = Objects.requireNonNull(taskOrderDao, "taskOrderDao must not be null");
        this.taskRecordDao = Objects.requireNonNull(taskRecordDao, "taskRecordDao must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public boolean createTemplate(TaskTemplate template, boolean setActive) {
        if (template == null || template.getTemplateKey().isBlank() || template.getVersion().isBlank()) {
            logger.warn("Cannot create template: missing required fields (key or version)");
            return false;
        }

        if (taskTemplateDao.exists(template.getTemplateKey(), template.getVersion())) {
            logger.warn("Cannot create template: version already exists for key [{}], version [{}]",
                template.getTemplateKey(), template.getVersion());
            return false;
        }

        taskTemplateDao.create(template);

        if (setActive) {
            // Deactivate all other versions of this template
            taskTemplateDao.deactivateOtherVersions(template.getTemplateKey(), template.getVersion());
        }

        logger.info("Created new template version: key=[{}], version=[{}], name=[{}], setActive=[{}]",
            template.getTemplateKey(), template.getVersion(), template.getTemplateName(), setActive);
        return true;
    }

    @Override
    public boolean updateTemplate(TaskTemplate template) {
        if (template == null || template.getTemplateKey().isBlank() || template.getVersion().isBlank()) {
            logger.warn("Cannot update template: missing required fields (key or version)");
            return false;
        }

        Optional<TaskTemplate> existing = taskTemplateDao.findByKeyAndVersion(
            template.getTemplateKey(), template.getVersion());
        if (existing.isEmpty()) {
            logger.warn("Cannot update template: not found key=[{}], version=[{}]",
                template.getTemplateKey(), template.getVersion());
            return false;
        }

        template.incrementVersionSeq();
        taskTemplateDao.update(template.getId(), template);
        logger.info("Updated template version: key=[{}], version=[{}], new sequence=[{}]",
            template.getTemplateKey(), template.getVersion(), template.getVersionSeq());
        return true;
    }

    @Override
    public boolean deactivateVersion(String templateKey, String version) {
        if (templateKey.isBlank() || version.isBlank()) {
            logger.warn("Cannot deactivate: missing templateKey or version");
            return false;
        }

        if (!taskTemplateDao.exists(templateKey, version)) {
            logger.warn("Cannot deactivate: template not found key=[{}], version=[{}]",
                templateKey, version);
            return false;
        }

        taskTemplateDao.deactivateVersion(templateKey, version);
        logger.info("Deactivated template version: key=[{}], version=[{}]", templateKey, version);
        return true;
    }

    @Override
    public boolean deleteVersion(String templateKey, String version) {
        if (templateKey.isBlank() || version.isBlank()) {
            logger.warn("Cannot delete: missing templateKey or version");
            return false;
        }

        if (!taskTemplateDao.exists(templateKey, version)) {
            logger.warn("Cannot delete: template not found key=[{}], version=[{}]",
                templateKey, version);
            return false;
        }

        taskTemplateDao.deleteByKeyAndVersion(templateKey, version);
        logger.info("Deleted template version: key=[{}], version=[{}]", templateKey, version);
        return true;
    }

    @Override
    public List<TaskTemplate> getVersions(String templateKey) {
        return taskTemplateDao.findByTemplateKey(templateKey);
    }

    @Override
    public Optional<TaskTemplate> getActive(String templateKey) {
        return taskTemplateDao.findActiveByTemplateKey(templateKey);
    }

    @Override
    public Optional<TaskTemplate> getByVersion(String templateKey, String version) {
        return taskTemplateDao.findByKeyAndVersion(templateKey, version);
    }

    @Override
    public List<TaskTemplate> getAllActive() {
        return taskTemplateDao.findAllActive();
    }

    @Override
    public List<TaskTemplate> getAll() {
        return taskTemplateDao.findAll();
    }

    @Override
    public Optional<TaskOrder> instantiateFromTemplate(String templateKey, String orderKey,
            String orderName, Map<String, String> parameters) {
        Optional<TaskTemplate> templateOpt = getActive(templateKey);
        if (templateOpt.isEmpty()) {
            logger.warn("Cannot instantiate: no active template found for key [{}]", templateKey);
            return Optional.empty();
        }
        return instantiateFromTemplate(templateKey, templateOpt.get().getVersion(),
            orderKey, orderName, parameters);
    }

    @Override
    public Optional<TaskOrder> instantiateFromTemplate(String templateKey, String version,
            String orderKey, String orderName, Map<String, String> parameters) {
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

        Optional<TaskTemplate> templateOpt = getByVersion(templateKey, version);
        if (templateOpt.isEmpty()) {
            logger.warn("Cannot instantiate: template not found key=[{}], version=[{}]",
                templateKey, version);
            return Optional.empty();
        }

        TaskTemplate template = templateOpt.get();

        // Merge provided parameters with defaults from parameter schema
        Map<String, String> resolvedParameters = resolveParameters(template, parameters);

        // Apply parameter substitution to template attributes
        Map<String, String> attributes = new HashMap<>();
        attributes.put("sourceTemplateKey", templateKey);
        attributes.put("sourceTemplateVersion", version);
        if (resolvedParameters != null) {
            attributes.putAll(resolvedParameters);
        }

        // Create the concrete TaskOrder
        TaskOrder order = TaskOrder.builder()
            .withKey(orderKey)
            .withName(orderName)
            .withOrderType(OrderType.INSTANTIATED_FROM_TEMPLATE)
            .withAttributes(attributes)
            .build();

        taskOrderDao.create(order);

        // Parse DAG definition and create all task records
        try {
            createTasksFromDagDefinition(template.getDagDefinition(), orderKey, resolvedParameters);
            logger.info("Created all task records from template DAG definition for order [{}]", orderKey);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse DAG definition from template: key=[{}], version=[{}]",
                templateKey, version, e);
            return Optional.empty();
        }

        logger.info("Instantiated new task order from template: templateKey=[{}], templateVersion=[{}], " +
            "orderKey=[{}], orderName=[{}]", templateKey, version, orderKey, orderName);

        return Optional.of(order);
    }

    /**
     * Parses the DAG definition JSON from the template, applies parameter substitution,
     * and creates all TaskRecord entities in the database with proper dependency relationships.
     *
     * @param dagDefinitionJson the JSON DAG definition from the template
     * @param orderKey the parent order key that all tasks belong to
     * @param parameters resolved parameters for substitution
     * @throws JsonProcessingException if the JSON is invalid
     */
    private void createTasksFromDagDefinition(String dagDefinitionJson, String orderKey,
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

    /**
     * Builds a TaskRecord from a JSON task node in the DAG definition,
     * applying parameter substitution to all string fields.
     *
     * @param taskNode the JSON node describing the task
     * @param orderKey the parent order key
     * @param parameters resolved parameters for substitution
     * @return the built TaskRecord and metadata about dependencies
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
     *
     * @param input the input string with placeholders
     * @param parameters resolved parameter map
     * @return the string with placeholders replaced
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

    /**
     * Gets a required text field from a JSON node.
     */
    private String getRequiredText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asText() : "";
    }

    /**
     * Gets an optional text field from a JSON node.
     */
    private String getOptionalText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asText() : null;
    }

    /**
     * Gets an optional boolean field from a JSON node with default value.
     */
    private boolean getOptionalBoolean(JsonNode node, String fieldName, boolean defaultValue) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asBoolean() : defaultValue;
    }

    /**
     * Gets an optional integer field from a JSON node.
     */
    private Integer getOptionalInteger(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && field.isInt() ? field.asInt() : null;
    }

    /**
     * Internal record to hold task build information during the two-pass creation process.
     */
    private record TaskBuildInfo(TaskRecord taskRecord, String taskName, Set<String> dependencies) {}

    /**
     * Resolves parameters by merging user-provided values with defaults from the template's
     * parameter schema.
     *
     * @param template the template to resolve parameters for
     * @param userParameters the user-provided parameter values
     * @return the merged parameter map with defaults applied
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
}
