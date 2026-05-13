package top.ilovemyhome.dagtask.agent.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class TaskLogAppenderTest {

    private TaskLogAppender appender;
    private InMemoryTaskLogWriter writer;

    @BeforeEach
    void setUp() {
        appender = new TaskLogAppender();
        appender.start();
        writer = new InMemoryTaskLogWriter();
        TaskLogContext.set(writer);
    }

    @AfterEach
    void tearDown() {
        TaskLogContext.clear();
        appender.stop();
    }

    @Test
    void testInfoLevelForwarded() {
        ILoggingEvent event = createEvent(Level.INFO, "hello info");
        appender.append(event);

        assertThat(writer.getInfos()).containsExactly("[test.logger] hello info");
        assertThat(writer.getWarns()).isEmpty();
        assertThat(writer.getErrors()).isEmpty();
    }

    @Test
    void testWarnLevelForwarded() {
        ILoggingEvent event = createEvent(Level.WARN, "hello warn");
        appender.append(event);

        assertThat(writer.getWarns()).containsExactly("[test.logger] hello warn");
        assertThat(writer.getInfos()).isEmpty();
        assertThat(writer.getErrors()).isEmpty();
    }

    @Test
    void testErrorLevelForwarded() {
        ILoggingEvent event = createEvent(Level.ERROR, "hello error");
        appender.append(event);

        assertThat(writer.getErrors()).containsExactly("[test.logger] hello error");
        assertThat(writer.getInfos()).isEmpty();
        assertThat(writer.getWarns()).isEmpty();
    }

    @Test
    void testDebugLevelIgnored() {
        ILoggingEvent event = createEvent(Level.DEBUG, "hello debug");
        appender.append(event);

        assertThat(writer.getInfos()).isEmpty();
        assertThat(writer.getWarns()).isEmpty();
        assertThat(writer.getErrors()).isEmpty();
    }

    @Test
    void testTraceLevelIgnored() {
        ILoggingEvent event = createEvent(Level.TRACE, "hello trace");
        appender.append(event);

        assertThat(writer.getInfos()).isEmpty();
        assertThat(writer.getWarns()).isEmpty();
        assertThat(writer.getErrors()).isEmpty();
    }

    @Test
    void testNoWriterBound() {
        TaskLogContext.clear();

        ILoggingEvent event = createEvent(Level.INFO, "no writer");
        appender.append(event);

        assertThat(writer.getInfos()).isEmpty();
    }

    @Test
    void testExceptionIncludedInError() {
        Exception ex = new RuntimeException("boom");
        ILoggingEvent event = createEvent(Level.ERROR, "failed", ex);
        appender.append(event);

        assertThat(writer.getErrors()).hasSize(1);
        String error = writer.getErrors().get(0);
        assertThat(error).startsWith("[test.logger] failed\n");
        assertThat(error).contains("RuntimeException: boom");
    }

    private ILoggingEvent createEvent(Level level, String message) {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("test.logger");
        return new LoggingEvent(
            ch.qos.logback.classic.Logger.class.getName(),
            logger,
            level,
            message,
            null,
            null
        );
    }

    private ILoggingEvent createEvent(Level level, String message, Throwable throwable) {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("test.logger");
        return new LoggingEvent(
            ch.qos.logback.classic.Logger.class.getName(),
            logger,
            level,
            message,
            throwable,
            null
        );
    }
}
