package top.ilovemyhome.dagtask.si.agent;

import top.ilovemyhome.dagtask.si.auth.TokenInfo;

/**
 * Response DTO for agent registration requests.
 * Contains the registration result, whitelist check status,
 * and optional token information when token generation is requested.
 */
public record AgentRegisterResponse(
    boolean success,
    boolean whitelisted,
    String message,
    TokenInfo tokenInfo
) {

    /**
     * Creates a successful registration response without token.
     *
     * @return a success response with whitelisted=true
     */
    public static AgentRegisterResponse ok() {
        return new AgentRegisterResponse(true, true, "Agent registered successfully", null);
    }

    /**
     * Creates a successful registration response with the given message.
     *
     * @param message the success message
     * @return a success response with whitelisted=true
     */
    public static AgentRegisterResponse ok(String message) {
        return new AgentRegisterResponse(true, true, message, null);
    }

    /**
     * Creates a response indicating the agent was denied by whitelist.
     *
     * @return a denied response with whitelisted=false
     */
    public static AgentRegisterResponse denied() {
        return new AgentRegisterResponse(false, false, "Agent registration denied: not in whitelist", null);
    }

    /**
     * Creates a failed registration response with the given message.
     * Assumes whitelist was passed but another error occurred.
     *
     * @param message the failure message
     * @return a failure response with whitelisted=true
     */
    public static AgentRegisterResponse failure(String message) {
        return new AgentRegisterResponse(false, true, message, null);
    }

    /**
     * Returns a new response with the token info set.
     *
     * @param tokenInfo the token information to include
     * @return a new response instance with token info
     */
    public AgentRegisterResponse withTokenInfo(TokenInfo tokenInfo) {
        return new AgentRegisterResponse(this.success, this.whitelisted, this.message, tokenInfo);
    }
}
