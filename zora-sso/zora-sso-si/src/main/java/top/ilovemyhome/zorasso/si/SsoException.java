package top.ilovemyhome.zorasso.si;

public final class SsoException extends RuntimeException {

    public enum Code {
        INVALID_REQUEST,
        UNKNOWN_CLIENT,
        INVALID_REDIRECT_URI,
        INVALID_TRANSACTION,
        LOGIN_REJECTED,
        RATE_LIMITED,
        INVALID_SESSION,
        INVALID_CODE,
        INVALID_CLIENT_CREDENTIALS
    }

    public SsoException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }

    private final Code code;
}
