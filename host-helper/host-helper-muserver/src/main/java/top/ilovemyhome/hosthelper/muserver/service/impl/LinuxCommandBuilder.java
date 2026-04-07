package top.ilovemyhome.hosthelper.muserver.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zora.common.system.OSUtil;
import top.ilovemyhome.zora.common.system.SystemCommandChecker;
import top.ilovemyhome.zora.common.util.CollectionUtil;
import top.ilovemyhome.hosthelper.si.domain.FileSearchCriteria;
import top.ilovemyhome.hosthelper.si.domain.FileType;
import top.ilovemyhome.hosthelper.muserver.service.CommandBuilder;
import top.ilovemyhome.hosthelper.muserver.util.os.LinuxOsUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class LinuxCommandBuilder implements CommandBuilder {

    private static final Logger logger = LoggerFactory.getLogger(LinuxCommandBuilder.class);
    private static final int DEFAULT_SEARCH_DEPTH = 1;


    @Override
    public String build(FileSearchCriteria searchCriteria) {
        logger.info("Building command for search criteria: [{}].", searchCriteria.toString());
        List<String> parsedPaths = parsePath(searchCriteria);
        StringBuilder builder = new StringBuilder();
        String searchDir = parsedPaths.get(0);
        String searchFileNamePattern= parsedPaths.get(1);
        String keywords = searchCriteria.keywords();
        Path searchPath = Paths.get(searchDir);
        if (!Files.exists(searchPath) || !Files.isDirectory(searchPath)){
            throw new IllegalArgumentException("The search directory is not a valid directory!");
        }
        //The search path need absolute path
        if (!searchPath.isAbsolute()){
            throw new IllegalArgumentException("The search directory is not a absolute directory!");
        }
        // Construct the find command
        builder.append("find ").append(searchDir);
        builder.append(" -maxdepth 1");

        FileType fileType = searchCriteria.fileType();
        //Force the file type to file only if input keyword
        if (StringUtils.isNotBlank(keywords)){
            fileType = FileType.FILE;
        }
        //Limit the file type
        switch (fileType){
            case DIRECTORY -> builder.append(" -type d");
            case FILE -> builder.append(" -type f");
            case null, default -> builder.append(" '(' -type f -o -type d ')'");
        }
        if (StringUtils.isNotBlank(searchFileNamePattern)) {
            builder.append(" -name '").append(searchFileNamePattern).append("'");
        }
        // If keywords are provided, append the grep command
        //find `pwd` -maxdepth 1 -type f -name '2025*' -exec grep "foo" -HIl {} + 2>/dev/null | xargs ls -l
        if (StringUtils.isNotBlank(keywords)) {
            builder.append(" -exec grep -HIl ").append("'").append(keywords).append("'")
                .append(" {} +")
                .append(" 2>/dev/null | xargs ls -l");
        }else {
            //Output
            // %y filetype
            // %f the base name
            // %p the absolute path
            // %s file size
            builder.append(" -printf '%y %f %p %s\\n'");
        }
        //The last
        logger.info("Generated command: [\n{}\n]", builder);
        return builder.toString();

    }

    private List<String> parsePath(FileSearchCriteria searchCriteria){
        boolean isLinux = OSUtil.isLinux();
        if (!isLinux) {
            throw new UnsupportedOperationException("This method is only supported on Linux System.");
        }
        if (!isFindCommandAvailable()){
            throw new UnsupportedOperationException("The find command is not available.");
        }
        if (!isGrepCommandAvailable() && StringUtils.isNotBlank(searchCriteria.keywords())){
            throw new UnsupportedOperationException("The grep command is not available, Could not support content search.");
        }
        List<String> parsedPaths = LinuxOsUtils.parsePath(searchCriteria.filePathPattern());
        if (CollectionUtil.isEmpty(parsedPaths) || parsedPaths.size() != 2){
            throw new IllegalArgumentException("File path pattern is not valid!");
        }
        String searchDir = parsedPaths.get(0);
        String searchFileNamePattern = parsedPaths.get(1);
        if (StringUtils.isBlank(searchDir) || StringUtils.isBlank(searchFileNamePattern)){
            throw new IllegalArgumentException("File path pattern is not valid!");
        }
        return parsedPaths;
    }

    private boolean isFindCommandAvailable(){
        return SystemCommandChecker.isCommandAvailable("find");
    }

    private boolean isGrepCommandAvailable(){
        return SystemCommandChecker.isCommandAvailable("grep");
    }


}
