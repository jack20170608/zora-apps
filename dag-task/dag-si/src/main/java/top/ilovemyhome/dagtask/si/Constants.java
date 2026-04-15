package top.ilovemyhome.dagtask.si;

public class Constants {

    private Constants() {}

    public static final String API_VERSION = "/api/v1";

    public static final String API_SUBMIT = API_VERSION + "/submit";

    public static final String API_FORCE_OK = API_VERSION + "/force-ok";
    public static final String API_FORCE_NOK = API_VERSION + "/force-nok";

    public static final String API_KILL = API_VERSION + "/kill";

    public static final String API_HEALTH = API_VERSION + "/health";

    public static final String API_PING = API_VERSION + "/ping";

    //Control the max query result
    public static final int MAX_QUERY_SIZE = 5000;

}
