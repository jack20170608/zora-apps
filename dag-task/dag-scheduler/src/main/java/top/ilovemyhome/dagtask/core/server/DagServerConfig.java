package top.ilovemyhome.dagtask.core.server;

/**
 * Configuration settings for the DAG scheduling server (scheduler center).
 * <p>
 * This class holds all configuration parameters that control the behavior
 * of the scheduling server including scheduling intervals, timeouts,
 * health check parameters, and resource limits.
 * </p>
 */
public record DagServerConfig(int scanIntervalSeconds
    , int maxSystemConcurrentTasks
    , String databaseType
    , int heartbeatTimeoutSeconds
    , int heartbeatIntervalSeconds
    , int maxHeartbeatFailedTimes
) {
}
