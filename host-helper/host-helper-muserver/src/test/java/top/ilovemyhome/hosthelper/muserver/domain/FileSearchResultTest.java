package top.ilovemyhome.hosthelper.muserver.domain;

import org.junit.jupiter.api.Test;

public class FileSearchResultTest {

    @Test
    public void FileSearchResultTest() {
        FileSearchResult fileSearchResult
            = FileSearchResult.fromFindResult("d 2025-1 /home/jack/java-io/2025-1 6")
            .build();
        assertThat(fileSearchResult.type()).isEqualTo(FileType.DIRECTORY);
        assertThat(fileSearchResult.name()).isEqualTo("2025-1");
        assertThat(fileSearchResult.absolutePath()).isEqualTo("/home/jack/java-io/2025-1");
        assertThat(fileSearchResult.sizeInBytes()).isEqualTo(6);
    }

    @Test
    public void FileSearchResultTest2() {
        FileSearchResult fileSearchResult
            = FileSearchResult.fromFindResult("f 2025-2.csv /home/jack/java-io/2025-2.csv 1234")
           .build();
        assertThat(fileSearchResult.type()).isEqualTo(FileType.FILE);
        assertThat(fileSearchResult.name()).isEqualTo("2025-2.csv");
        assertThat(fileSearchResult.absolutePath()).isEqualTo("/home/jack/java-io/2025-2.csv");
        assertThat(fileSearchResult.sizeInBytes()).isEqualTo(1234);
    }

    @Test
    public void FileSearchResultTest3() {
        FileSearchResult fileSearchResult
            = FileSearchResult.fromGrepResult("-rw-r--r-- 1 jack jack 18 Jul 20 11:04 /home/jack/java-io/2025-1.csv")
           .build();
        assertThat(fileSearchResult.type()).isEqualTo(FileType.FILE);
        assertThat(fileSearchResult.name()).isEqualTo("2025-1.csv");
        assertThat(fileSearchResult.absolutePath()).isEqualTo("/home/jack/java-io/2025-1.csv");
        assertThat(fileSearchResult.sizeInBytes()).isEqualTo(18);
    }

}
