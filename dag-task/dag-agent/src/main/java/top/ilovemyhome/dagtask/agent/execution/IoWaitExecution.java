package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Execution that blocks indefinitely on an I/O operation.
 * Opens a {@link ServerSocket} on a random available port and blocks
 * on {@link ServerSocket#accept()}, simulating a task waiting for an
 * external connection that never arrives.
 *
 * <p>Unlike {@link IoNoWaitExecution} (which polls in a non-blocking loop),
 * this task blocks on a kernel-level I/O syscall. The {@code accept()} call
 * is not interruptible via {@link Thread#interrupt()}, so a {@code kill}
 * request cannot stop this task by normal means. This makes it useful for
 * testing the engine's cancellation boundary.</p>
 *
 * <p>Example input JSON:
 * <pre>{"port":0,"description":"io wait test"}</pre>
 * A {@code port} of {@code 0} lets the OS pick an available ephemeral port.
 */
public class IoWaitExecution implements TaskExecution {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        Param param = input.getInputAs(Param.class);
        logger.info("Start I/O wait task taskId={}, param={}", taskId, param);

        int port = param != null && param.port() > 0 ? param.port() : 0;
        try (ServerSocket serverSocket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            int actualPort = serverSocket.getLocalPort();
            logger.info("TaskId={} waiting for connection on localhost:{} — this will block forever",
                taskId, actualPort);
            // Blocks until a connection is made (which never happens in normal test flow)
            Socket clientSocket = serverSocket.accept();
            logger.info("TaskId={} accepted connection on port {} — unexpected!",
                taskId, actualPort);
            clientSocket.close();
        } catch (IOException e) {
            logger.warn("TaskId={} I/O wait ended with exception: {}", taskId, e.getMessage());
            return TaskOutput.fail(taskId, null, "I/O error: " + e.getMessage());
        }

        logger.info("TaskId={} completed — this should never print under normal conditions", taskId);
        return TaskOutput.success(taskId, "OK");
    }

    /**
     * Input parameter DTO for IoWaitExecution.
     *
     * @param port        the port to bind (0 = OS picks an ephemeral port)
     * @param description optional description for the test run
     */
    public record Param(int port, String description) {
        public Param {
            if (port < 0) {
                port = 0;
            }
            if (description == null || description.isBlank()) {
                description = "io-wait-test";
            }
        }
    }
}
