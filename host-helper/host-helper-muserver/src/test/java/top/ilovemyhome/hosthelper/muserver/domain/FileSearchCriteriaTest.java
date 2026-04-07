package top.ilovemyhome.hosthelper.muserver.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

public class FileSearchCriteriaTest {

    private static final String HOST_LABEL = "local1";
    private static final String FILE_PATH_PATTERN = "/test/path/";
    private static final String KEYWORDS = "test";
    private static final long MIN_FILE_SIZE = 100;
    private static final long MAX_FILE_SIZE = 1000;
    private static final LocalDateTime MIN_MODIFIED_TIME = LocalDateTime.of(2023, Month.JANUARY, 1, 0, 0);
    private static final LocalDateTime MAX_MODIFIED_TIME = LocalDateTime.of(2023, Month.DECEMBER, 31, 23, 59);
    private static final LocalDateTime MIN_CREATED_TIME = LocalDateTime.of(2022, Month.JANUARY, 1, 0, 0);
    private static final LocalDateTime MAX_CREATED_TIME = LocalDateTime.of(2022, Month.DECEMBER, 31, 23, 59);

    @Test
    void testCreationWithValidParameters() {
        // Arrange & Act
        FileSearchCriteria criteria = new FileSearchCriteria(
            HOST_LABEL,
            FILE_PATH_PATTERN,
            KEYWORDS,
            FileType.FILE,
            MIN_FILE_SIZE,
            MAX_FILE_SIZE,
            MIN_MODIFIED_TIME,
            MAX_MODIFIED_TIME,
            MIN_CREATED_TIME,
            MAX_CREATED_TIME
        );

        // Assert
        assertEquals(HOST_LABEL, criteria.hostLabel());
        assertEquals(FILE_PATH_PATTERN, criteria.filePathPattern());
        assertEquals(KEYWORDS, criteria.keywords());
        assertEquals(FileType.FILE, criteria.fileType());
        assertEquals(MIN_FILE_SIZE, criteria.minFileSize());
        assertEquals(MAX_FILE_SIZE, criteria.maxFileSize());
        assertEquals(MIN_MODIFIED_TIME, criteria.minModifiedTime());
        assertEquals(MAX_MODIFIED_TIME, criteria.maxModifiedTime());
        assertEquals(MIN_CREATED_TIME, criteria.minCreatedTime());
        assertEquals(MAX_CREATED_TIME, criteria.maxCreatedTime());
    }

