package top.ilovemyhome.dagtask.scheduler.application;

/**
 * Raised when a requested task template (or specific version) cannot be located.
 */
public class TemplateNotFoundException extends DomainException {

    public TemplateNotFoundException(String message) {
        super(message);
    }

    public TemplateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
