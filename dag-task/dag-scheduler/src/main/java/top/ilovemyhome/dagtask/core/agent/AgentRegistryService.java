package top.ilovemyhome.dagtask.core.agent;

import top.ilovemyhome.dagtask.si.agent.AgentRegistration;
import top.ilovemyhome.dagtask.si.agent.AgentStatusReport;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.agent.TaskResultReport;

/**
 * Service interface for the server-side handling of agent registration and status reporting.
 * This is the server counterpart to {@link top.ilovemyhome.dagtask.si.agent.AgentSchedulerClient}
 * that defines the contract for handling requests coming from agent instances.
 *
 * Implementations manage the agent registry, track agent status, and process task result reports.
 */
public interface AgentRegistryService {

    /**
     * Register a new agent instance with the scheduling center.
     *
     * @param registration the agent registration information containing agent capabilities and endpoint
     * @return true if registration was successful, false otherwise
     */
    boolean registerAgent(AgentRegistration registration);

    /**
     * Unregister an existing agent instance from the scheduling center.
     *
     * @param unregistration the unregistration information containing the agent ID
     * @return true if unregistration was successful, false if agent was not found
     */
    boolean unregisterAgent(AgentUnregistration unregistration);

    /**
     * Process a task execution result reported by an agent.
     *
     * @param taskResultReport the task result report containing execution outcome
     * @return true if the result was processed successfully, false otherwise
     */
    boolean reportTaskResult(TaskResultReport taskResultReport);

    /**
     * Update the status of an agent. This is called periodically by agents to report
     * current queue statistics and health status.
     *
     * @param statusReport the agent status report with current queue metrics
     * @return true if status was updated successfully, false if agent not found
     */
    boolean reportAgentStatus(AgentStatusReport statusReport);
}
