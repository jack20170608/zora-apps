package top.ilovemyhome.dagtask.si.enums;

public enum OpsType {

    SUBMIT,
    KILL,
    HOLD,
    FREE,
    FORCE_OK,
    FORCE_NOK;

    private final String description;

    OpsType(String description){
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
