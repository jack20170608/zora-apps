package top.ilovemyhome.dagtask.scheduler.auth;

import java.util.List;
import java.util.Optional;

public interface AgentRegistrationDao {

    AgentRegistration insert(AgentRegistration registration);

    Optional<AgentRegistration> findByRegistrationId(String registrationId);

    List<AgentRegistration> findByStatus(AgentRegistration.Status status, int limit);

    List<AgentRegistration> findExpiredPending(java.time.Instant now, int limit);

    int deleteExpiredPending(java.time.Instant now);

    void updateStatus(String registrationId, AgentRegistration.Status status, String processedBy, String notes);
}
