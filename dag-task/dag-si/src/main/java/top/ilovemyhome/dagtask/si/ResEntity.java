package top.ilovemyhome.dagtask.si;

public record ResEntity<T>(int code
    , String message
    , long timestamp
    , T data) {

    public ResEntity(int code, String message, T data){
        this(code, message, System.currentTimeMillis(), data);
    }

}




