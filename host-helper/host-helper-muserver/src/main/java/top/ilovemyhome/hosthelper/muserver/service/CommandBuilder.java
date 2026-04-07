package top.ilovemyhome.hosthelper.muserver.service;

import top.ilovemyhome.hosthelper.si.domain.FileSearchCriteria;

public interface CommandBuilder {

    String build(FileSearchCriteria searchCriteria);
}
