package top.ilovemyhome.dagtask.si.agent;

import java.util.List;

/**
 * Record that encapsulates agent registration information to be sent to the DAG scheduling server.
 * Contains all information the server needs to know about an agent when registering.
 *
 * @param agentId unique identifier for this agent instance
 * @param agentUrl the full URL where this agent is accessible from the server
 * @param maxConcurrentTasks maximum number of concurrent tasks this agent can execute
 * @param maxPendingTasks maximum number of pending tasks waiting in the queue
 * @param supportedExecutionKeys list of execution keys (task types) that this agent supports
 */
public record AgentRegistration(
    String agentId,
    String agentUrl,
    int maxConcurrentTasks,
    int maxPendingTasks,
    List<String> supportedExecutionKeys
) {}
