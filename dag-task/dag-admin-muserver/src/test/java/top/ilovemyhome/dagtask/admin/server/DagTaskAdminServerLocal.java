package top.ilovemyhome.dagtask.admin.server;

public class DagTaskAdminServerLocal {

    static void main(String [] args) {
        System.setProperty("env", "local");
        DagTaskAdminServer.main(args);
    }
}
