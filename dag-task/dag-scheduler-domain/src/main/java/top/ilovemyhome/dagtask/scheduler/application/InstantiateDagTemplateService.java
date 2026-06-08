package top.ilovemyhome.dagtask.scheduler.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.scheduler.domain.dag.DagInstantiator;
import top.ilovemyhome.dagtask.scheduler.port.in.InstantiateDagTemplateUseCase;
import top.ilovemyhome.dagtask.scheduler.domain.dag.DagDefinition;
import top.ilovemyhome.dagtask.scheduler.port.out.DagDefinitionParser;
import top.ilovemyhome.dagtask.scheduler.port.out.IdGenerator;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskOrderRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskRecordRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskTemplateRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.UnitOfWork;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.TaskTemplate;
import top.ilovemyhome.dagtask.si.dto.TaskTemplateSearchCriteria;
import top.ilovemyhome.dagtask.si.enums.OrderType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service for instantiating a concrete DAG run from a stored template.
 * <p>
 * Replaces {@code DagManageService.instantiateFromTemplate} (both overloads).
 * JSON parsing of the template's stored definition + parameter schema is delegated
 * to the {@link DagDefinitionParser} outbound port — the domain itself stays
 * Jackson-free.
 * </p>
 */
public class InstantiateDagTemplateService implements InstantiateDagTemplateUseCase {

    private static final Logger logger = LoggerFactory.getLogger(InstantiateDagTemplateService.class);

    private final TaskOrderRepository taskOrderRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final TaskRecordRepository taskRecordRepository;
    private final UnitOfWork unitOfWork;
    private final IdGenerator idGenerator;
    private final DagDefinitionParser parser;

    public InstantiateDagTemplateService(TaskOrderRepository taskOrderRepository,
                                         TaskTemplateRepository taskTemplateRepository,
                                         TaskRecordRepository taskRecordRepository,
                                         UnitOfWork unitOfWork,
                                         IdGenerator idGenerator,
                                         DagDefinitionParser parser) {
        this.taskOrderRepository = Objects.requireNonNull(taskOrderRepository, "taskOrderRepository must not be null");
        this.taskTemplateRepository = Objects.requireNonNull(taskTemplateRepository, "taskTemplateRepository must not be null");
        this.taskRecordRepository = Objects.requireNonNull(taskRecordRepository, "taskRecordRepository must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
    }

    @Override
    public Optional<TaskOrder> instantiateFromTemplate(String templateKey, String orderKey,
                                                       String orderName, Map<String, String> parameters) {
        return instantiateFromTemplate(templateKey, null, orderKey, orderName, parameters);
    }

    @Override
    public Optional<TaskOrder> instantiateFromTemplate(String templateKey, String version,
                                                       String orderKey, String orderName,
                                                       Map<String, String> parameters) {
        Objects.requireNonNull(orderKey, "orderKey must not be null");
        Objects.requireNonNull(orderName, "orderName must not be null");

        if (orderKey.isBlank()) {
            logger.warn("Cannot instantiate: orderKey is blank");
            return Optional.empty();
        }

        if (taskOrderRepository.findByKey(orderKey).isPresent()) {
            logger.warn("Cannot instantiate: task order with key [{}] already exists", orderKey);
            return Optional.empty();
        }

        Optional<TaskTemplate> templateOpt = findTemplate(templateKey, version);
        if (templateOpt.isEmpty()) {
            logger.warn("Cannot instantiate: template not found key=[{}], version=[{}]", templateKey, version);
            return Optional.empty();
        }

        TaskTemplate template = templateOpt.get();
        String actualVersion = version != null ? version : template.getVersion();

        // Merge user parameters with schema defaults via outbound parser
        Map<String, String> resolvedParameters = resolveParameters(template, parameters);

        // Build TaskOrder attributes
        Map<String, String> attributes = new HashMap<>();
        attributes.put("sourceTemplateKey", templateKey);
        attributes.put("sourceTemplateVersion", actualVersion);
        if (resolvedParameters != null) {
            attributes.putAll(resolvedParameters);
        }

        TaskOrder order = TaskOrder.builder()
            .withKey(orderKey)
            .withName(orderName)
            .withOrderType(OrderType.INSTANTIATED_FROM_TEMPLATE)
            .withAttributes(attributes)
            .build();

        // Parse the template's DAG definition (JSON) via outbound port
        DagDefinition definition;
        try {
            definition = parser.parseDag(template.getDagDefinition());
        } catch (RuntimeException e) {
            logger.error("Failed to parse DAG definition from template: key=[{}], version=[{}]",
                templateKey, actualVersion, e);
            return Optional.empty();
        }

        // Build task records via domain helper (IDs + successor links)
        List<TaskRecord> tasks = DagInstantiator.instantiate(definition, orderKey, resolvedParameters, idGenerator::nextTaskId);

        unitOfWork.execute(() -> {
            taskOrderRepository.create(order);
            for (TaskRecord r : tasks) {
                taskRecordRepository.create(r);
            }
        });

        logger.info("Instantiated new task order from template: templateKey=[{}], templateVersion=[{}], orderKey=[{}], orderName=[{}]",
            templateKey, actualVersion, orderKey, orderName);

        return Optional.of(order);
    }

    private Optional<TaskTemplate> findTemplate(String templateKey, String version) {
        return taskTemplateRepository.find(TaskTemplateSearchCriteria.builder()
            .withTemplateKey(templateKey)
            .withVersion(version)
            .build()).stream().findFirst();
    }

    private Map<String, String> resolveParameters(TaskTemplate template, Map<String, String> userParameters) {
        String schemaJson = template.getParameterSchema();
        if (schemaJson == null || schemaJson.isBlank()) {
            return userParameters;
        }

        Map<String, String> defaults;
        try {
            defaults = parser.parseParameterDefaults(schemaJson);
        } catch (RuntimeException e) {
            logger.warn("Failed to parse parameter schema for template key=[{}], version=[{}], using user parameters as-is",
                template.getTemplateKey(), template.getVersion(), e);
            return userParameters;
        }

        Map<String, String> resolved = new HashMap<>(defaults != null ? defaults : Map.of());
        if (userParameters != null) {
            resolved.putAll(userParameters);
        }
        return resolved;
    }
}
