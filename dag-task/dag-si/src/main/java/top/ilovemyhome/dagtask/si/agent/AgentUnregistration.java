package top.ilovemyhome.dagtask.si.agent;

/**
 * Record that encapsulates agent unregistration information to be sent to the DAG scheduling server.
 *
 * @param agentId unique identifier of the agent to unregister
 */
public record AgentUnregistration(
    String agentId
) {}
