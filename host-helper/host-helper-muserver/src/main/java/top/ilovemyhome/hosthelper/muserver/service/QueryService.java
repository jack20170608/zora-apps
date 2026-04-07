package top.ilovemyhome.hosthelper.muserver.service;

import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.impl.PageRequest;
import top.ilovemyhome.hosthelper.si.domain.FileSearchCriteria;
import top.ilovemyhome.hosthelper.si.domain.FileSearchResult;
import top.ilovemyhome.hosthelper.si.domain.HostItem;

import java.util.List;
import java.util.Map;

public interface QueryService {

    Map<String, List<HostItem>> getAllHosts();

    Page<FileSearchResult> search(FileSearchCriteria searchCriteria, PageRequest pageRequest);
}
