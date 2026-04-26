package top.ilovemyhome.dagtask.agent.muserver;

import top.ilovemyhome.dagtask.agent.muserver.starter.AppMain;

public class AgentMainLocal {

    static void main() {
        System.setProperty("env", "local");
        AppMain.main(null);
    }
}
