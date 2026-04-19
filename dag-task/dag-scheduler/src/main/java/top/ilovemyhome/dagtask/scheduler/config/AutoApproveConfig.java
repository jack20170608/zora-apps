package top.ilovemyhome.dagtask.scheduler.config;

import java.util.List;

public record AutoApproveConfig(
    boolean enabled,
    List<String> patterns
) {
    public boolean isMatch(String agentName) {
        if (!enabled || patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (matches(agentName, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String input, String pattern) {
        if (pattern.equals(input)) {
            return true;
        }
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        return input.matches(regex);
    }
}
