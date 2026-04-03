package top.ilovemyhome.dagtask.si;

/**
 * Exception thrown when there is an error loading or validating a DAG configuration.
 */
public class DagConfigurationException extends RuntimeException {

    public DagConfigurationException(String message) {
        super(message);
    }

    public DagConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
