package top.ilovemyhome.dagtask.scheduler.port.out;

/**
 * Infrastructure-neutral persistence failure raised by outbound persistence adapters.
 * Adapters MUST translate vendor-specific exceptions (JdbiException, SQLException,
 * etc.) into this type at the adapter boundary.
 */
public class PersistenceException extends RuntimeException {

    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
