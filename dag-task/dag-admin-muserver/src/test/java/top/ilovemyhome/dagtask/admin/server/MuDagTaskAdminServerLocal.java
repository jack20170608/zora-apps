package top.ilovemyhome.dagtask.admin.server;

public class MuDagTaskAdminServerLocal {

    static void main(String [] args) {
        System.setProperty("env", "local");
        MuDagTaskAdminServer.main(args);
    }
}
