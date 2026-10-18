package top.ilovemyhome.hosthelper.muserver.api.dto;

public record RestResponse<T>(boolean success, T data, String message) {

    public RestResponse {
        if (success && message != null) {
            throw new IllegalArgumentException("Successful responses must not contain an error message");
        }
        if (!success && (message == null || message.isBlank())) {
            throw new IllegalArgumentException("Failed responses require an error message");
        }
        if (!success && data != null) {
            throw new IllegalArgumentException("Failed responses must not contain data");
        }
    }

    public static <T> RestResponse<T> success(T data) {
        return new RestResponse<>(true, data, null);
    }

    public static <T> RestResponse<T> failure(String message) {
        return new RestResponse<>(false, null, message);
    }
}
