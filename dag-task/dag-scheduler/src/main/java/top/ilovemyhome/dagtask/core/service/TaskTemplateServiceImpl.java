package top.ilovemyhome.dagtask.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;
import top.ilovemyhome.dagtask.si.persistence.TaskTemplateDao;
import top.ilovemyhome.dagtask.si.service.TaskTemplateService;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TaskTemplateServiceImpl implements TaskTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TaskTemplateServiceImpl.class);

    private final TaskTemplateDao taskTemplateDao;

    /**
     * Creates a TaskTemplateServiceImpl with required dependencies.
     *
     * @param taskTemplateDao DAO for template persistence
     */
    public TaskTemplateServiceImpl(TaskTemplateDao taskTemplateDao) {
        this.taskTemplateDao = Objects.requireNonNull(taskTemplateDao, "taskTemplateDao must not be null");
    }

    @Override
    public boolean createTemplate(TaskTemplate template, boolean setActive) {
        if (template == null || template.getTemplateKey().isBlank() || template.getVersion().isBlank()) {
            logger.warn("Cannot create template: missing required fields (key or version)");
            return false;
        }

        if (taskTemplateDao.exists(template.getTemplateKey(), template.getVersion())) {
            logger.warn("Cannot create template: version already exists for key [{}], version [{}]",
                template.getTemplateKey(), template.getVersion());
            return false;
        }

        taskTemplateDao.create(template);

        if (setActive) {
            // Deactivate all other versions of this template
            taskTemplateDao.deactivateOtherVersions(template.getTemplateKey(), template.getVersion());
        }

        logger.info("Created new template version: key=[{}], version=[{}], name=[{}], setActive=[{}]",
            template.getTemplateKey(), template.getVersion(), template.getTemplateName(), setActive);
        return true;
    }

    @Override
    public boolean updateTemplate(TaskTemplate template) {
        if (template == null || template.getTemplateKey().isBlank() || template.getVersion().isBlank()) {
            logger.warn("Cannot update template: missing required fields (key or version)");
            return false;
        }

        Optional<TaskTemplate> existing = taskTemplateDao.find(TaskTemplateSearchCriteria.builder()
                .withTemplateKey(template.getTemplateKey())
                .withVersion(template.getVersion())
                .build()).stream().findFirst();
        if (existing.isEmpty()) {
            logger.warn("Cannot update template: not found key=[{}], version=[{}]",
                template.getTemplateKey(), template.getVersion());
            return false;
        }

        template.incrementVersionSeq();
        taskTemplateDao.update(template.getId(), template);
        logger.info("Updated template version: key=[{}], version=[{}], new sequence=[{}]",
            template.getTemplateKey(), template.getVersion(), template.getVersionSeq());
        return true;
    }

    @Override
    public boolean deactivateVersion(String templateKey, String version) {
        if (templateKey.isBlank() || version.isBlank()) {
            logger.warn("Cannot deactivate: missing templateKey or version");
            return false;
        }

        if (!taskTemplateDao.exists(templateKey, version)) {
            logger.warn("Cannot deactivate: template not found key=[{}], version=[{}]",
                templateKey, version);
            return false;
        }

        taskTemplateDao.deactivateVersion(templateKey, version);
        logger.info("Deactivated template version: key=[{}], version=[{}]", templateKey, version);
        return true;
    }

    @Override
    public boolean deleteVersion(String templateKey, String version) {
        if (templateKey.isBlank() || version.isBlank()) {
            logger.warn("Cannot delete: missing templateKey or version");
            return false;
        }

        if (!taskTemplateDao.exists(templateKey, version)) {
            logger.warn("Cannot delete: template not found key=[{}], version=[{}]",
                templateKey, version);
            return false;
        }

        taskTemplateDao.deleteByKeyAndVersion(templateKey, version);
        logger.info("Deleted template version: key=[{}], version=[{}]", templateKey, version);
        return true;
    }

    @Override
    public List<TaskTemplate> findAll(TaskTemplateSearchCriteria searchCriteria) {
        Objects.requireNonNull(searchCriteria, "searchCriteria must not be null");
        return taskTemplateDao.find(searchCriteria);
    }

    @Override
    public Page<TaskTemplate> find(TaskTemplateSearchCriteria searchCriteria, Pageable page) {
        Objects.requireNonNull(searchCriteria, "searchCriteria must not be null");
        Objects.requireNonNull(page, "page must not be null");
        return taskTemplateDao.find(searchCriteria, page);
    }
}
