package top.ilovemyhome.dagtask.scheduler.application;

/**
 * Raised when a requested task record cannot be located.
 */
public class TaskNotFoundException extends DomainException {

    public TaskNotFoundException(String message) {
        super(message);
    }

    public TaskNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