    @Test
    void testCreationWithNullHostLabel() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> new FileSearchCriteria(
            null,
            FILE_PATH_PATTERN,
            KEYWORDS,
            FileType.FILE,
            MIN_FILE_SIZE,
            MAX_FILE_SIZE,
            MIN_MODIFIED_TIME,
            MAX_MODIFIED_TIME,
            MIN_CREATED_TIME,
            MAX_CREATED_TIME
        ));
    }

    @Test
    void testCreationWithNullFilePathPattern() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> new FileSearchCriteria(
            HOST_LABEL,
            null,
            KEYWORDS,
            FileType.FILE,
            MIN_FILE_SIZE,
            MAX_FILE_SIZE,
            MIN_MODIFIED_TIME,
            MAX_MODIFIED_TIME,
            MIN_CREATED_TIME,
            MAX_CREATED_TIME
        ));
    }

    @Test
    void testCreationWithParentPath() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> new FileSearchCriteria(
            HOST_LABEL,
            "../../invalid/./path",
            KEYWORDS,
            FileType.FILE,
            MIN_FILE_SIZE,
            MAX_FILE_SIZE,
            MIN_MODIFIED_TIME,
            MAX_MODIFIED_TIME,
            MIN_CREATED_TIME,
            MAX_CREATED_TIME
        ));
    }
    @Test
    void testCreationWithInvalidPath1() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new FileSearchCriteria(
            HOST_LABEL,
            "/appvol/invalid/$(rm -f)/path",
            KEYWORDS,
            FileType.FILE,
            MIN_FILE_SIZE,
            MAX_FILE_SIZE,
            MIN_MODIFIED_TIME,
            MAX_MODIFIED_TIME,
            MIN_CREATED_TIME,
            MAX_CREATED_TIME
        ));
    }


    @Test
    void testCreationWithOnlyRequiredParameters() {
        // Arrange & Act
        FileSearchCriteria criteria = new FileSearchCriteria(
            HOST_LABEL,
            FILE_PATH_PATTERN,
            null,
            null,
            0,
            0,
            null,
            null,
            null,
            null
        );

        // Assert
        assertNull(criteria.keywords());
        assertNull(criteria.fileType());
        assertEquals(0, criteria.minFileSize());
        assertEquals(0, criteria.maxFileSize());
        assertNull(criteria.minModifiedTime());
        assertNull(criteria.maxModifiedTime());
        assertNull(criteria.minCreatedTime());
        assertNull(criteria.maxCreatedTime());
    }

    @Test
    void testFilePathPatternValidation() {
        // Test valid patterns
        assertDoesNotThrow(() -> new FileSearchCriteria(
            HOST_LABEL,
            "/valid/path/to/file",
            null,
            null,
            0,
            0,
            null,
            null,
            null,
            null
        ));

        assertDoesNotThrow(() -> new FileSearchCriteria(
            HOST_LABEL,
            "/valid/*/to/file*",
            null,
            null,
            0,
            0,
            null,
            null,
            null,
            null
        ));

        assertDoesNotThrow(() -> new FileSearchCriteria(
            HOST_LABEL,
            "file_name_with_underscores.txt",
            null,
            null,
            0,
            0,
            null,
            null,
            null,
            null
        ));

        assertDoesNotThrow(() -> new FileSearchCriteria(
            HOST_LABEL,
            "file.with.dots.txt",
            null,
            null,
            0,
            0,
            null,
            null,
            null,
            null
        ));

        // Test invalid patterns
        assertThrows(IllegalArgumentException.class, () -> new FileSearchCriteria(
            HOST_LABEL,
            "..$(cmd)/path/traversal",
            null,
            null,
            0,
            0,
            null,
            null,
            null,
            null
        ));

        assertThrows(IllegalArgumentException.class, () -> new FileSearchCriteria(
            HOST_LABEL,
            "file with spaces.txt",
            null,
            null,
            0,
            0,
            null,
            null,
            null,
            null
        ));

        assertThrows(IllegalArgumentException.class, () -> new FileSearchCriteria(
            HOST_LABEL,
            "file*with?wildcards.txt",
            null,
            null,
            0,
            0,
            null,
            null,
            null,
            null
        ));
    }

    @Test
    void testTimeRangeValidation() {
        // Test valid time ranges
        FileSearchCriteria criteria = new FileSearchCriteria(
            HOST_LABEL,
            FILE_PATH_PATTERN,
            KEYWORDS,
            FileType.FILE,
            MIN_FILE_SIZE,
            MAX_FILE_SIZE,
            MIN_MODIFIED_TIME,
            MAX_MODIFIED_TIME,
            MIN_CREATED_TIME,
            MAX_CREATED_TIME
        );

        assertEquals(MIN_MODIFIED_TIME, criteria.minModifiedTime());
        assertEquals(MAX_MODIFIED_TIME, criteria.maxModifiedTime());
        assertEquals(MIN_CREATED_TIME, criteria.minCreatedTime());
        assertEquals(MAX_CREATED_TIME, criteria.maxCreatedTime());

        // Test null time ranges
        criteria = new FileSearchCriteria(
            HOST_LABEL,
            FILE_PATH_PATTERN,
            KEYWORDS,
            FileType.FILE,
            MIN_FILE_SIZE,
            MAX_FILE_SIZE,
            null,
            null,
            null,
            null
        );

        assertNull(criteria.minModifiedTime());
        assertNull(criteria.maxModifiedTime());
        assertNull(criteria.minCreatedTime());
        assertNull(criteria.maxCreatedTime());
    }
}
