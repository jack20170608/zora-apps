package top.ilovemyhome.dagtask.si;

public class ResEntityHelper {

    private ResEntityHelper() {}

    public static <T> ResEntity<T> ok(T data) {
        return new ResEntity<>(200, "OK", data);
    }

    public static <T> ResEntity<T> ok(int code, T data) {
        return new ResEntity<>(code, "OK", data);
    }

    public static <T> ResEntity<T> notFound(T data) {
        return new ResEntity<>(404, "Not Found", data);
    }

    public static <T> ResEntity<T> notFound(int code, T data) {
        return new ResEntity<>(code, "Not Found", data);
    }

    public static <T> ResEntity<T> serverError(T data) {
        return new ResEntity<>(500, "Server Error", data);
    }

    public static <T> ResEntity<T> serverError(int code, T data) {
        return new ResEntity<>(code, "Server Error", data);
    }

    public static <T> ResEntity<T> serverError(int code, String message, T data) {
        return new ResEntity<>(code, message, data);
    }

    public static <T> ResEntity<T> unauthorized(T data) {
        return new ResEntity<>(401, "Unauthorized", data);
    }

    public static <T> ResEntity<T> unauthorized(int code, T data) {
        return new ResEntity<>(code, "Unauthorized", data);
    }

    public static <T> ResEntity<T> forbidden(T data) {
        return new ResEntity<>(403, "Forbidden", data);
    }
    public static <T> ResEntity<T> forbidden(int code, T data) {
        return new ResEntity<>(code, "Forbidden", data);
    }
}
