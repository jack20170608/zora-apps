package top.ilovemyhome.hosthelper.muserver.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.ilovemyhome.commons.common.system.OSUtil;
import top.ilovemyhome.commons.common.system.SystemCommandChecker;
import top.ilovemyhome.tooling.hosthelper.domain.FileSearchCriteria;
import top.ilovemyhome.tooling.hosthelper.domain.FileType;
import top.ilovemyhome.tooling.hosthelper.util.os.LinuxOsUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public class LinuxCommandBuilderTest {

    @BeforeAll
    public static void setUp() {
    }

    LinuxCommandBuilder builder = new LinuxCommandBuilder();

    @Test
    void testBuildWithValidDirectorySearch() {
        String path = "/test/path/";
        String searchPath = "/test/path";
        FileSearchCriteria input = new FileSearchCriteria("local1"
            , path, null, FileType.DIRECTORY, 0L, 0L, null
            , null, null, null);

        try (MockedStatic<SystemCommandChecker> commandCheckerMockedStatic = mockStatic(SystemCommandChecker.class);
             MockedStatic<OSUtil> mockedOsUtil = mockStatic(OSUtil.class)) {
            mockedOsUtil.when(OSUtil::isLinux).thenReturn(true);
            commandCheckerMockedStatic.when(() -> SystemCommandChecker.isCommandAvailable("find"))
                .thenReturn(true);
            commandCheckerMockedStatic.when(() -> SystemCommandChecker.isCommandAvailable("grep"))
                .thenReturn(true);

            Path mockPath = mock(Path.class);
            when(mockPath.toAbsolutePath()).thenReturn(mockPath);
            when(mockPath.isAbsolute()).thenReturn(true);

            try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
                filesMockedStatic.when(() -> Files.exists(mockPath)).thenReturn(true);
                filesMockedStatic.when(() -> Files.isDirectory(mockPath)).thenReturn(true);

                try (MockedStatic<Path> pathMockedStatic = mockStatic(Path.class)) {
                    pathMockedStatic.when(() -> Path.of(searchPath)).thenReturn(mockPath);
                    String result = builder.build(input);
                    assertThat(result).isEqualTo("find /test/path -maxdepth 1 -type d -name '*' -printf '%y %f %p %s\\n'");
                }
            }
        }
    }

    @Test
    void testBuildWithValidFileSearch() {
        String path = "/test/path/foo*";
        FileSearchCriteria input = new FileSearchCriteria("local1"
            , path, null, FileType.FILE, 0L, 0L, null
            , null, null, null);

        try (MockedStatic<LinuxOsUtils> linuxOsUtilsMockedStatic = mockStatic(LinuxOsUtils.class);
             MockedStatic<SystemCommandChecker> commandCheckerMockedStatic = mockStatic(SystemCommandChecker.class);
             MockedStatic<OSUtil> mockedOsUtil = mockStatic(OSUtil.class)) {

            mockedOsUtil.when(OSUtil::isLinux).thenReturn(true);
            linuxOsUtilsMockedStatic.when(() -> LinuxOsUtils.parsePath(path))
                .thenReturn(List.of(path, "foo*"));
            commandCheckerMockedStatic.when(() -> SystemCommandChecker.isCommandAvailable("find"))
                .thenReturn(true);
            commandCheckerMockedStatic.when(() -> SystemCommandChecker.isCommandAvailable("grep"))
                .thenReturn(true);

            Path mockPath = mock(Path.class);
            when(mockPath.toAbsolutePath()).thenReturn(mockPath);
            when(mockPath.isAbsolute()).thenReturn(true);

            try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
                filesMockedStatic.when(() -> Files.exists(mockPath)).thenReturn(true);
                filesMockedStatic.when(() -> Files.isDirectory(mockPath)).thenReturn(true);

                try (MockedStatic<Path> pathMockedStatic = mockStatic(Path.class)) {
                    pathMockedStatic.when(() -> Path.of(path)).thenReturn(mockPath);
                    String result = builder.build(input);
                    assertThat(result).isEqualTo("find /test/path/foo* -maxdepth 1 -type f -name 'foo*' -printf '%y %f %p %s\\n'");
                }
            }
        }
    }

    @Test
    void testBuildWithAllSearch() {
        String pathPattern = "/test/path/foo*";
        String searchPath = "/test/path";
        FileSearchCriteria input = new FileSearchCriteria("local1"
            , pathPattern, null, null, 0L, 0L, null
            , null, null, null);

        try (MockedStatic<SystemCommandChecker> commandCheckerMockedStatic = mockStatic(SystemCommandChecker.class);
             MockedStatic<OSUtil> mockedOsUtil = mockStatic(OSUtil.class)) {
            mockedOsUtil.when(OSUtil::isLinux).thenReturn(true);
            commandCheckerMockedStatic.when(() -> SystemCommandChecker.isCommandAvailable("find"))
                .thenReturn(true);
            commandCheckerMockedStatic.when(() -> SystemCommandChecker.isCommandAvailable("grep"))
                .thenReturn(true);

            Path mockPath = mock(Path.class);
            when(mockPath.toAbsolutePath()).thenReturn(mockPath);
            when(mockPath.isAbsolute()).thenReturn(true);

            try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
                filesMockedStatic.when(() -> Files.exists(mockPath)).thenReturn(true);
                filesMockedStatic.when(() -> Files.isDirectory(mockPath)).thenReturn(true);

                try (MockedStatic<Path> pathMockedStatic = mockStatic(Path.class)) {
                    pathMockedStatic.when(() -> Path.of(searchPath)).thenReturn(mockPath);
                    String result = builder.build(input);
                    assertThat(result).isEqualTo("find /test/path -maxdepth 1 '(' -type f -o -type d ')' -name 'foo*' -printf '%y %f %p %s\\n'");
                }
            }
        }
    }

    @Test
    void testBuildWithAllSearchWithKeyWord() {
        String pathPattern = "/test/path/foo*";
        String searchPath = "/test/path";
        FileSearchCriteria input = new FileSearchCriteria("local1"
            , pathPattern, "foo", null, 0L, 0L, null
            , null, null, null);

        try (MockedStatic<SystemCommandChecker> commandCheckerMockedStatic = mockStatic(SystemCommandChecker.class);
             MockedStatic<OSUtil> mockedOsUtil = mockStatic(OSUtil.class)) {
            mockedOsUtil.when(OSUtil::isLinux).thenReturn(true);
            commandCheckerMockedStatic.when(() -> SystemCommandChecker.isCommandAvailable("find"))
                .thenReturn(true);
            commandCheckerMockedStatic.when(() -> SystemCommandChecker.isCommandAvailable("grep"))
                .thenReturn(true);

            Path mockPath = mock(Path.class);
            when(mockPath.toAbsolutePath()).thenReturn(mockPath);
            when(mockPath.isAbsolute()).thenReturn(true);

            try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
                filesMockedStatic.when(() -> Files.exists(mockPath)).thenReturn(true);
                filesMockedStatic.when(() -> Files.isDirectory(mockPath)).thenReturn(true);

                try (MockedStatic<Path> pathMockedStatic = mockStatic(Path.class)) {
                    pathMockedStatic.when(() -> Path.of(searchPath)).thenReturn(mockPath);
                    String result = builder.build(input);
                    assertThat(result).isEqualTo("find /test/path -maxdepth 1 -type f -name 'foo*' -exec grep -HIl 'foo' {} + 2>/dev/null | xargs ls -l");
                }
            }
        }
    }
}
