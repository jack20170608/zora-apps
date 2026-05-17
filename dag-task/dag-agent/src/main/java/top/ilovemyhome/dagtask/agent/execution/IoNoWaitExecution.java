package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Execution that waits for an I/O event in a non-blocking, cooperatively cancellable loop.
 * Opens a {@link ServerSocketChannel} on a random available port and polls
 * for an incoming connection using a {@link Selector} with a short timeout.
 *
 * <p>Because the channel is non-blocking, the loop periodically calls
 * {@link #checkInterruption(Long)} so that a {@code kill} request can
 * abort the wait by closing the underlying channel via try-with-resources.
 * This contrasts with the classic blocking {@code ServerSocket.accept()},
 * which ignores {@link Thread#interrupt()}.
 *
 * <p>Example input JSON:
 * <pre>{"port":0,"description":"io non-wait test"}</pre>
 * A {@code port} of {@code 0} lets the OS pick an available ephemeral port.
 */
public class IoNoWaitExecution implements TaskExecution {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        Param param = input.getInputAs(Param.class);
        logger.info("Start I/O non-wait task taskId={}, param={}", taskId, param);

        int port = param != null && param.port() > 0 ? param.port() : 0;
        try (Selector selector = Selector.open();
             ServerSocketChannel serverChannel = ServerSocketChannel.open()) {

            serverChannel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            int actualPort = serverChannel.socket().getLocalPort();
            logger.info("TaskId={} waiting for connection on localhost:{} (non-blocking loop)",
                taskId, actualPort);

            while (true) {
                checkInterruption(taskId);

                int ready = selector.select(100);
                if (ready > 0) {
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        if (key.isAcceptable()) {
                            SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
                            if (client != null) {
                                logger.info("TaskId={} accepted connection — unexpected!", taskId);
                                client.close();
                                return TaskOutput.success(taskId, "OK");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("TaskId={} I/O non-wait ended with exception: {}", taskId, e.getMessage());
            return TaskOutput.fail(taskId, null, "I/O error: " + e.getMessage());
        }
    }

    /**
     * Input parameter DTO for IoNoWaitExecution.
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
                description = "io-non-wait-test";
            }
        }
    }
}
