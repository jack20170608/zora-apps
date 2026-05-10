package top.ilovemyhome.dagtask.scheduler.muserver;

import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.scheduler.muserver.application.AppContext;
import top.ilovemyhome.dagtask.scheduler.muserver.application.WebServerBootstrap;
import top.ilovemyhome.zora.config.ConfigLoader;

public class MuSchedulerServer {

    public static void main(String[] args) {
        LOGGER.info("Starting application.");
        String env = System.getProperty("env");
        if (StringUtils.isBlank(env)){
            throw new IllegalStateException("Cannot find env property.");
        }
        MuSchedulerServer schedulerServer = new MuSchedulerServer();
        schedulerServer.initAppContext(env);
        schedulerServer.initWebServer(schedulerServer.getAppContext());
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public static MuSchedulerServer getInstance() {
        return SchedulerServer;
    }

    private MuSchedulerServer() {
    }

    private void initAppContext(String env){
        String rootConfig = "config/application.conf";
        String envConfig = "config/application-" + env + ".conf";
        Config config = ConfigLoader.loadConfig(rootConfig, envConfig);
        this.appContext = new AppContext(env, config);
    }

    private void initWebServer(AppContext appContext){
        WebServerBootstrap.start(appContext);
    }

    private AppContext appContext;
    private static MuSchedulerServer SchedulerServer;
    private static final Logger LOGGER = LoggerFactory.getLogger(MuSchedulerServer.class);

}
