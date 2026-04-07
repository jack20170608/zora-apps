package top.ilovemyhome.hosthelper.si.domain;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public record FileSearchCriteria(
    String hostLabel
    , String filePathPattern
    , String keywords
    , FileType fileType
    , long minFileSize
    , long maxFileSize
    , LocalDateTime minModifiedTime
    , LocalDateTime maxModifiedTime
    , LocalDateTime minCreatedTime
    , LocalDateTime maxCreatedTime) {

    private static final Pattern SAFE_PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9/._*-]+$");

    public FileSearchCriteria {
        if (StringUtils.isNotEmpty(filePathPattern)
            && !SAFE_PATH_PATTERN.matcher(filePathPattern).matches()) {
            throw new IllegalArgumentException("filePathPattern contains illegal characters");
        }
    }


}
