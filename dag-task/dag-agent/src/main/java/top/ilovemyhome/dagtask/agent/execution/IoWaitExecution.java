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
import java.net.SocketTimeoutException;

/**
 * Execution that blocks on an I/O operation with cooperative cancellation.
 * Opens a {@link ServerSocket} on a random available port and blocks on
 * {@link ServerSocket#accept()}, but uses {@link ServerSocket#setSoTimeout(int)}
 * so that {@code accept()} times out periodically and the loop can check
 * {@link #checkInterruption(Long)}.
 *
 * <p>This demonstrates how a classic blocking I/O syscall (which ignores
 * {@link Thread#interrupt()}) can still be made responsive to kill requests
 * by adding short timeouts and checking the cancellation registry between
 * attempts. For a pure non-blocking alternative, see {@link IoNoWaitExecution}.
 *
 * <p>Example input JSON:
 * <pre>{"port":0,"description":"io wait test"}</pre>
 * A {@code port} of {@code 0} lets the OS pick an available ephemeral port.
 */
public class IoWaitExecution implements TaskExecution {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Accept timeout in milliseconds. Short enough to feel responsive when
     * killing, long enough to avoid excessive CPU wake-ups.
     */
    private static final int ACCEPT_TIMEOUT_MS = 1000;

    @Override
    public TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        Param param = input.getInputAs(Param.class);
        logger.info("Start I/O wait task taskId={}, param={}", taskId, param);

        int port = param != null && param.port() > 0 ? param.port() : 0;
        try (ServerSocket serverSocket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MS);
            int actualPort = serverSocket.getLocalPort();
            logger.info("TaskId={} waiting for connection on localhost:{} (accept timeout={}ms)",
                taskId, actualPort, ACCEPT_TIMEOUT_MS);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("TaskId={} accepted connection on port {} — unexpected!",
                        taskId, actualPort);
                    clientSocket.close();
                    return TaskOutput.success(taskId, "OK");
                } catch (SocketTimeoutException e) {
                    // accept() timed out — check if we were cancelled before retrying
                    checkInterruption(taskId);
                }
            }
        } catch (IOException e) {
            logger.warn("TaskId={} I/O wait ended with exception: {}", taskId, e.getMessage());
            return TaskOutput.fail(taskId, null, "I/O error: " + e.getMessage());
        }
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
