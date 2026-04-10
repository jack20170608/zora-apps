package top.ilovemyhome.dagtask.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.enums.OrderType;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.persistence.TaskTemplateDao;
import top.ilovemyhome.dagtask.si.service.DagManager;
import top.ilovemyhome.dagtask.si.service.TaskTemplateService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of {@link TaskTemplateService} with database persistence.
 * <p>
 * <b>Responsibility:</b> Only manages template CRUD and version control.
 * Actual creation of concrete {@link top.ilovemyhome.dagtask.si.TaskRecord} tasks from template
 * is delegated to {@link DagManager}.
 * </p>
 */
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TaskTemplateServiceImpl.class);

    private final TaskTemplateDao taskTemplateDao;
    private final TaskOrderDao taskOrderDao;
    private final DagManager dagManager;
    private final ObjectMapper objectMapper;

    /**
     * Pattern for parameter placeholders in template configuration: {{paramName}}
     */
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_\\-.]+)\\}\\}");

    /**
     * Creates a TaskTemplateServiceImpl with required dependencies.
     *
     * @param taskTemplateDao DAO for template persistence
     * @param taskOrderDao DAO for task order persistence
     * @param dagManager Manager for creating concrete DAG tasks
     * @param objectMapper JSON mapper for processing DAG definition
     */
    public TaskTemplateServiceImpl(TaskTemplateDao taskTemplateDao,
                                   TaskOrderDao taskOrderDao,
                                   DagManager dagManager,
                                   ObjectMapper objectMapper) {
        this.taskTemplateDao = Objects.requireNonNull(taskTemplateDao, "taskTemplateDao must not be null");
        this.taskOrderDao = Objects.requireNonNull(taskOrderDao, "taskOrderDao must not be null");
        this.dagManager = Objects.requireNonNull(dagManager, "dagManager must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
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

        Optional<TaskTemplate> existing = taskTemplateDao.findByKeyAndVersion(
            template.getTemplateKey(), template.getVersion());
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
    public List<TaskTemplate> getVersions(String templateKey) {
        return taskTemplateDao.findByTemplateKey(templateKey);
    }

    @Override
    public Optional<TaskTemplate> getActive(String templateKey) {
        return taskTemplateDao.findActiveByTemplateKey(templateKey);
    }

    @Override
    public Optional<TaskTemplate> getByVersion(String templateKey, String version) {
        return taskTemplateDao.findByKeyAndVersion(templateKey, version);
    }

    @Override
    public List<TaskTemplate> getAllActive() {
        return taskTemplateDao.findAllActive();
    }

    @Override
    public List<TaskTemplate> getAll() {
        return taskTemplateDao.findAll();
    }
}
