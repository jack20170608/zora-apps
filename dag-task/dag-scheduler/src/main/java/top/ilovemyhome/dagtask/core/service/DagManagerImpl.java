package top.ilovemyhome.dagtask.core.service;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.dag.DagHelper;
import top.ilovemyhome.dagtask.core.dag.DagNode;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.service.DagManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DagManagerImpl implements DagManager {

    private final TaskRecordDao taskRecordDao;
    private final TaskOrderDao taskOrderDao;
    private final Jdbi jdbi;

    private static final Logger logger = LoggerFactory.getLogger(DagManagerImpl.class);

    public DagManagerImpl(Jdbi jdbi, TaskOrderDao taskOrderDao, TaskRecordDao taskRecordDao) {
        this.jdbi = jdbi;
        this.taskOrderDao = taskOrderDao;
        this.taskRecordDao = taskRecordDao;
    }

    @Override
    public List<Long> getNextTaskIds(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("The count must be greater than 0");
        }
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(taskRecordDao.getNextId());
        }
        return ids;
    }

    @Override
    public List<Long> createTasks(List<TaskRecord> records) {
        Objects.requireNonNull(records);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("The records list must not be empty");
        }

        // Check all tasks have the same order key
        String orderKey = records.getFirst().getOrderKey();
        if (StringUtils.isBlank(orderKey)) {
            throw new IllegalArgumentException("The record order key must not be empty");
        }
        records.forEach(r -> {
            if (!StringUtils.equals(orderKey, r.getOrderKey())) {
                throw new IllegalArgumentException("All records must have the same order key");
            }
        });

        return jdbi.inTransaction(h -> {
            Optional<TaskOrder> taskOrderOptional = taskOrderDao.findByKey(orderKey);
            final List<Long> result = new ArrayList<>();

            if (taskOrderOptional.isPresent()) {
                Map<Long, TaskRecord> existingMap = findTasksByOrderKey(orderKey).stream()
                    .collect(Collectors.toMap(TaskRecord::getId, r -> r));

                // Check for duplicates
                records.forEach(newRecord -> {
                    if (existingMap.containsKey(newRecord.getId())) {
                        throw new IllegalArgumentException("Already existing task record with id: " + newRecord.getId());
                    }
                });

                // Build complete DAG and check for cycles
                List<DagNode> dagNodes = existingMap.values().stream()
                    .map(this::toDagNode)
                    .collect(Collectors.toList());

                List<String> path = new ArrayList<>();
                logger.info("================================================");
                DagHelper.visitDAG(dagNodes, path);
                logger.info("{}", path);

                // Add new tasks and recheck
                records.forEach(newRecord -> {
                    dagNodes.add(toDagNode(newRecord));
                });

                path.clear();
                logger.info("After adding new tasks:");
                DagHelper.visitDAG(dagNodes, path);
                logger.info("{}", path);
                logger.info("================================================");

                // Create all new tasks
                records.forEach(newRecord -> {
                    result.add(taskRecordDao.create(newRecord));
                });
            } else {
                // No existing order, just create all tasks
                records.forEach(newRecord -> {
                    result.add(taskRecordDao.create(newRecord));
                });
                logger.info("Created new DAG for order: {}", orderKey);
            }

            return result;
        });
    }

    @Override
    public List<TaskRecord> findTasksByOrderKey(String orderKey) {
        return taskRecordDao.findByOrderKey(orderKey);
    }

    @Override
    public boolean exists(String orderKey) {
        return taskRecordDao.isOrdered(orderKey);
    }

    @Override
    public boolean isAllSuccess(String orderKey) {
        return taskRecordDao.isSuccess(orderKey);
    }

    @Override
    public Map<TaskStatus, Long> countByStatus(String orderKey) {
        List<TaskRecord> allTasks = findTasksByOrderKey(orderKey);
        Map<TaskStatus, Long> result = new HashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            result.put(status, 0L);
        }
        for (TaskRecord task : allTasks) {
            result.computeIfPresent(task.getStatus(), (k, v) -> v + 1);
        }
        return result;
    }

    private DagNode toDagNode(TaskRecord record) {
        Objects.requireNonNull(record);
        return new DagNode(record.getId(), record.getName(),
            Objects.nonNull(record.getSuccessorIds()) ? Sets.newHashSet(record.getSuccessorIds()) : null);
    }
}
