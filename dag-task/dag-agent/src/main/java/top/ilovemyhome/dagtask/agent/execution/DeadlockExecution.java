package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.util.Objects;

/**
 * Execution that intentionally creates a deadlock between two threads.
 * Used for testing scenarios where a task cannot be terminated by normal means.
 *
 * <p>Two worker threads each lock a different resource and then attempt to
 * acquire the other's lock. Because neither ever releases its own lock first,
 * both block forever, causing the main execution thread to hang on {@code join()}.</p>
 *
 * <p>Example input JSON:
 * <pre>{"description":"test deadlock"}</pre></p>
 */
public class DeadlockExecution implements TaskExecution {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        Param param = input.input() == null ? null : JacksonUtil.fromJson(input.input(), Param.class);
        logger.info("Start deadlock task taskId={}, param={}", taskId, param);

        final Object lockA = new Object();
        final Object lockB = new Object();

        Thread threadA = new Thread(() -> {
            synchronized (lockA) {
                logger.info("[Deadlock] Thread-A acquired lock-A, waiting for lock-B");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("[Deadlock] Thread-A interrupted while sleeping");
                    return;
                }
                synchronized (lockB) {
                    logger.info("[Deadlock] Thread-A acquired lock-B — this should never print");
                }
            }
        }, "deadlock-thread-a-" + taskId);

        Thread threadB = new Thread(() -> {
            synchronized (lockB) {
                logger.info("[Deadlock] Thread-B acquired lock-B, waiting for lock-A");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("[Deadlock] Thread-B interrupted while sleeping");
                    return;
                }
                synchronized (lockA) {
                    logger.info("[Deadlock] Thread-B acquired lock-A — this should never print");
                }
            }
        }, "deadlock-thread-b-" + taskId);

        threadA.start();
        threadB.start();

        try {
            threadA.join();
            threadB.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("TaskId={} interrupted while waiting for deadlock threads", taskId);
            return TaskOutput.fail(taskId, null, "Interrupted while waiting for deadlock threads");
        }

        logger.info("TaskId={} completed — this should never print", taskId);
        return TaskOutput.success(taskId, "OK");
    }

    /**
     * Input parameter DTO for DeadlockExecution.
     *
     * @param description optional description for the test run
     */
    public record Param(String description) {
        public Param {
            if (description == null || description.isBlank()) {
                description = "deadlock-test";
            }
        }
    }
}
