package top.ilovemyhome.dagtask.scheduler.application;

/**
 * Raised when a requested DAG / task order cannot be located.
 */
public class DagNotFoundException extends DomainException {

    public DagNotFoundException(String message) {
        super(message);
    }

    public DagNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
