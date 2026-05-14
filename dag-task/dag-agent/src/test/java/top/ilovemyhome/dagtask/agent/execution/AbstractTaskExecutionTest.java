package top.ilovemyhome.dagtask.agent.execution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractTaskExecutionTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateLogFile_WithNameAndTimestamp_WhenTaskLogDirProvided() throws IOException {
        TestExecution execution = new TestExecution();
        String taskLogDir = tempDir.toString();
        TaskInput input = TaskInput.of(1L, "test-task", "{}", Map.of("taskLogDir", taskLogDir));

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        Path logFile = findSingleLogFile(tempDir, "test-task-1-");
        assertThat(logFile).exists();
        String content = Files.readString(logFile);
        assertThat(content).contains("Hello from task 1");
        assertThat(content).contains("Error line");
    }

    @Test
    void shouldCreateLogFile_WithoutName_WhenNameIsNull() throws IOException {
        TestExecution execution = new TestExecution();
        String taskLogDir = tempDir.toString();
        TaskInput input = TaskInput.of(2L, null, "{}", Map.of("taskLogDir", taskLogDir));

        execution.execute(input);

        Path logFile = findSingleLogFile(tempDir, "2-");
        assertThat(logFile).exists();
        String content = Files.readString(logFile);
        assertThat(content).contains("Hello from task 2");
    }

    @Test
    void shouldNotCreateLogFile_WhenNoTaskLogDir() throws IOException {
        TestExecution execution = new TestExecution();
        TaskInput input = TaskInput.of(3L, "task3", "{}", null);

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        assertThat(Files.list(tempDir)).isEmpty();
    }

    @Test
    void shouldNotCreateLogFile_WhenAttributesEmpty() throws IOException {
        TestExecution execution = new TestExecution();
        TaskInput input = TaskInput.of(4L, "task4", "{}", Map.of());

        TaskOutput output = execution.execute(input);

        assertThat(output.isSuccess()).isTrue();
        assertThat(Files.list(tempDir)).isEmpty();
    }

    @Test
    void shouldCleanupLogger_WhenDoExecuteThrows() throws IOException {
        ThrowingExecution execution = new ThrowingExecution();
        String taskLogDir = tempDir.toString();
        TaskInput input = TaskInput.of(5L, "task5", "{}", Map.of("taskLogDir", taskLogDir));

        assertThatThrownBy(() -> execution.execute(input))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");

        Path logFile = findSingleLogFile(tempDir, "task5-5-");
        assertThat(logFile).exists();
        String content = Files.readString(logFile);
        assertThat(content).contains("Before throw");
    }

    @Test
    void shouldCreateNewFile_WhenSameTaskIdExecutedTwice() throws IOException {
        TestExecution execution = new TestExecution();
        String taskLogDir = tempDir.toString();
        TaskInput input = TaskInput.of(6L, "task6", "{}", Map.of("taskLogDir", taskLogDir));

        execution.execute(input);
        execution.execute(input);

        // Two executions should create two files (different timestamps)
        try (var stream = Files.list(tempDir)) {
            long count = stream.filter(p -> p.getFileName().toString().startsWith("task6-6-")).count();
            assertThat(count).isEqualTo(2);
        }
    }

    @Test
    void shouldUseCustomPattern_WhenProvidedInAttributes() throws IOException {
        PatternTestExecution execution = new PatternTestExecution();
        String taskLogDir = tempDir.toString();
        String customPattern = "%msg%n";
        TaskInput input = TaskInput.of(7L, "task7", "{}", Map.of(
            "taskLogDir", taskLogDir,
            "taskLogPattern", customPattern
        ));

        execution.execute(input);

        Path logFile = findSingleLogFile(tempDir, "task7-7-");
        String content = Files.readString(logFile);
        assertThat(content).startsWith("Custom pattern message");
    }

    private Path findSingleLogFile(Path dir, String prefix) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().startsWith(prefix))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No log file with prefix " + prefix + " found in " + dir));
        }
    }

    private static class TestExecution extends AbstractTaskExecution {
        @Override
        protected TaskOutput doExecute(TaskInput input) {
            logger.info("Hello from task {}", input.taskId());
            logger.error("Error line");
            return TaskOutput.success(input.taskId(), "ok");
        }
    }

    private static class ThrowingExecution extends AbstractTaskExecution {
        @Override
        protected TaskOutput doExecute(TaskInput input) {
            logger.info("Before throw");
            throw new RuntimeException("boom");
        }
    }

    private static class PatternTestExecution extends AbstractTaskExecution {
        @Override
        protected TaskOutput doExecute(TaskInput input) {
            logger.info("Custom pattern message");
            return TaskOutput.success(input.taskId(), "ok");
        }
    }
}
