package top.ilovemyhome.dagtask.si.enums;

public enum OpsType {

    SUBMIT("Submit new task to execution queue"),
    KILL("Kill running/pending task"),
    HOLD("Suspend waiting task"),
    FREE("Release suspended task back to queue"),
    FORCE_OK("Force mark task as successful"),
    FORCE_NOK("Force mark task as failed");

    private final String description;

    OpsType(String description){
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
