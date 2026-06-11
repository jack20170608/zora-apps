package top.ilovemyhome.dagtask.scheduler.application;

import top.ilovemyhome.dagtask.scheduler.port.in.ManageTaskOrderUseCase;
import top.ilovemyhome.dagtask.scheduler.port.in.QueryTaskOrderUseCase;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskOrderRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.TaskRecordRepository;
import top.ilovemyhome.dagtask.scheduler.port.out.UnitOfWork;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static top.ilovemyhome.dagtask.si.enums.TaskStatus.HOLD;
import static top.ilovemyhome.dagtask.si.enums.TaskStatus.READY;
import static top.ilovemyhome.dagtask.si.enums.TaskStatus.RUNNING;

/**
 * Application service handling both write and read operations on task orders.
 * <p>
 * Replaces the legacy {@code TaskOrderServiceImpl}. Implements both
 * {@link ManageTaskOrderUseCase} (write side) and {@link QueryTaskOrderUseCase}
 * (read side) because the two share the same aggregate root.
 * </p>
 */
public class TaskOrderApplicationService implements ManageTaskOrderUseCase, QueryTaskOrderUseCase {

    private final TaskOrderRepository taskOrderRepository;
    private final TaskRecordRepository taskRecordRepository;
    private final UnitOfWork unitOfWork;

    public TaskOrderApplicationService(TaskOrderRepository taskOrderRepository,
                                       TaskRecordRepository taskRecordRepository,
                                       UnitOfWork unitOfWork) {
        this.taskOrderRepository = Objects.requireNonNull(taskOrderRepository, "taskOrderRepository must not be null");
        this.taskRecordRepository = Objects.requireNonNull(taskRecordRepository, "taskRecordRepository must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
    }

    @Override
    public boolean isOrdered(String orderKey) {
        Objects.requireNonNull(orderKey);
        return taskOrderRepository.findByKey(orderKey).isPresent();
    }

    @Override
    public Long createOrder(TaskOrder order) {
        Objects.requireNonNull(order);
        Objects.requireNonNull(order.getKey());
        String orderKey = order.getKey();
        Optional<TaskOrder> existing = taskOrderRepository.findByKey(orderKey);
        if (existing.isPresent()) {
            throw new OrderKeyAlreadyExistsException(
                "The task order with key: " + orderKey + " already exists");
        }
        Long id = taskOrderRepository.create(order);
        order.setId(id);
        return id;
    }

    @Override
    public int updateOrderByKey(String orderKey, TaskOrder taskOrder) {
        Objects.requireNonNull(orderKey);
        Objects.requireNonNull(taskOrder);
        Optional<TaskOrder> existing = taskOrderRepository.findByKey(orderKey);
        if (existing.isEmpty()) {
            throw new DagNotFoundException(
                "The task order with key: " + orderKey + " not exists");
        }
        return taskOrderRepository.updateByKey(orderKey, taskOrder);
    }

    @Override
    public int deleteOrderByKey(String orderKey) {
        Objects.requireNonNull(orderKey);
        unitOfWork.execute(() -> {
            // Check for any running tasks across the three "active" statuses.
            // TaskRecordSearchCriteria only supports single-status queries,
            // so we check each status separately. This is acceptable in a
            // transactional context; step 3 may introduce a richer query API.
            for (TaskStatus activeStatus : List.of(READY, HOLD, RUNNING)) {
                int count = taskRecordRepository.count(TaskRecordSearchCriteria.builder()
                    .withOrderKey(orderKey)
                    .withStatus(activeStatus)
                    .build());
                if (count > 0) {
                    throw new IllegalArgumentException("The task order with key: " + orderKey + " is already running");
                }
            }
            taskOrderRepository.deleteByKey(orderKey);
            taskRecordRepository.deleteByOrderKey(orderKey);
        });
        return 1;
    }
}
