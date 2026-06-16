package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Execution that creates an interruptible deadlock between two threads.
 * Unlike {@link DeadlockExecution} which uses {@code synchronized} (ignores
 * {@link Thread#interrupt()}), this implementation uses
 * {@link ReentrantLock#lockInterruptibly()} so that a {@code kill} request
 * can break the deadlock by interrupting the waiting threads.
 *
 * <p>Two worker threads each lock a different {@link ReentrantLock} and then
 * attempt to acquire the other's lock via {@code lockInterruptibly()}.
 * When interrupted, the waiting thread throws {@code InterruptedException},
 * releases its own lock, and exits — allowing the other thread to eventually
 * acquire the freed lock and complete.</p>
 *
 * <p>Example input JSON:
 * <pre>{"description":"interruptible deadlock test"}</pre></p>
 */
public class InterruptibleDeadlockExecution implements TaskExecution {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Initial sleep before trying to acquire the second lock, giving both
     * threads time to hold their first locks so the deadlock forms.
     */
    private static final long DEADLOCK_FORM_DELAY_MS = 200;

    @Override
    public TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        Param param = input.input() == null ? null : JacksonUtil.fromJson(input.input(), Param.class);
        logger.info("Start interruptible deadlock task taskId={}, param={}", taskId, param);

        final ReentrantLock lockA = new ReentrantLock();
        final ReentrantLock lockB = new ReentrantLock();

        Thread threadA = new Thread(() -> runThreadA(taskId, lockA, lockB), "int-deadlock-a-" + taskId);
        Thread threadB = new Thread(() -> runThreadB(taskId, lockA, lockB), "int-deadlock-b-" + taskId);

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

        logger.info("TaskId={} completed — threads resolved or were interrupted", taskId);
        return TaskOutput.success(taskId, "OK");
    }

    private void runThreadA(Long taskId, ReentrantLock lockA, ReentrantLock lockB) {
        lockA.lock();
        logger.info("[Deadlock-A] Thread-A acquired lock-A, waiting for lock-B");
        try {
            Thread.sleep(DEADLOCK_FORM_DELAY_MS);
            lockB.lockInterruptibly();
            try {
                logger.info("[Deadlock-A] Thread-A acquired lock-B — this should never print under normal conditions");
            } finally {
                lockB.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("[Deadlock-A] Thread-A interrupted while waiting for lock-B, releasing lock-A");
        } finally {
            lockA.unlock();
        }
    }

    private void runThreadB(Long taskId, ReentrantLock lockA, ReentrantLock lockB) {
        lockB.lock();
        logger.info("[Deadlock-B] Thread-B acquired lock-B, waiting for lock-A");
        try {
            Thread.sleep(DEADLOCK_FORM_DELAY_MS);
            lockA.lockInterruptibly();
            try {
                logger.info("[Deadlock-B] Thread-B acquired lock-A — this should never print under normal conditions");
            } finally {
                lockA.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("[Deadlock-B] Thread-B interrupted while waiting for lock-A, releasing lock-B");
        } finally {
            lockB.unlock();
        }
    }

    /**
     * Input parameter DTO for InterruptibleDeadlockExecution.
     *
     * @param description optional description for the test run
     */
    public record Param(String description) {
        public Param {
            if (description == null || description.isBlank()) {
                description = "interruptible-deadlock-test";
            }
        }
    }
}
