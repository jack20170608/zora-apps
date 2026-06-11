package top.ilovemyhome.dagtask.scheduler.application;

/**
 * Base class for all domain-layer exceptions raised by application services.
 * Inbound adapters (web/admin) MUST translate these to protocol-specific
 * errors (HTTP status codes, gRPC status, etc.) at their boundary.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
