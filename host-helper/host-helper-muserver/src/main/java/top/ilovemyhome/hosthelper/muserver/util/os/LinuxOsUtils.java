package top.ilovemyhome.hosthelper.muserver.util.os;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class LinuxOsUtils {

    public static final String DEFAULT_FILE_PATH_PATTERN = System.getProperty("user.dir");
    public static final String DEFAULT_FILE_NAME_PATTERN = "*";

    public static boolean isValidLinuxPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (path.indexOf('\0') != -1) {
            return false;
        }
        if (path.contains("//")) {
            return false;
        }
        if (path.getBytes().length > 4096) {
            return false;
        }
        String[] components = path.split("/");
        for (String component : components) {
            if (component.getBytes().length > 255) {
                return false;
            }
        }
        return true;
    }

    public static List<String> parsePath(String filePathPattern) {
        String searchDir;
        String searchFileNamePattern;
        if (StringUtils.isBlank(filePathPattern)){
            searchDir = DEFAULT_FILE_PATH_PATTERN;
            searchFileNamePattern = DEFAULT_FILE_NAME_PATTERN;
        }else {
            if (!LinuxOsUtils.isValidLinuxPath(filePathPattern)){
                throw new IllegalArgumentException("filePathPattern is not a valid linux path!");
            }
            boolean isAbsolutePath = filePathPattern.startsWith("/");
            if (!isAbsolutePath){
                filePathPattern = DEFAULT_FILE_PATH_PATTERN + "/" + filePathPattern;
            }
            int lastSlashIndex = filePathPattern.lastIndexOf('/');
            if (lastSlashIndex != -1) {
                searchDir = filePathPattern.substring(0, lastSlashIndex);
                searchFileNamePattern = filePathPattern.substring(lastSlashIndex + 1);
            } else {
                searchDir = DEFAULT_FILE_PATH_PATTERN;
                searchFileNamePattern = filePathPattern;
            }
            if (StringUtils.isBlank(searchFileNamePattern)){
                searchFileNamePattern = DEFAULT_FILE_NAME_PATTERN;
            }
        }
        return List.of(searchDir, searchFileNamePattern);
    }
}
