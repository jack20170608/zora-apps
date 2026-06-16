package top.ilovemyhome.dagtask.agent.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ShellDetector utility class.
 */
class ShellDetectorTest {

    @Test
    void testGetDefaultShell() {
        String shell = ShellDetector.getDefaultShell();
        assertThat(shell).isNotEmpty();

        if (ShellDetector.isWindows()) {
            assertThat(shell).isEqualTo("cmd.exe");
        } else {
            assertThat(shell).isEqualTo("bash");
        }
    }

    @Test
    @EnabledOnOs({OS.WINDOWS})
    void testIsWindows() {
        boolean isWindows = ShellDetector.isWindows();
        assertThat(isWindows).isTrue();  // Always true or false, not null
    }

    @Test
    @EnabledOnOs({OS.LINUX})
    void testIsLinux() {
        boolean isLinux = ShellDetector.isLinux();
        assertThat(isLinux).isTrue();
    }

    @Test
    @EnabledOnOs({OS.MAC})
    void testIsMac() {
        boolean isMac = ShellDetector.isMac();
        assertThat(isMac).isTrue();
    }

    @Test
    void testGetOsName() {
        String osName = ShellDetector.getOsName();
        assertThat(osName)
            .isNotEmpty()
            .isIn("Windows", "Linux", "macOS");
    }

    @Test
    void testBuildCommandArrayBash() {
        String[] commands = ShellDetector.buildCommandArray("bash", "echo hello");
        assertThat(commands).hasSize(3);
        assertThat(commands[0]).isEqualTo("bash");
        assertThat(commands[1]).isEqualTo("-c");
        assertThat(commands[2]).isEqualTo("echo hello");
    }

    @Test
    void testBuildCommandArraySh() {
        String[] commands = ShellDetector.buildCommandArray("sh", "echo hello");
        assertThat(commands).hasSize(3);
        assertThat(commands[0]).isEqualTo("sh");
        assertThat(commands[1]).isEqualTo("-c");
        assertThat(commands[2]).isEqualTo("echo hello");
    }

    @Test
    void testBuildCommandArrayCmd() {
        String[] commands = ShellDetector.buildCommandArray("cmd.exe", "echo hello");
        assertThat(commands).hasSize(3);
        assertThat(commands[0]).isEqualTo("cmd.exe");
        assertThat(commands[1]).isEqualTo("/c");
        assertThat(commands[2]).isEqualTo("chcp 65001 >nul && echo hello");
    }

    @Test
    void testBuildCommandArrayPowerShell() {
        String[] commands = ShellDetector.buildCommandArray("powershell.exe", "Write-Host hello");
        assertThat(commands).hasSize(3);
        assertThat(commands[0]).isEqualTo("powershell.exe");
        assertThat(commands[1]).isEqualTo("-Command");
        assertThat(commands[2]).isEqualTo("[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Write-Host hello");
    }

    @Test
    void testBuildCommandArrayPwsh() {
        String[] commands = ShellDetector.buildCommandArray("pwsh", "Write-Host hello");
        assertThat(commands).hasSize(3);
        assertThat(commands[0]).isEqualTo("pwsh.exe");
        assertThat(commands[1]).isEqualTo("-Command");
        assertThat(commands[2]).isEqualTo("[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Write-Host hello");
    }

    @Test
    void testBuildCommandArrayWithNullShell() {
        String[] commands = ShellDetector.buildCommandArray(null, "echo hello");
        assertThat(commands).hasSize(3);
        // First element should be the default shell
        String shell = commands[0];
        assertThat(shell).isNotEmpty();
    }

    @Test
    void testBuildCommandArrayWithBlankShell() {
        String[] commands = ShellDetector.buildCommandArray("   ", "echo hello");
        assertThat(commands).hasSize(3);
        String shell = commands[0];
        assertThat(shell).isNotEmpty();
    }

    @Test
    void testIsWindowsCmd() {
        assertThat(ShellDetector.isWindowsCmd("cmd.exe")).isTrue();
        assertThat(ShellDetector.isWindowsCmd("cmd")).isTrue();
        assertThat(ShellDetector.isWindowsCmd("CMD.EXE")).isTrue();
        assertThat(ShellDetector.isWindowsCmd("bash")).isFalse();
        assertThat(ShellDetector.isWindowsCmd(null)).isFalse();
    }

    @Test
    void testIsPowerShell() {
        assertThat(ShellDetector.isPowerShell("powershell.exe")).isTrue();
        assertThat(ShellDetector.isPowerShell("POWERSHELL.EXE")).isTrue();
        assertThat(ShellDetector.isPowerShell("pwsh")).isTrue();
        assertThat(ShellDetector.isPowerShell("pwsh.exe")).isTrue();
        assertThat(ShellDetector.isPowerShell("bash")).isFalse();
        assertThat(ShellDetector.isPowerShell(null)).isFalse();
    }

    @Test
    void testValidateShellWithValidShell() {
        ShellDetector.ShellValidationResult result = ShellDetector.validateShell("bash");
        assertThat(result.isValid()).isTrue();
        assertThat(result.toString()).contains("bash");
    }

    @Test
    void testValidateShellWithNullShell() {
        ShellDetector.ShellValidationResult result = ShellDetector.validateShell(null);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidateShellWithUnknownShell() {
        ShellDetector.ShellValidationResult result = ShellDetector.validateShell("unknown_shell");
        assertThat(result.isValid()).isFalse();
        assertThat(result.toString()).contains("unknown_shell");
    }

    @Test
    void testGetAlternativeShells() {
        String[] alternatives = ShellDetector.getAlternativeShells();
        assertThat(alternatives).isNotEmpty();

        if (ShellDetector.isWindows()) {
            assertThat(alternatives).contains("powershell.exe");
        } else {
            assertThat(alternatives).contains("sh");
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testAdaptScriptForUnixShell() {
        String script = ShellDetector.adaptScriptForShell("echo hello", "bash");
        assertThat(script).isEqualTo("echo hello");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testAdaptScriptForWindowsCmd() {
        // Test ls -> dir conversion
        String script = ShellDetector.adaptScriptForShell("ls -la", "cmd.exe");
        assertThat(script).contains("dir");
    }
}

