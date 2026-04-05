package top.ilovemyhome.dagtask.si;

public class ResEntityHelper {

    private ResEntityHelper() {}

    public static <T> ResEntity<T> ok(T data) {
        return new ResEntity<>(200, "OK", data);
    }

    public static <T> ResEntity<T> ok(String message, T data) {
        return new ResEntity<>(200, message, data);
    }

    public static <T> ResEntity<T> ok(int code, String message, T data) {
        return new ResEntity<>(code, message, data);
    }

    public static <T> ResEntity<T> badRequest(T data) {
        return new ResEntity<>(400, "Bad Request", data);
    }
    public static <T> ResEntity<T> badRequest(String message, T data) {
        return new ResEntity<>(400, message, data);
    }
    public static <T> ResEntity<T> badRequest(int code, String message, T data) {
        return new ResEntity<>(code, message, data);
    }

    public static <T> ResEntity<T> unauthorized(T data) {
        return new ResEntity<>(401, "Unauthorized", data);
    }
    public static <T> ResEntity<T> unauthorized(String message, T data) {
        return new ResEntity<>(401, message, data);
    }
    public static <T> ResEntity<T> unauthorized(int code, String message, T data) {
        return new ResEntity<>(code, message, data);
    }

    public static <T> ResEntity<T> forbidden(T data) {
        return new ResEntity<>(403, "Forbidden", data);
    }

    public static <T> ResEntity<T> forbidden(String message, T data) {
        return new ResEntity<>(403, message, data);
    }

    public static <T> ResEntity<T> forbidden(int code, String message, T data) {
        return new ResEntity<>(code, message, data);
    }

    public static <T> ResEntity<T> notFound(T data) {
        return new ResEntity<>(404, "Not Found", data);
    }

    public static <T> ResEntity<T> notFound(String message, T data) {
        return new ResEntity<>(404, message, data);
    }

    public static <T> ResEntity<T> notFound(int code, String message, T data) {
        return new ResEntity<>(code, message, data);
    }

    public static <T> ResEntity<T> serverError(T data) {
        return new ResEntity<>(500, "Server Error", data);
    }

    public static <T> ResEntity<T> serverError(String message, T data) {
        return new ResEntity<>(500, message, data);
    }

    public static <T> ResEntity<T> serverError(int code, String message, T data) {
        return new ResEntity<>(code, message, data);
    }

}
