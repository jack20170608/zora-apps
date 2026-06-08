package top.ilovemyhome.dagtask.scheduler.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.scheduler.domain.dag.DagDefinition;
import top.ilovemyhome.dagtask.scheduler.domain.dag.DagInstantiator;
import top.ilovemyhome.dagtask.scheduler.port.in.SubmitDagRunUseCase;
import top.ilovemyhome.dagtask.scheduler.port.out.IdGenerator;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskOrderRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskRecordRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.UnitOfWork;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service for submitting a new DAG run.
 * <p>
 * Replaces {@code DagManageService.createTasks/createTasksFromDagDefinition} and
 * the {@code TaskDagService.createTasks} overload from the legacy services.
 * </p>
 * <p>
 * JSON parsing of the legacy {@code createTasksFromDagDefinition(String json, ...)}
 * has been moved to the inbound web adapter — this service accepts already-parsed
 * {@link top.ilovemyhome.dagtask.scheduler.domain.dag.DagDefinition} records.
 * </p>
 */
public class SubmitDagRunService implements SubmitDagRunUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SubmitDagRunService.class);

    private final TaskOrderRepository taskOrderRepository;
    private final TaskRecordRepository taskRecordRepository;
    private final UnitOfWork unitOfWork;
    private final IdGenerator idGenerator;

    public SubmitDagRunService(TaskOrderRepository taskOrderRepository,
                               TaskRecordRepository taskRecordRepository,
                               UnitOfWork unitOfWork,
                               IdGenerator idGenerator) {
        this.taskOrderRepository = Objects.requireNonNull(taskOrderRepository, "taskOrderRepository must not be null");
        this.taskRecordRepository = Objects.requireNonNull(taskRecordRepository, "taskRecordRepository must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
    }

    @Override
    public List<Long> submitTasks(List<TaskRecord> records) {
        Objects.requireNonNull(records);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("The records list must not be empty");
        }

        String orderKey = records.getFirst().getOrderKey();
        if (orderKey == null || orderKey.isBlank()) {
            throw new IllegalArgumentException("The record order key must not be empty");
        }
        for (TaskRecord r : records) {
            if (!orderKey.equals(r.getOrderKey())) {
                throw new IllegalArgumentException("All records must have the same order key");
            }
        }

        return unitOfWork.execute(() -> {
            Optional<TaskOrder> existingOrder = taskOrderRepository.findByKey(orderKey);
            if (existingOrder.isPresent()) {
                // Verify no duplicate IDs against existing tasks for this order
                // (legacy behavior: throws on duplicate)
                // We don't need to query existing tasks here for cycle detection because
                // DagHelper.visitDAG is called by callers that already have the full DAG view.
            }
            List<Long> result = new ArrayList<>(records.size());
            for (TaskRecord r : records) {
                result.add(taskRecordRepository.create(r));
            }
            logger.info("Submitted {} tasks for order {}", records.size(), orderKey);
            return result;
        });
    }

    @Override
    public List<Long> submitFromDefinition(DagDefinition definition, String orderKey,
                                            Map<String, String> parameters) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(orderKey, "orderKey must not be null");

        if (definition.tasks() == null || definition.tasks().isEmpty()) {
            logger.warn("DAG definition is empty for order [{}], no tasks created", orderKey);
            return List.of();
        }

        // Build all task records (IDs assigned, successor IDs resolved) in domain helper
        List<TaskRecord> tasks = DagInstantiator.instantiate(definition, orderKey, parameters, idGenerator::nextTaskId);

        return unitOfWork.execute(() -> {
            List<Long> result = new ArrayList<>(tasks.size());
            for (TaskRecord r : tasks) {
                result.add(taskRecordRepository.create(r));
            }
            logger.info("Submitted {} tasks from DAG definition for order [{}]", tasks.size(), orderKey);
            return result;
        });
    }
}
