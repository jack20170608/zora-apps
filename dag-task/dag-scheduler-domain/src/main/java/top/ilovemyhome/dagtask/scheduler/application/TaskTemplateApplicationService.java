package top.ilovemyhome.dagtask.scheduler.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.scheduler.port.in.ManageTaskTemplateUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.QueryTaskTemplateUseCase;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskTemplateRepository;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;
import top.ilovemyhome.zora.jdbi.page.Page;
import top.ilovemyhome.zora.jdbi.page.Pageable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service handling both write and read operations on task templates.
 * <p>
 * Replaces the legacy {@code TaskTemplateServiceImpl}. Implements both
 * {@link ManageTaskTemplateUseCase} (write side) and {@link QueryTaskTemplateUseCase}
 * (read side) because the two share the same aggregate root and many private helpers.
 * </p>
 */
public class TaskTemplateApplicationService implements ManageTaskTemplateUseCase, QueryTaskTemplateUseCase {

    private static final Logger logger = LoggerFactory.getLogger(TaskTemplateApplicationService.class);

    private final TaskTemplateRepository taskTemplateRepository;

    public TaskTemplateApplicationService(TaskTemplateRepository taskTemplateRepository) {
        this.taskTemplateRepository = Objects.requireNonNull(taskTemplateRepository, "taskTemplateRepository must not be null");
    }

    @Override
    public boolean createTemplate(TaskTemplate template, boolean setActive) {
        if (template == null || template.getTemplateKey() == null || template.getTemplateKey().isBlank()
            || template.getVersion() == null || template.getVersion().isBlank()) {
            logger.warn("Cannot create template: missing required fields (key or version)");
            return false;
        }

        if (taskTemplateRepository.exists(template.getTemplateKey(), template.getVersion())) {
            logger.warn("Cannot create template: version already exists for key [{}], version [{}]",
                template.getTemplateKey(), template.getVersion());
            return false;
        }

        taskTemplateRepository.create(template);

        if (setActive) {
            taskTemplateRepository.deactivateOtherVersions(template.getTemplateKey(), template.getVersion());
        }

        logger.info("Created new template version: key=[{}], version=[{}], name=[{}], setActive=[{}]",
            template.getTemplateKey(), template.getVersion(), template.getTemplateName(), setActive);
        return true;
    }

    @Override
    public boolean updateTemplate(TaskTemplate template) {
        if (template == null || template.getTemplateKey() == null || template.getTemplateKey().isBlank()
            || template.getVersion() == null || template.getVersion().isBlank()) {
            logger.warn("Cannot update template: missing required fields (key or version)");
            return false;
        }

        Optional<TaskTemplate> existing = taskTemplateRepository.find(TaskTemplateSearchCriteria.builder()
            .withTemplateKey(template.getTemplateKey())
            .withVersion(template.getVersion())
            .build()).stream().findFirst();
        if (existing.isEmpty()) {
            logger.warn("Cannot update template: not found key=[{}], version=[{}]",
                template.getTemplateKey(), template.getVersion());
            return false;
        }

        template.incrementVersionSeq();
        taskTemplateRepository.update(template.getId(), template);
        logger.info("Updated template version: key=[{}], version=[{}], new sequence=[{}]",
            template.getTemplateKey(), template.getVersion(), template.getVersionSeq());
        return true;
    }

    @Override
    public boolean deactivateVersion(String templateKey, String version) {
        if (templateKey == null || templateKey.isBlank() || version == null || version.isBlank()) {
            logger.warn("Cannot deactivate: missing templateKey or version");
            return false;
        }

        if (!taskTemplateRepository.exists(templateKey, version)) {
            logger.warn("Cannot deactivate: template not found key=[{}], version=[{}]", templateKey, version);
            return false;
        }

        taskTemplateRepository.deactivateVersion(templateKey, version);
        logger.info("Deactivated template version: key=[{}], version=[{}]", templateKey, version);
        return true;
    }

    @Override
    public boolean deleteVersion(String templateKey, String version) {
        if (templateKey == null || templateKey.isBlank() || version == null || version.isBlank()) {
            logger.warn("Cannot delete: missing templateKey or version");
            return false;
        }

        if (!taskTemplateRepository.exists(templateKey, version)) {
            logger.warn("Cannot delete: template not found key=[{}], version=[{}]", templateKey, version);
            return false;
        }

        taskTemplateRepository.deleteByKeyAndVersion(templateKey, version);
        logger.info("Deleted template version: key=[{}], version=[{}]", templateKey, version);
        return true;
    }

    @Override
    public List<TaskTemplate> findAll(TaskTemplateSearchCriteria searchCriteria) {
        Objects.requireNonNull(searchCriteria, "searchCriteria must not be null");
        return taskTemplateRepository.find(searchCriteria);
    }

    @Override
    public Page<TaskTemplate> find(TaskTemplateSearchCriteria searchCriteria, Pageable page) {
        Objects.requireNonNull(searchCriteria, "searchCriteria must not be null");
        Objects.requireNonNull(page, "page must not be null");
        return taskTemplateRepository.find(searchCriteria, page);
    }
}
