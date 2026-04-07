package top.ilovemyhome.hosthelper.muserver.service.impl;

import org.apache.commons.lang3.RandomUtils;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.impl.PageImpl;
import top.ilovemyhome.zora.jdbi.page.impl.PageRequest;
import top.ilovemyhome.hosthelper.si.domain.FileSearchCriteria;
import top.ilovemyhome.hosthelper.si.domain.FileSearchResult;
import top.ilovemyhome.hosthelper.si.domain.FileType;
import top.ilovemyhome.hosthelper.muserver.service.FileSearchStrategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DummyFileSearchStrategy implements FileSearchStrategy {

    @Override
    public Page<FileSearchResult> search(FileSearchCriteria searchCriteria, PageRequest pageRequest) {
        int total = 118;
        List<FileSearchResult> result = generateTestData(total, pageRequest);
        return new PageImpl<>(result, pageRequest, total);
    }

    private List<FileSearchResult> generateTestData(int total, PageRequest pageRequest) {
        List<FileSearchResult> results = new ArrayList<>();
        long minSize = 10;
        long maxSize = 1024L * 1024 * 1024;
        for (int i = 1; i <= total; i++) {
            long sizeInBytes = RandomUtils.nextLong(minSize, maxSize + 1);
            results.add(new FileSearchResult(
                "host" + (i % 10 + 1), // hostLabel
                "testFile" + i + ".txt", // name
                "/path/to/testFile" + i + ".txt", // absolutePath
                i % 20 == 0 ? FileType.FILE : FileType.DIRECTORY, // type
                sizeInBytes, // sizeInBytes
                i % 2 == 0 ? "text" : "gzip",
                LocalDateTime.of(2025, 7, 14, 12, 0).plusMinutes(i), // lastModifiedTime
                LocalDateTime.of(2025, 7, 14, 10, 0).plusMinutes(i) // createdTime
            ));
        }

        return results.subList(pageRequest.getOffset(), Math.min(pageRequest.getOffset() + pageRequest.getPageSize(), results.size()));
    }
}
