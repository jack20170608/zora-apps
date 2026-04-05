package top.ilovemyhome.dagtask.core.task;

import org.jdbi.v3.core.Jdbi;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskOrderService;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TaskOrderServiceImpl implements TaskOrderService {

    public TaskOrderServiceImpl(Jdbi jdbi, TaskRecordDao taskRecordDao, TaskOrderDao taskOrderDao) {
        this.jdbi = jdbi;
        this.taskRecordDao = taskRecordDao;
        this.taskOrderDao = taskOrderDao;
    }

    @Override
    public boolean isOrdered(String orderKey) {
        return taskOrderDao.findByKey(orderKey).isPresent();
    }


    @Override
    public Long createOrder(TaskOrder taskOrder) {
        Objects.requireNonNull(taskOrder);
        Objects.requireNonNull(taskOrder.getKey());
        String orderKey = taskOrder.getKey();
        Optional<TaskOrder> taskOrderOptional = taskOrderDao.findByKey(orderKey);
        Long id;
        if (taskOrderOptional.isEmpty()) {
            id = taskOrderDao.create(taskOrder);
            taskOrder.setId(id);
        } else {
            throw new IllegalArgumentException("The task order with key: " + orderKey + " already exists");
        }
        return id;
    }

    @Override
    public int updateOrderByKey(String orderKey, TaskOrder taskOrder) {
        Objects.requireNonNull(orderKey);
        Objects.requireNonNull(taskOrder);
        Optional<TaskOrder> taskOrderOptional = taskOrderDao.findByKey(orderKey);
        int result;
        if (taskOrderOptional.isPresent()) {
            result = taskOrderDao.updateByKey(orderKey, taskOrder);
        } else {
            throw new IllegalArgumentException("The task order with key: " + orderKey + " not exists");
        }
        return result;
    }

    @Override
    public int deleteOrderByKey(String orderKey) {
        Objects.requireNonNull(orderKey);
        jdbi.useTransaction(h -> {
            taskRecordDao.count(TaskSearchCriteria.builder()
                    .withOrderKey(orderKey)
                    .withStatusList(List.of())
                .build());
            taskOrderDao.deleteByKey(orderKey);
            taskRecordDao.deleteByOrderKey(orderKey);
        });
        return 1;
    }


    private final Jdbi jdbi;
    private final TaskRecordDao taskRecordDao;
    private final TaskOrderDao taskOrderDao;
}
