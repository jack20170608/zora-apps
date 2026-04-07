package top.ilovemyhome.hosthelper.muserver.service;

import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.PageRequest;
import top.ilovemyhome.hosthelper.si.domain.FileSearchCriteria;
import top.ilovemyhome.hosthelper.si.domain.FileSearchResult;

public interface FileSearchStrategy {

    Page<FileSearchResult> search(FileSearchCriteria searchCriteria
        , PageRequest pageRequest);
}
