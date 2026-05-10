package top.ilovemyhome.dagtask.admin.server;

import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.admin.server.application.AppContext;
import top.ilovemyhome.dagtask.admin.server.application.WebServerBootstrap;
import top.ilovemyhome.zora.config.ConfigLoader;

public class DagTaskAdminServer {

    public static void main(String[] args) {
        LOGGER.info("Starting dag-admin application.");
        String env = System.getProperty("env");
        if (StringUtils.isBlank(env)){
            throw new IllegalStateException("Cannot find env property.");
        }
        DagTaskAdminServer app = new DagTaskAdminServer();
        app.initAppContext(env);
        app.initWebServer(app.getAppContext());
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public static DagTaskAdminServer getInstance() {
        return APP;
    }

    private DagTaskAdminServer() {
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
    private static DagTaskAdminServer APP;
    private static final Logger LOGGER = LoggerFactory.getLogger(DagTaskAdminServer.class);

}
