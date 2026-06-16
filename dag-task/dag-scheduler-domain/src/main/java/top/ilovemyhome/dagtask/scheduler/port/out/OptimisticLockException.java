package top.ilovemyhome.dagtask.scheduler.port.out;

/**
 * Raised by persistence adapters when a stale-write is detected (the underlying row
 * was modified by another transaction between read and write). Application services
 * may retry or surface a conflict response.
 */
public class OptimisticLockException extends PersistenceException {

    public OptimisticLockException(String message) {
        super(message);
    }

    public OptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
