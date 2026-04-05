package top.ilovemyhome.dagtask.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.dag.DagHelper;
import top.ilovemyhome.dagtask.core.dag.DagNode;
import top.ilovemyhome.dagtask.si.DagConfigurationException;
import top.ilovemyhome.dagtask.si.TaskContext;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskFactory;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.TaskDagConfigLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link TaskDagConfigLoader} that loads DAG configurations from YAML files.
 */
public class TaskDagConfigLoaderImpl implements TaskDagConfigLoader {

    private final TaskContext taskContext;
    private final TaskFactory taskFactory;
    final ObjectMapper yamlMapper;

    public TaskDagConfigLoaderImpl(TaskContext taskContext, TaskFactory taskFactory) {
        this.taskContext = taskContext;
        this.taskFactory = taskFactory;
        this.yamlMapper = new YAMLMapper();
    }

    public TaskDagConfigLoaderImpl(TaskContext taskContext, TaskFactory taskFactory, ObjectMapper yamlMapper) {
        this.taskContext = taskContext;
        this.taskFactory = taskFactory;
        this.yamlMapper = yamlMapper;
    }

    @Override
    public TaskOrder loadFromClasspath(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new DagConfigurationException("Resource not found on classpath: " + resourcePath);
            }
            String yaml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return loadFromYaml(yaml);
        } catch (IOException e) {
            throw new DagConfigurationException("Failed to read resource: " + resourcePath, e);
        }
    }

    @Override
    public TaskOrder loadFromFile(File file) {
        try {
            return yamlMapper.readValue(file, TaskDagConfig.class)
                    .convertToTaskOrder(this::buildTaskRecords);
        } catch (IOException e) {
            throw new DagConfigurationException("Failed to read file: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public TaskOrder loadFromYaml(String yamlContent) {
        try {
            TaskDagConfig config = yamlMapper.readValue(yamlContent, TaskDagConfig.class);
            return config.convertToTaskOrder(this::buildTaskRecords);
        } catch (IOException e) {
            throw new DagConfigurationException("Failed to parse YAML content", e);
        }
    }

    List<TaskRecord> buildTaskRecords(TaskDagConfig config) {
        // Validate configuration first
        validateConfig(config);

        // Build a map of taskId -> TaskConfig for easy lookup
        Map<Integer, TaskConfig> taskConfigMap = config.getTasks().stream()
                .collect(Collectors.toMap(TaskConfig::getTaskId, tc -> tc));

        // Convert dependencies to successorIds: for each dependency (predecessor),
        // add current task to the predecessor's successor list
        Map<Integer, Set<Long>> successorMap = new HashMap<>();
        for (TaskConfig taskConfig : config.getTasks()) {
            long taskId = taskConfig.getTaskId().longValue();
            for (int depId : taskConfig.getDependencies()) {
                successorMap.computeIfAbsent(depId, k -> new HashSet<>()).add(taskId);
            }
        }

        // Create TaskRecord objects
        List<TaskRecord> records = new ArrayList<>();
        for (TaskConfig taskConfig : config.getTasks()) {
            TaskRecord record = buildTaskRecord(taskConfig, successorMap.getOrDefault(taskConfig.getTaskId(), Set.of()));
            records.add(record);
        }

        // Validate DAG structure using existing DagHelper
        List<DagNode> dagNodes = new ArrayList<>();
        for (TaskRecord record : records) {
            dagNodes.add(toDagNode(record));
        }
        try {
            List<String> taskPath = new ArrayList<>();
            DagHelper.visitDAG(dagNodes, taskPath);
            LOGGER.debug("DAG validation passed. Traversal path: {}", taskPath);
        } catch (IllegalArgumentException e) {
            throw new DagConfigurationException("DAG validation failed: " + e.getMessage(), e);
        }

        return records;
    }

    private DagNode toDagNode(TaskRecord record) {
        return new DagNode(record.getId(), record.getName(),
                record.getSuccessorIds());
    }

    private TaskRecord buildTaskRecord(TaskConfig config, Set<Long> successorIds) {
        TaskRecord.Builder builder = TaskRecord.builder()
                .withId(config.getTaskId().longValue())
                .withName(config.getTaskName())
                .withExecutionKey(config.getExecutionClass())
                .withAsync(config.isAsync())
                .withDummy(config.isDummy())
                .withTimeout(config.getTimeout().longValue())
                .withTimeoutUnit(config.getTimeoutTimeUnit())
                .withSuccessorIds(successorIds);
        if (config.getDescription() != null) {
            builder.withDescription(config.getDescription());
        }
        return builder.build();
    }

    private void validateConfig(TaskDagConfig config) {
        List<TaskConfig> tasks = config.getTasks();

        // Check empty tasks
        if (tasks == null || tasks.isEmpty()) {
            throw new DagConfigurationException("Task list cannot be empty");
        }

        // Check for duplicate task IDs
        Set<Integer> seenIds = new HashSet<>();
        for (TaskConfig task : tasks) {
            if (seenIds.contains(task.getTaskId())) {
                throw new DagConfigurationException("Duplicate task ID found: " + task.getTaskId());
            }
            seenIds.add(task.getTaskId());
        }

        // Check that all dependencies exist and validate execution class
        for (TaskConfig task : tasks) {
            for (int depId : task.getDependencies()) {
                if (!seenIds.contains(depId)) {
                    throw new DagConfigurationException(
                            String.format("Task %d (%s) depends on non-existent task ID %d",
                                    task.getTaskId(), task.getTaskName(), depId));
                }
            }

            // Validate execution class exists and implements TaskExecution
            validateExecutionClass(task);
        }

        // Check required fields
        if (config.getOrderKey() == null || config.getOrderKey().isBlank()) {
            throw new DagConfigurationException("orderKey cannot be null or empty");
        }
        if (config.getOrderName() == null || config.getOrderName().isBlank()) {
            throw new DagConfigurationException("orderName cannot be null or empty");
        }
    }

    private void validateExecutionClass(TaskConfig task) {
        String className = task.getExecutionClass();
        if (className == null || className.isBlank()) {
            throw new DagConfigurationException(
                    String.format("Task %d (%s) executionClass cannot be null or empty",
                            task.getTaskId(), task.getTaskName()));
        }
        try {
            Class<?> clazz = Class.forName(className);
            if (!TaskExecution.class.isAssignableFrom(clazz)) {
                throw new DagConfigurationException(
                        String.format("Task %d (%s) executionClass %s does not implement TaskExecution interface",
                                task.getTaskId(), task.getTaskName(), className));
            }
        } catch (ClassNotFoundException e) {
            throw new DagConfigurationException(
                    String.format("Task %d (%s) executionClass %s not found in classpath",
                            task.getTaskId(), task.getTaskName(), className), e);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskDagConfigLoaderImpl.class);
}
