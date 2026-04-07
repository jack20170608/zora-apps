package top.ilovemyhome.hosthelper.muserver.util.os;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class LinuxOsUtilsParsePathTest {

    @Test
    public void test_parsePath_withNullInput_shouldUseDefaultValues() {
        List<String> result = LinuxOsUtils.parsePath(null);
        assertParsePathResult(result, LinuxOsUtils.DEFAULT_FILE_PATH_PATTERN, LinuxOsUtils.DEFAULT_FILE_NAME_PATTERN);
    }

    @Test
    public void test_parsePath_withBlankInput_shouldUseDefaultValues() {
        List<String> result = LinuxOsUtils.parsePath("");
        assertParsePathResult(result, LinuxOsUtils.DEFAULT_FILE_PATH_PATTERN, LinuxOsUtils.DEFAULT_FILE_NAME_PATTERN);
    }

    @Test
    public void test_parsePath_withValidAbsolutePathWithSlash_shouldSplitCorrectly() {
        String inputPath = "/home/user/file.txt";
        String expectedDir = "/home/user";
        String expectedFile = "file.txt";
        List<String> result = LinuxOsUtils.parsePath(inputPath);
        assertParsePathResult(result, expectedDir, expectedFile);
    }

    @Test
    public void test_parsePath_withValidAbsolutePathWithoutSlash_shouldUseDefaultDirectory() {
        String inputPath = "file.txt";
        String expectedFile = "file.txt";
        List<String> result = LinuxOsUtils.parsePath(inputPath);
        assertParsePathResult(result, LinuxOsUtils.DEFAULT_FILE_PATH_PATTERN, expectedFile);
    }

    @Test
    public void test_parsePath_withInvalidPath_shouldThrowIllegalArgumentException() {
        String inputPath = "/invalid//path";
        assertParsePathThrowsException(inputPath, IllegalArgumentException.class, "filePathPattern is not a valid linux path!");
    }

    @Test
    public void test_parsePath_withRelativePath() {
        String inputPath = "relative/path/file.txt";
        String expectedDir = LinuxOsUtils.DEFAULT_FILE_PATH_PATTERN + "/relative/path";
        String expectedFile = "file.txt";
        List<String> result = LinuxOsUtils.parsePath(inputPath);
        assertParsePathResult(result, expectedDir, expectedFile);

    }

    @Test
    public void test_parsePath_withRelativePath_withoutFilename() {
        String inputPath = "relative/path/";
        String expectedDir = LinuxOsUtils.DEFAULT_FILE_PATH_PATTERN + "/relative/path";
        String expectedFile = LinuxOsUtils.DEFAULT_FILE_NAME_PATTERN;
        List<String> result = LinuxOsUtils.parsePath(inputPath);
        assertParsePathResult(result, expectedDir, expectedFile);

    }

    @Test
    public void test_parsePath_withValidPathButEmptyFileNamePattern_shouldUseDefaultFileName() {
        String inputPath = "/home/user/";
        String expectedDir = "/home/user";
        List<String> result = LinuxOsUtils.parsePath(inputPath);
        assertParsePathResult(result, expectedDir, LinuxOsUtils.DEFAULT_FILE_NAME_PATTERN);
    }

    // Extract common assertion logic into a helper method
    private void assertParsePathResult(List<String> result, String expectedDir, String expectedFile) {
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0)).isEqualTo(expectedDir);
        assertThat(result.get(1)).isEqualTo(expectedFile);
    }

    // Extract common exception assertion logic into a helper method
    private void assertParsePathThrowsException(String inputPath, Class<? extends Throwable> exceptionClass, String expectedMessage) {
        assertThatThrownBy(() -> LinuxOsUtils.parsePath(inputPath))
            .isInstanceOf(exceptionClass)
            .hasMessage(expectedMessage);
    }
}
