package top.ilovemyhome.dagtask.si.agent;

import java.time.Instant;
import java.util.List;

public record AgentRegisterRequest(
    String agentId,
    String name,
    String agentUrl,
    int maxConcurrentTasks,
    int maxPendingTasks,
    List<String> supportedExecutionKeys
) {
    public static Agent toAgent(AgentRegisterRequest request){
        return Agent.builder()
            .withAgentId(request.agentId())
            .withName(request.name())
            .withDescription("Registered agent with URL: " + request.agentUrl())
            .withLabelsJson("{}") // Placeholder for labels, can be extended to include actual labels
            .withStatus(Agent.Status.PENDING)
            .withRegisteredAt(Instant.now())
            .withLastHeartbeatAt(Instant.now())
            .build();
    }

    public static AgentStatus toAgentStatus(AgentRegisterRequest request){
        return AgentStatus.builder()
            .withAgentId(request.agentId())
            .withAgentUrl(request.agentUrl())
            .withMaxConcurrentTasks(request.maxConcurrentTasks())
            .withMaxPendingTasks(request.maxPendingTasks())
            .withSupportedExecutionKeys(String.join(",", request.supportedExecutionKeys()))
            .withRunning(false)
            .withPendingTasks(0)
            .withRunningTasks(0)
            .withLastHeartbeatAt(Instant.now())
            .build();
    }
}
