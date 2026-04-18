package top.ilovemyhome.dagtask.si;

public class Constants {

    private Constants() {
    }

    public static final String API_VERSION = "/api/v1";

    public static final String API_SCHEDULER = API_VERSION + "/scheduler";
    public static final String API_REGISTER = "/register";
    public static final String API_UNREGISTER =  "/unregister";
    public static final String API_REPORT_RESULT= "/report_result";
    public static final String API_REPORT_STATUS= "/report_status";

    public static final String API_AGENT = API_VERSION + "/agent";
    public static final String API_SUBMIT = "/submit";
    public static final String API_FORCE_OK = "/force-ok";
    public static final String API_FORCE_NOK = "/force-nok";
    public static final String API_HOLD = "/hold";
    public static final String API_FREE = "/free";
    public static final String API_KILL = "/kill";
    public static final String API_HEALTH =  "/health";
    public static final String API_PING = "/ping";

    //Control the max query result
    public static final int MAX_QUERY_SIZE = 5000;

}
