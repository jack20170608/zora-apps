package top.ilovemyhome.dagtask.agent.utils;

import java.util.Locale;

/**
 * Utility class for detecting the operating system and providing appropriate shell commands.
 *
 * <p>This class handles cross-platform shell execution by automatically detecting the OS
 * and providing the most suitable shell and command format for each platform.
 */
public final class ShellDetector {

    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_MAC = OS_NAME.contains("mac");
    private static final boolean IS_LINUX = OS_NAME.contains("linux");

    private ShellDetector() {
        // Utility class
    }

    /**
     * Get the default shell for the current operating system.
     *
     * @return shell command (e.g., "bash", "cmd.exe", "powershell.exe")
     */
    public static String getDefaultShell() {
        if (IS_WINDOWS) {
            return "cmd.exe";
        } else if (IS_MAC || IS_LINUX) {
            return "bash";
        }
        return "bash";  // Default fallback
    }

    /**
     * Get alternative shells available on the current platform.
     *
     * @return array of alternative shell names
     */
    public static String[] getAlternativeShells() {
        if (IS_WINDOWS) {
            return new String[]{"powershell.exe", "pwsh.exe"};
        }
        return new String[]{"sh", "zsh"};
    }

    /**
     * Build command array for the specified shell and command.
     *
     * @param shell the shell executable (e.g., "bash", "cmd.exe")
     * @param script the command/command to execute
     * @return command array suitable for ProcessBuilder
     */
    public static String[] buildCommandArray(String shell, String script) {
        if (shell == null || shell.isBlank()) {
            shell = getDefaultShell();
        }

        if (isWindowsCmd(shell)) {
            // For cmd.exe, use /c flag
            return new String[]{shell, "/c", script};
        } else if (isPowerShell(shell)) {
            // For PowerShell, use -Command flag
            return new String[]{shell, "-Command", script};
        } else {
            // For bash, sh, zsh, etc., use -c flag
            return new String[]{shell, "-c", script};
        }
    }

    /**
     * Check if the given shell is Windows cmd.exe.
     *
     * @param shell the shell name to check
     * @return true if it's Windows cmd.exe
     */
    public static boolean isWindowsCmd(String shell) {
        return shell != null && (shell.equalsIgnoreCase("cmd.exe") || shell.equalsIgnoreCase("cmd"));
    }

    /**
     * Check if the given shell is PowerShell.
     *
     * @param shell the shell name to check
     * @return true if it's PowerShell
     */
    public static boolean isPowerShell(String shell) {
        if (shell == null) {
            return false;
        }
        String lower = shell.toLowerCase(Locale.ENGLISH);
        return lower.contains("powershell") || lower.equals("pwsh") || lower.equals("pwsh.exe");
    }

    /**
     * Convert a Unix-style command to Windows-style if needed.
     *
     * @param script the original command
     * @param targetShell the target shell
     * @return converted command or original if no conversion needed
     */
    public static String adaptScriptForShell(String script, String targetShell) {
        if (script == null || script.isBlank()) {
            return script;
        }

        if (isWindowsCmd(targetShell)) {
            // Simple conversion: echo -> echo, ls -> dir, etc.
            String converted = script;
            if (script.trim().startsWith("ls ") || script.trim().equals("ls")) {
                converted = script.replaceFirst("^\\s*ls\\s*", "dir");
            }
            if (script.trim().startsWith("cat ")) {
                converted = script.replaceFirst("^\\s*cat\\s+", "type ");
            }
            return converted;
        }

        return script;
    }

    /**
     * Check if the current OS is Windows.
     *
     * @return true if running on Windows
     */
    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    /**
     * Check if the current OS is Linux.
     *
     * @return true if running on Linux
     */
    public static boolean isLinux() {
        return IS_LINUX;
    }

    /**
     * Check if the current OS is macOS.
     *
     * @return true if running on macOS
     */
    public static boolean isMac() {
        return IS_MAC;
    }

    /**
     * Get the OS identifier.
     *
     * @return human-readable OS name
     */
    public static String getOsName() {
        if (IS_WINDOWS) {
            return "Windows";
        } else if (IS_MAC) {
            return "macOS";
        } else if (IS_LINUX) {
            return "Linux";
        }
        return OS_NAME;
    }

    /**
     * Validate that the shell is available on the system.
     * Note: This is a best-effort check and may not be 100% reliable.
     *
     * @param shell the shell to validate
     * @return validation result
     */
    public static ShellValidationResult validateShell(String shell) {
        if (shell == null || shell.isBlank()) {
            return ShellValidationResult.valid("Using default shell: " + getDefaultShell());
        }

        String normalizedShell = shell.toLowerCase(Locale.ENGLISH);

        // Check for common shells
        if (normalizedShell.equals("bash") || normalizedShell.equals("sh") ||
            normalizedShell.equals("zsh") || normalizedShell.equals("cmd.exe") ||
            normalizedShell.equals("cmd") || normalizedShell.contains("powershell")) {
            return ShellValidationResult.valid("Shell is recognized: " + shell);
        }

        return ShellValidationResult.warning("Shell may not be available: " + shell);
    }

    /**
     * Result of shell validation.
     */
    public static class ShellValidationResult {
        private final boolean valid;
        private final String message;

        private ShellValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ShellValidationResult valid(String message) {
            return new ShellValidationResult(true, message);
        }

        public static ShellValidationResult warning(String message) {
            return new ShellValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return message;
        }
    }
}

