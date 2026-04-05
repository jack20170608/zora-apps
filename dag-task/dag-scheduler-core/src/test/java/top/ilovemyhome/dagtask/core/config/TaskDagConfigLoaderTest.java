package top.ilovemyhome.dagtask.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.si.DagConfigurationException;
import top.ilovemyhome.dagtask.si.TaskContext;
import top.ilovemyhome.dagtask.si.TaskFactory;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.core.TestTaskExecution;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TaskDagConfigLoaderTest {

    private TaskContext taskContext;
    private TaskFactory taskFactory;
    private TaskDagConfigLoaderImpl loader;

    @BeforeEach
    void setUp() {
        taskContext = mock(TaskContext.class);
        taskFactory = new FooTaskFactoryImpl();
        loader = new TaskDagConfigLoaderImpl(taskContext, taskFactory);
    }

    @Test
    void loadValidDagFromYaml_success() throws Exception {
        String yaml = """
                orderKey: test-workflow
                orderName: Test Workflow
                tasks:
                  - taskId: 1
                    taskName: First Task
                    executionClass: top.ilovemyhome.dagtask.core.TestTaskExecution
                    executionType: sync
                    dependencies: []
                    timeout: 60
                  - taskId: 2
                    taskName: Second Task
                    executionClass: top.ilovemyhome.dagtask.core.TestTaskExecution
                    executionType: async
                    dependencies: [1]
                    timeout: 120
                """;

        TaskOrder order = loader.loadFromYaml(yaml);

        assertThat(order).isNotNull();
        assertThat(order.getKey()).isEqualTo("test-workflow");
        assertThat(order.getName()).isEqualTo("Test Workflow");

        // The loader returns the TaskOrder, but TaskRecords are built by the function
        // We need to build them ourselves to check the structure
        TaskDagConfig config = loader.yamlMapper.readValue(yaml, TaskDagConfig.class);
        List<TaskRecord> records = loader.buildTaskRecords(config);

        assertThat(records).hasSize(2);

        TaskRecord task1 = records.stream().filter(r -> r.getId() == 1L).findFirst().orElseThrow();
        TaskRecord task2 = records.stream().filter(r -> r.getId() == 2L).findFirst().orElseThrow();

        assertThat(task1.getSuccessorIds()).contains(2L);
        assertThat(task2.getSuccessorIds()).isEmpty();
        assertThat(task1.isAsync()).isFalse();
        assertThat(task2.isAsync()).isTrue();
        assertThat(task1.getExecutionKey()).isEqualTo("top.ilovemyhome.dagtask.core.TestTaskExecution");
    }

    @Test
    void loadCyclicDag_throwsException() {
        String yaml = """
                orderKey: cyclic-workflow
                orderName: Cyclic Workflow
                tasks:
                  - taskId: 1
                    taskName: Task 1
                    executionClass: top.ilovemyhome.dagtask.core.TestTaskExecution
                    dependencies: []
                  - taskId: 2
                    taskName: Task 2
                    executionClass: top.ilovemyhome.dagtask.core.TestTaskExecution
                    dependencies: [1]
                  - taskId: 3
                    taskName: Task 3
                    executionClass: top.ilovemyhome.dagtask.core.TestTaskExecution
                    dependencies: [2]
                  - taskId: 1
                    taskName: Task 1
                    executionClass: top.ilovemyhome.dagtask.core.TestTaskExecution
                    dependencies: [3]
                """;

        assertThatThrownBy(() -> loader.loadFromYaml(yaml))
                .isInstanceOf(DagConfigurationException.class)
                .hasMessageContaining("Duplicate task ID found: 1");
    }

    @Test
    void duplicateTaskIds_throwsException() {
        String yaml = """
                orderKey: duplicate-ids
                orderName: Duplicate IDs
                tasks:
                  - taskId: 1
                    taskName: Task 1
                    executionClass: top.ilovemyhome.dagtask.core.TestTaskExecution
                    dependencies: []
                  - taskId: 1
                    taskName: Task 2
                    executionClass: top.ilovemyhome.dagtask.core.TestTaskExecution
                    dependencies: []
                """;

        assertThatThrownBy(() -> loader.loadFromYaml(yaml))
                .isInstanceOf(DagConfigurationException.class)
                .hasMessageContaining("Duplicate task ID found");
    }

    @Test
    void missingDependency_throwsException() {
        String yaml = """
                orderKey: missing-dep
                orderName: Missing Dependency
                tasks:
                  - taskId: 1
                    taskName: Task 1
                    executionClass: top.ilovemyhome.dagtask.core.TestTaskExecution
                    dependencies: [999]
                """;

        assertThatThrownBy(() -> loader.loadFromYaml(yaml))
                .isInstanceOf(DagConfigurationException.class)
                .hasMessageContaining("depends on non-existent task ID");
    }

    @Test
    void emptyTaskList_throwsException() {
        String yaml = """
                orderKey: empty
                orderName: Empty
                tasks: []
                """;

        assertThatThrownBy(() -> loader.loadFromYaml(yaml))
                .isInstanceOf(DagConfigurationException.class)
                .hasMessageContaining("Task list cannot be empty");
    }

    @Test
    void missingOrderKey_throwsException() {
        String yaml = """
                orderName: Missing Order Key
                tasks:
                  - taskId: 1
                    taskName: Task 1
                    executionClass: top.ilovemyhome.dagtask.core.TestTaskExecution
                    dependencies: []
                """;

        assertThatThrownBy(() -> loader.loadFromYaml(yaml))
                .isInstanceOf(DagConfigurationException.class)
                .hasMessageContaining("orderKey cannot be null or empty");
    }

    @Test
    void executionClassDoesNotImplementTaskExecution_throwsException() {
        String yaml = """
                orderKey: bad-class
                orderName: Bad Class
                tasks:
                  - taskId: 1
                    taskName: Task 1
                    executionClass: java.lang.String
                    dependencies: []
                """;

        assertThatThrownBy(() -> loader.loadFromYaml(yaml))
                .isInstanceOf(DagConfigurationException.class)
                .hasMessageContaining("does not implement TaskExecution interface");
    }

    @Test
    void executionClassNotFound_throwsException() {
        String yaml = """
                orderKey: missing-class
                orderName: Missing Class
                tasks:
                  - taskId: 1
                    taskName: Task 1
                    executionClass: not.found.Class
                    dependencies: []
                """;

        assertThatThrownBy(() -> loader.loadFromYaml(yaml))
                .isInstanceOf(DagConfigurationException.class)
                .hasMessageContaining("not found in classpath");
    }

    static class FooTaskFactoryImpl implements TaskFactory {
        // Use default implementation from interface
    }
}
