package top.ilovemyhome.hosthelper.muserver.sso;

public final class SsoAuthenticationException extends RuntimeException {

    public SsoAuthenticationException(String message) {
        super(message);
    }

    public SsoAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
