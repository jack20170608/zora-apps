package top.ilovemyhome.dagtask.si.enums;

import java.util.Locale;

/**
 * Enumerates supported shell types with their platform-specific execution parameters.
 *
 * <p>Each shell type defines its executable name, command flag, and target platform.
 * This enum centralizes shell metadata to eliminate hard-coded string literals across
 * the task execution framework.
 */
public enum ShellType {

    BASH("bash", "-c", Platform.UNIX),
    SH("sh", "-c", Platform.UNIX),
    ZSH("zsh", "-c", Platform.UNIX),
    CMD("cmd.exe", "/c", Platform.WINDOWS),
    POWERSHELL("powershell.exe", "-Command", Platform.WINDOWS),
    PWSH("pwsh.exe", "-Command", Platform.WINDOWS);

    private final String executable;
    private final String flag;
    private final Platform platform;

    ShellType(String executable, String flag, Platform platform) {
        this.executable = executable;
        this.flag = flag;
        this.platform = platform;
    }

    /**
     * Returns the shell executable name (e.g., "bash", "cmd.exe").
     */
    public String getExecutable() {
        return executable;
    }

    /**
     * Returns the flag used to pass a command string to the shell (e.g., "-c", "/c").
     */
    public String getFlag() {
        return flag;
    }

    /**
     * Returns the target platform for this shell.
     */
    public Platform getPlatform() {
        return platform;
    }

    /**
     * Checks whether this shell is a Windows shell.
     *
     * @return true if the shell targets Windows
     */
    public boolean isWindowsShell() {
        return platform == Platform.WINDOWS;
    }

    /**
     * Builds a command array suitable for {@link java.lang.ProcessBuilder}.
     *
     * @param script the command string to execute
     * @return array in the form {@code [executable, flag, script]}
     */
    public String[] buildCommandArray(String script) {
        return new String[]{executable, flag, wrapCommandForEncoding(script)};
    }

    /**
     * Wraps the user script with encoding directives to ensure UTF-8 output.
     *
     * <ul>
     *   <li>Windows {@code cmd.exe}: switches code page to 65001 (UTF-8)</li>
     *   <li>PowerShell / pwsh: sets {@code [Console]::OutputEncoding} to UTF-8</li>
     *   <li>Unix shells ({@code bash}, {@code sh}, {@code zsh}): rely on {@code LC_ALL} / {@code LANG}
     *       environment variables injected by the caller</li>
     * </ul>
     *
     * @param script the raw user command
     * @return script prefixed with shell-specific encoding setup if needed
     */
    public String wrapCommandForEncoding(String script) {
        return switch (this) {
            case CMD -> "chcp 65001 >nul && " + script;
            case POWERSHELL, PWSH -> "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; " + script;
            default -> script;
        };
    }

    /**
     * Resolves a shell type from a string value.
     *
     * <p>Matches are case-insensitive and tolerate abbreviations such as
     * "cmd" for {@link #CMD} or "pwsh" for {@link #PWSH}.
     *
     * @param value the raw shell identifier; may be null or blank
     * @return the matched {@code ShellType}, or null if no match
     */
    public static ShellType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ENGLISH);
        for (ShellType type : values()) {
            if (type.executable.equalsIgnoreCase(value)
                || type.name().toLowerCase(Locale.ENGLISH).equals(normalized)) {
                return type;
            }
        }
        // Tolerate common abbreviations
        return switch (normalized) {
            case "cmd" -> CMD;
            case "powershell" -> POWERSHELL;
            default -> null;
        };
    }

    /**
     * Returns the default shell for the current operating system.
     *
     * @return {@link #CMD} on Windows, {@link #BASH} otherwise
     */
    public static ShellType getDefaultForCurrentOs() {
        if (Platform.isCurrentWindows()) {
            return CMD;
        }
        return BASH;
    }

    /**
     * Supported operating system platforms.
     */
    public enum Platform {
        UNIX,
        WINDOWS;

        private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);

        /**
         * Checks whether the current JVM is running on Windows.
         */
        public static boolean isCurrentWindows() {
            return OS_NAME.contains("win");
        }
    }
}
