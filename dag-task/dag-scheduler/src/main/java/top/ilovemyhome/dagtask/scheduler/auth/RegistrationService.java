package top.ilovemyhome.dagtask.scheduler.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationRequest;
import top.ilovemyhome.dagtask.si.auth.AgentRegistrationResponse;
import top.ilovemyhome.dagtask.si.auth.TokenPushRequest;
import top.ilovemyhome.dagtask.scheduler.config.AutoApproveConfig;
import top.ilovemyhome.dagtask.scheduler.token.AgentToken;
import top.ilovemyhome.dagtask.scheduler.token.AgentTokenDao;
import top.ilovemyhome.dagtask.scheduler.token.TokenService;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RegistrationService {

    private final AgentRegistrationDao registrationDao;
    private final TokenService tokenService;
    private final TokenPusher tokenPusher;
    private final AutoApproveConfig autoApproveConfig;
    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegistrationService(AgentRegistrationDao registrationDao,
                              TokenService tokenService,
                              TokenPusher tokenPusher,
                              AutoApproveConfig autoApproveConfig) {
        this.registrationDao = registrationDao;
        this.tokenService = tokenService;
        this.tokenPusher = tokenPusher;
        this.autoApproveConfig = autoApproveConfig;
    }

    private String generateId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public AgentRegistrationResponse createRegistration(AgentRegistrationRequest request, String clientAddress) {
        String registrationId = generateId();
        String nonce = generateId();

        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(24, ChronoUnit.HOURS);

        boolean autoApprove = autoApproveConfig.isMatch(request.name());
        AgentRegistration.Status status = autoApprove
            ? AgentRegistration.Status.APPROVED
            : AgentRegistration.Status.PENDING;

        String labelsJson = null;
        if (request.labels() != null && !request.labels().isEmpty()) {
            try {
                labelsJson = objectMapper.writeValueAsString(request.labels());
            } catch (Exception e) {
                labelsJson = null;
            }
        }

        AgentRegistration registration = new AgentRegistration(
            null,
            registrationId,
            request.name(),
            request.description(),
            labelsJson,
            request.callbackUrl(),
            nonce,
            clientAddress,
            status,
            null,
            null,
            null,
            createdAt,
            expiresAt
        );

        registrationDao.insert(registration);

        if (autoApprove) {
            approve(registrationId, "system", null);
            return new AgentRegistrationResponse(true, new AgentRegistrationResponse.Data(
                registrationId,
                "APPROVED",
                "Registration auto-approved via whitelist"
            ), null);
        }

        return new AgentRegistrationResponse(true, new AgentRegistrationResponse.Data(
            registrationId,
            "PENDING",
            "Registration submitted, waiting for admin approval"
        ), null);
    }

    public Optional<AgentRegistration> getRegistration(String registrationId) {
        return registrationDao.findByRegistrationId(registrationId);
    }

    public List<AgentRegistration> listByStatus(AgentRegistration.Status status, int limit) {
        return registrationDao.findByStatus(status, limit);
    }

    public void approve(String registrationId, String processedBy, String notes) {
        Optional<AgentRegistration> optReg = registrationDao.findByRegistrationId(registrationId);
        if (optReg.isEmpty()) {
            return;
        }
        AgentRegistration reg = optReg.get();
        if (reg.status() != AgentRegistration.Status.PENDING && reg.status() != AgentRegistration.Status.APPROVED) {
            return;
        }

        // Generate token
        var tokenResult = tokenService.generateToken(reg.agentName(), reg.description(), 365, processedBy);
        String jwt = tokenService.generateJwt(tokenResult);

        // Push token to callback
        TokenPushRequest pushRequest = new TokenPushRequest(
            registrationId,
            jwt,
            tokenResult.tokenId(),
            tokenResult.expiresAt(),
            reg.agentName()
        );

        boolean pushed = tokenPusher.pushToken(reg.callbackUrl(), reg.nonce(), pushRequest);

        if (pushed) {
            registrationDao.updateStatus(registrationId, AgentRegistration.Status.APPROVED, processedBy, notes);
        }
    }

    public void reject(String registrationId, String processedBy, String notes) {
        registrationDao.updateStatus(registrationId, AgentRegistration.Status.REJECTED, processedBy, notes);
    }

    public int cleanupExpired(Instant now) {
        return registrationDao.deleteExpiredPending(now);
    }
}
