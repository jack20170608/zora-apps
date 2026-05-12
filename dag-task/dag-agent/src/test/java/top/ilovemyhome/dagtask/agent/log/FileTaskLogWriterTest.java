package top.ilovemyhome.dagtask.agent.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileTaskLogWriterTest {

    @Test
    void testWriteAndRead(@TempDir Path tempDir) throws IOException {
        String logDir = tempDir.toString();
        FileTaskLogWriter writer = new FileTaskLogWriter(1L, logDir);

        writer.info("Starting task");
        writer.stdout("hello");
        writer.stderr("warning");
        writer.warn("Something suspicious");
        writer.error("Something bad");
        writer.close();

        Path logFile = tempDir.resolve("1.log");
        assertThat(logFile).exists();
        String content = Files.readString(logFile);
        assertThat(content).contains("[INFO] Starting task");
        assertThat(content).contains("[STDOUT] hello");
        assertThat(content).contains("[STDERR] warning");
        assertThat(content).contains("[WARN] Something suspicious");
        assertThat(content).contains("[ERROR] Something bad");
    }

    @Test
    void testDirectoryAutoCreate(@TempDir Path tempDir) throws IOException {
        String nestedDir = tempDir.resolve("a").resolve("b").resolve("c").toString();
        FileTaskLogWriter writer = new FileTaskLogWriter(2L, nestedDir);
        writer.info("test");
        writer.close();

        Path logFile = Path.of(nestedDir).resolve("2.log");
        assertThat(logFile).exists();
    }
}
