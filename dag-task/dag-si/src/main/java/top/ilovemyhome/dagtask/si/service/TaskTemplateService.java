package top.ilovemyhome.dagtask.si.service;

import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.List;

public interface TaskTemplateService {

    boolean createTemplate(TaskTemplate template, boolean setActive);

    boolean updateTemplate(TaskTemplate template);

    boolean deactivateVersion(String templateKey, String version);

    boolean deleteVersion(String templateKey, String version);

    List<TaskTemplate> findAll(TaskTemplateSearchCriteria searchCriteria);

    Page<TaskTemplate> find(TaskTemplateSearchCriteria searchCriteria, Pageable page);
}
