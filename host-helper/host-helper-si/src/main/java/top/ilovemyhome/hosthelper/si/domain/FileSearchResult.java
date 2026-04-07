package top.ilovemyhome.hosthelper.si.domain;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public record FileSearchResult(
    String hostLabel
    , String name
    , String absolutePath
    , FileType type
    , long sizeInBytes
    , String contentType
    , LocalDateTime lastModifiedTime
    , LocalDateTime createdTime) {

    private static final Logger logger = LoggerFactory.getLogger(FileSearchResult.class);

    public enum Fields {
        HOST_LABEL,
        NAME,
        ABSOLUTE_PATH,
        TYPE,
        SIZE_IN_BYTES,
        CONTENT_TYPE,
        LAST_MODIFIED_TIME,
        CREATED_TIME
    }

    /*
    find `pwd` -maxdepth 1 -name '2025*' -printf '%y %f %p %s\n'

    d 2025-1 /home/jack/java-io/2025-1 6
    d 2025-2 /home/jack/java-io/2025-2 6
    d 2025-3 /home/jack/java-io/2025-3 6
    d 2025-4 /home/jack/java-io/2025-4 6
    d 2025-5 /home/jack/java-io/2025-5 6
    d 2025-6 /home/jack/java-io/2025-6 6
    d 2025-7 /home/jack/java-io/2025-7 6
    d 2025-8 /home/jack/java-io/2025-8 6
    d 2025-9 /home/jack/java-io/2025-9 6
    d 2025-10 /home/jack/java-io/2025-10 6
    d 2025-11 /home/jack/java-io/2025-11 6
    d 2025-12 /home/jack/java-io/2025-12 6
    f 2025-1.csv /home/jack/java-io/2025-1.csv 0
    f 2025-2.csv /home/jack/java-io/2025-2.csv 0
    */
    //if need more info, need to change the find command
    public static Builder fromFindResult(String line) {
        String[] parts = line.trim().split(" ", -1);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Missing required fields in find result");
        }
        String fileTypeStr = parts[0];
        FileType fileType;
        switch (fileTypeStr) {
            case "d":
                fileType = FileType.DIRECTORY;
                break;
            case "f":
                fileType = FileType.FILE;
                break;
            default:
                fileType = FileType.OTHER;
                break;
        }
        return FileSearchResult.builder()
            .withType(fileType)
            .withName(parts[1])
            .withAbsolutePath(parts[2])
            .withSizeInBytes(Long.parseLong(parts[3]))
            ;
    }

    /*
-rw-r--r-- 1 jack jack 18 Jul 20 11:04 /home/jack/java-io/2025-1.csv
-rw-r--r-- 1 jack jack 16 Jul 20 11:04 /home/jack/java-io/2025-2.csv
     */
    public static Builder fromGrepResult(String line) {
        String[] parts = line.trim().split("\\s+", 9);
        logger.debug("Part[4] is [{}].", parts[4]);
        if (parts.length < 9) {
            throw new IllegalArgumentException("Missing required fields in ls result");
        }

        FileType fileType = FileType.FILE;
        String fullPath = parts[8];
        String name = StringUtils.substringAfterLast(fullPath, "/");
        long sizeInBytes = Long.parseLong(parts[4]);
//         解析最后修改时间
//        String dateTimeStr = parts[5] + " " + parts[6] + " " + parts[7].substring(0, parts[7].indexOf(" "));
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d HH:mm");
//        LocalDateTime lastModifiedTime = LocalDateTime.parse(dateTimeStr, formatter);

        return FileSearchResult.builder()
            .withType(fileType)
            .withName(name)
            .withAbsolutePath(fullPath)
            .withSizeInBytes(sizeInBytes)
//            .withLastModifiedTime(lastModifiedTime)
            ;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String hostLabel;
        private String name;
        private String absolutePath;
        private FileType type;
        private long sizeInBytes;
        private String contentType;
        private LocalDateTime lastModifiedTime;
        private LocalDateTime createdTime;

        private Builder() {
        }


        public Builder withHostLabel(String hostLabel) {
            this.hostLabel = hostLabel;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withAbsolutePath(String absolutePath) {
            this.absolutePath = absolutePath;
            return this;
        }

        public Builder withType(FileType type) {
            this.type = type;
            return this;
        }

        public Builder withSizeInBytes(long sizeInBytes) {
            this.sizeInBytes = sizeInBytes;
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withLastModifiedTime(LocalDateTime lastModifiedTime) {
            this.lastModifiedTime = lastModifiedTime;
            return this;
        }

        public Builder withCreatedTime(LocalDateTime createdTime) {
            this.createdTime = createdTime;
            return this;
        }

        public FileSearchResult build() {
            return new FileSearchResult(hostLabel, name, absolutePath, type, sizeInBytes, contentType, lastModifiedTime, createdTime);
        }
    }
}
