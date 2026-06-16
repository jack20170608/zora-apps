package top.ilovemyhome.dagtask.scheduler.port.out;

/**
 * Raised by {@link AgentDispatcher} implementations when a task cannot be delivered
 * to the target agent (network failure, agent down, timeout, etc.). This is an
 * agent-communication concern, distinct from storage failures, so it does NOT
 * extend {@link PersistenceException}.
 */
public class AgentUnreachableException extends RuntimeException {

    public AgentUnreachableException(String message) {
        super(message);
    }

    public AgentUnreachableException(String message, Throwable cause) {
        super(message, cause);
    }
}
