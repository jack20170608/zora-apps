package top.ilovemyhome.dagtask.si.agent;

import jakarta.ws.rs.core.Response;

/**
 * Client interface for agents to communicate with the DAG scheduling server.
 * Defines the contract for agent registration, unregistration, result reporting and status reporting.
 *
 * Implementations handle the actual HTTP communication with the scheduling server.
 */
public interface AgentSchedulerClient {

    /**
     * Register this agent with the DAG scheduling server.
     *
     * @param registration the agent registration information
     * @return the HTTP response from the server
     */
    Response register(AgentRegistration registration);

    /**
     * Unregister this agent from the DAG scheduling server.
     *
     * @param unregistration the agent unregistration information
     * @return the HTTP response from the server
     */
    Response unregister(AgentUnregistration unregistration);

    /**
     * Report the result of a task execution back to the scheduling server.
     *
     * @param taskResultReport the task execution result report
     * @return the HTTP response from the server
     */
    Response reportTaskResult(TaskResultReport taskResultReport);

    /**
     * Report current agent status and queue statistics back to the scheduling server.
     * This is used for health monitoring and load balancing.
     *
     * @param agentStatusReport the agent status report containing queue statistics
     * @return the HTTP response from the server
     */
    Response reportStatus(AgentStatusReport agentStatusReport);
}
