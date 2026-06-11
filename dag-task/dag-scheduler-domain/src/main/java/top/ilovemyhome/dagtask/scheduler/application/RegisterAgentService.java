package top.ilovemyhome.dagtask.scheduler.application;

import top.ilovemyhome.dagtask.scheduler.domain.agent.WhitelistMatcher;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentStatusRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentWhitelistRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.TokenIssuer;
import top.ilovemyhome.dagtask.scheduler.port.out.UnitOfWork;
import top.ilovemyhome.dagtask.si.agent.Agent;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterResponse;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;

import java.util.List;
import java.util.Objects;

/**
 * Application service for agent registration and de-registration.
 * <p>
 * Replaces the registration/unregistration methods of the legacy
 * {@code DefaultAgentRegistryService}. The in-memory cache from the legacy
 * implementation is intentionally omitted (TD-3); this service always reads
 * from the repository.
 * </p>
 */
public class RegisterAgentService implements top.ilovemyhome.dagtask.scheduler.port.in.RegisterAgentUseCase {

    private final AgentRepository agentRepository;
    private final AgentStatusRepository agentStatusRepository;
    private final AgentWhitelistRepository agentWhitelistRepository;
    private final TokenIssuer tokenIssuer;
    private final UnitOfWork unitOfWork;

    public RegisterAgentService(AgentRepository agentRepository,
                                AgentStatusRepository agentStatusRepository,
                                AgentWhitelistRepository agentWhitelistRepository,
                                TokenIssuer tokenIssuer,
                                UnitOfWork unitOfWork) {
        this.agentRepository = Objects.requireNonNull(agentRepository, "agentRepository must not be null");
        this.agentStatusRepository = Objects.requireNonNull(agentStatusRepository, "agentStatusRepository must not be null");
        this.agentWhitelistRepository = Objects.requireNonNull(agentWhitelistRepository, "agentWhitelistRepository must not be null");
        this.tokenIssuer = Objects.requireNonNull(tokenIssuer, "tokenIssuer must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
    }

    @Override
    public AgentRegisterResponse registerAgent(AgentRegisterRequest registration, String clientIp) {
        if (registration == null || registration.agentId() == null || registration.agentId().isBlank()) {
            return AgentRegisterResponse.failure("Invalid registration: agentId is blank");
        }

        // Validate against whitelist: deny by default if no matching rule found
        List<String> ipSegments = agentWhitelistRepository.findIpSegmentsByAgentId(registration.agentId());
        if (!WhitelistMatcher.isAllowed(clientIp, registration.agentId(), ipSegments)) {
            return AgentRegisterResponse.denied();
        }

        Agent agent = AgentRegisterRequest.toAgent(registration);
        agent.setStatus(Agent.Status.ACTIVE);
        AgentStatus status = AgentRegisterRequest.toAgentStatus(registration);

        unitOfWork.execute(() -> {
            if (agentRepository.exists(registration.agentId())) {
                // Reactivate existing agent
                agentRepository.updateStatus(registration.agentId(), Agent.Status.ACTIVE);
                if (agentStatusRepository.exists(registration.agentId())) {
                    agentStatusRepository.updateStatus(registration.agentId(), true, 0, 0, 0);
                } else {
                    agentStatusRepository.create(status);
                }
            } else {
                // Insert new agent and status
                agentRepository.create(agent);
                agentStatusRepository.create(status);
            }
        });

        AgentRegisterResponse response = AgentRegisterResponse.ok();

        // Generate token if requested
        if (registration.generateToken()) {
            TokenInfo tokenInfo = tokenIssuer.issueAgentToken(
                registration.agentId(),
                registration.name(),
                "Auto-generated token for agent: " + registration.agentId(),
                30,
                "system"
            );
            response = response.withTokenInfo(tokenInfo);
        }

        return response;
    }

    @Override
    public boolean unregisterAgent(AgentUnregistration unregistration) {
        if (unregistration == null || unregistration.agentId() == null || unregistration.agentId().isBlank()) {
            return false;
        }

        if (!agentRepository.exists(unregistration.agentId())) {
            return false;
        }

        agentRepository.updateStatus(unregistration.agentId(), Agent.Status.INACTIVE);
        agentStatusRepository.markUnregistered(unregistration.agentId());
        return true;
    }
}
