package top.ilovemyhome.dagtask.scheduler.domain.agent;

import top.ilovemyhome.dagtask.si.agent.AgentWhitelist;
import top.ilovemyhome.dagtask.scheduler.domain.util.IpAddressMatcher;

import java.util.List;

/**
 * Pure-domain helper: checks whether a given client IP is allowed by any of
 * the agent's whitelist rules. Used by {@code RegisterAgentService} to enforce
 * IP-based access control without depending on outbound ports.
 */
public final class WhitelistMatcher {

    private WhitelistMatcher() {
        // utility class
    }

    /**
     * Check if the client IP matches any IP segment defined in the agent's whitelist.
     *
     * @param clientIp   the client's IP address, may be null
     * @param agentId    the agent identifier, may be null
     * @param ipSegments the IP segments (CIDR or exact) associated with this agent
     * @return true if any segment matches, false otherwise
     */
    public static boolean isAllowed(String clientIp, String agentId, List<String> ipSegments) {
        if (clientIp == null || clientIp.isBlank() || agentId == null || agentId.isBlank()) {
            return false;
        }
        if (ipSegments == null || ipSegments.isEmpty()) {
            return false;
        }
        for (String segment : ipSegments) {
            if (IpAddressMatcher.matches(segment, clientIp)) {
                return true;
            }
        }
        return false;
    }
}
