package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterResponse;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;

/**
 * Server-side agent lifecycle: registration and de-registration.
 * <p>
 * Wraps the registration-related methods of the legacy
 * {@code AgentRegistryService.registerAgent} (both overloads) and
 * {@code AgentRegistryService.unregisterAgent}.
 * </p>
 */
public interface RegisterAgentUseCase {

    /**
     * Register a new agent instance with the scheduling center.
     *
     * @param registration the agent registration information
     * @return registration response with success status and optional token info
     */
    default AgentRegisterResponse registerAgent(AgentRegisterRequest registration) {
        return registerAgent(registration, null);
    }

    /**
     * Register a new agent instance with the scheduling center.
     *
     * @param registration the agent registration information
     * @param clientIp the client IP address for whitelist validation, may be {@code null}
     * @return registration response with success status, whitelist check result, and optional token info
     */
    AgentRegisterResponse registerAgent(AgentRegisterRequest registration, String clientIp);

    /**
     * Unregister an existing agent instance from the scheduling center.
     *
     * @param unregistration the unregistration information containing the agent ID
     * @return {@code true} if unregistration was successful, {@code false} if agent was not found
     */
    boolean unregisterAgent(AgentUnregistration unregistration);
}
