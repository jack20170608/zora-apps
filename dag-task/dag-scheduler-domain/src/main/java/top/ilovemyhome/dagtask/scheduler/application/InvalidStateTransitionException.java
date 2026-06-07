package top.ilovemyhome.dagtask.scheduler.application;

/**
 * Raised when a caller attempts to drive a domain aggregate (task, order, agent)
 * through a state change that is illegal given its current status.
 */
public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
