package top.ilovemyhome.dagtask.si.agent;

/**
 * Record that encapsulates agent status information to be sent to the DAG scheduling server.
 * Contains current queue statistics and agent health information.
 *
 * @param agentId unique identifier of the agent
 * @param running whether the agent is currently running
 * @param pendingTasks number of tasks waiting in the pending queue
 * @param runningTasks number of tasks currently executing
 * @param finishedTasks number of completed tasks since agent started
 * @param maxConcurrentTasks maximum concurrent tasks configured
 * @param maxPendingTasks maximum pending tasks configured
 */
public record AgentStatusReport(
    String agentId,
    boolean running,
    int pendingTasks,
    int runningTasks,
    int finishedTasks,
    int maxConcurrentTasks,
    int maxPendingTasks
) {}
