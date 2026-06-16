package top.ilovemyhome.dagtask.scheduler.application;

/**
 * Raised when a caller tries to create a task order with a key that is already in use.
 */
public class OrderKeyAlreadyExistsException extends DomainException {

    public OrderKeyAlreadyExistsException(String message) {
        super(message);
    }

    public OrderKeyAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
