package top.ilovemyhome.hosthelper.muserver.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.impl.PageImpl;
import top.ilovemyhome.zora.jdbi.page.impl.PageRequest;
import top.ilovemyhome.hosthelper.si.domain.FileSearchCriteria;
import top.ilovemyhome.hosthelper.si.domain.FileSearchResult;
import top.ilovemyhome.hosthelper.si.domain.FileType;
import top.ilovemyhome.hosthelper.muserver.service.FileSearchStrategy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JavaFileSearchStrategy implements FileSearchStrategy {

    @Override
    public Page<FileSearchResult> search(FileSearchCriteria searchCriteria, PageRequest pageRequest) {
        List<FileSearchResult> allResults = new ArrayList<>();
        String directoryPath = ".";
        String fileNamePattern = null;

        File rootDir = new File(directoryPath);
        //The keywords
        String keywords = searchCriteria.keywords();
        if (rootDir.exists() && rootDir.isDirectory()) {
            if (StringUtils.isBlank(keywords)) {
                File[] files = rootDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (matchesCriteria(file, searchCriteria, fileNamePattern)) {
                            try {
                                Path filePath = Paths.get(file.getAbsolutePath());
                                BasicFileAttributes attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
                                FileType type = file.isDirectory() ? FileType.DIRECTORY : FileType.FILE;
                                long sizeInBytes = file.isDirectory() ? 0 : file.length();
                                String contentType = file.isDirectory() ? "directory" : getFileExtension(file.getName());

                                FileSearchResult result = new FileSearchResult(
                                    searchCriteria.hostLabel(),
                                    file.getName(),
                                    file.getAbsolutePath(),
                                    type,
                                    sizeInBytes,
                                    contentType,
                                    LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), java.time.ZoneId.systemDefault()),
                                    LocalDateTime.ofInstant(attributes.creationTime().toInstant(), java.time.ZoneId.systemDefault())
                                );
                                allResults.add(result);
                            } catch (IOException e) {
                                logger.error("Error reading file attributes for {}", file.getAbsolutePath(), e);
                            }
                        }
                    }
                }
            } else {
                throw new UnsupportedOperationException("The search strategy does not support keywords search!");
            }
        }
        int fromIndex = pageRequest.getOffset();
        int toIndex = Math.min(fromIndex + pageRequest.getPageSize(), allResults.size());
        List<FileSearchResult> paginatedResults = allResults.subList(fromIndex, toIndex);
        return new PageImpl<>(paginatedResults, pageRequest, allResults.size());
    }

    private boolean matchesCriteria(File file, FileSearchCriteria searchCriteria, String fileNamePattern) {
        if (fileNamePattern != null && !fileNamePattern.isBlank()) {
            String regexPattern = fileNamePattern.replace("*", ".*");
            if (!file.getName().matches(regexPattern)) {
                return false;
            }
        }
        if (!file.isDirectory()) {
            if (file.length() < searchCriteria.minFileSize()) {
                return false;
            }
            if (searchCriteria.maxFileSize() > 0L && file.length() > searchCriteria.maxFileSize()) {
                return false;
            }
        }
        try {
            Path filePath = Paths.get(file.getAbsolutePath());
            BasicFileAttributes attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
            LocalDateTime lastModified = LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), java.time.ZoneId.systemDefault());
            LocalDateTime createdTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant(), java.time.ZoneId.systemDefault());

            if (searchCriteria.minModifiedTime() != null && lastModified.isBefore(searchCriteria.minModifiedTime())) {
                return false;
            }
            if (searchCriteria.maxModifiedTime() != null && lastModified.isAfter(searchCriteria.maxModifiedTime())) {
                return false;
            }
            if (searchCriteria.minCreatedTime() != null && createdTime.isBefore(searchCriteria.minCreatedTime())) {
                return false;
            }
            if (searchCriteria.maxCreatedTime() != null && createdTime.isAfter(searchCriteria.maxCreatedTime())) {
                return false;
            }
        } catch (IOException e) {
            logger.error("Error reading file attributes for {}", file.getAbsolutePath(), e);
        }
        return true;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? "" : fileName.substring(dotIndex + 1);
    }

    private static final Logger logger = LoggerFactory.getLogger(JavaFileSearchStrategy.class);
}
