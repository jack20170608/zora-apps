package top.ilovemyhome.dagtask.core.task;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.service.TaskDagService;
import top.ilovemyhome.dagtask.core.dag.DagHelper;
import top.ilovemyhome.dagtask.core.dag.DagNode;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link TaskDagService} focusing on DAG task record management.
 * Runtime scheduling is handled by {@link top.ilovemyhome.dagtask.core.service.DagScheduleServiceImpl}.
 */
public class TaskDagServiceImpl implements TaskDagService {

    private final Jdbi jdbi;
    private final TaskRecordDao taskRecordDao;
    private final TaskOrderDao taskOrderDao;

    private static final Logger logger = LoggerFactory.getLogger(TaskDagServiceImpl.class);

    public TaskDagServiceImpl(Jdbi jdbi, TaskOrderDao taskOrderDao, TaskRecordDao taskRecordDao) {
        this.jdbi = Objects.requireNonNull(jdbi, "jdbi must not be null");
        this.taskOrderDao = Objects.requireNonNull(taskOrderDao, "taskOrderDao must not be null");
        this.taskRecordDao = Objects.requireNonNull(taskRecordDao, "taskRecordDao must not be null");
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
        return ImmutableList.copyOf(ids);
    }

    @Override
    public List<TaskRecord> findByOrderKey(String orderKey) {
        Objects.requireNonNull(orderKey);
        TaskRecordSearchCriteria criteria = TaskRecordSearchCriteria.builder()
            .withOrderKey(orderKey)
            .build();
        return taskRecordDao.find(criteria);
    }

    @Override
    public List<TaskRecord> findByStatus(top.ilovemyhome.dagtask.si.enums.TaskStatus status) {
        Objects.requireNonNull(status);
        TaskRecordSearchCriteria criteria = TaskRecordSearchCriteria.builder()
            .withStatus(status)
            .build();
        return taskRecordDao.find(criteria);
    }

    @Override
    public List<Long> createTasks(List<TaskRecord> records) {
        Objects.requireNonNull(records);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("The records list must not be empty");
        }
        String orderKey = records.getFirst().getOrderKey();
        if (StringUtils.isBlank(orderKey)) {
            throw new IllegalArgumentException("The record order key must not be empty");
        }
        records.forEach(r -> {
            if (!Strings.CS.equals(orderKey, r.getOrderKey())) {
                throw new IllegalArgumentException("All records must have the same order key");
            }
        });
        return jdbi.inTransaction(h -> {
            Optional<TaskOrder> taskOrderOptional = taskOrderDao.findByKey(orderKey);
            final List<Long> rs = new ArrayList<>();
            if (taskOrderOptional.isPresent()) {
                Map<Long, TaskRecord> rMap = findTaskByOrderKey(orderKey).stream()
                    .collect(Collectors.toMap(TaskRecord::getId, Function.identity()));
                records.forEach(n -> {
                    if (rMap.containsKey(n.getId())) {
                        throw new IllegalArgumentException("Already existing task record with id: " + n.getId());
                    }
                });

                List<DagNode> dagNodes;
                if (!rMap.isEmpty()) {
                    dagNodes = rMap.values().stream().map(this::toDagNode).collect(Collectors.toList());
                } else {
                    dagNodes = new ArrayList<>();
                }
                List<String> taskPath = new ArrayList<>();
                logger.info("================================================");
                DagHelper.visitDAG(dagNodes, taskPath);
                logger.info("{}", taskPath);
                records.forEach(newRecord -> dagNodes.add(toDagNode(newRecord)));
                logger.info("================================================");
                taskPath.clear();
                DagHelper.visitDAG(dagNodes, taskPath);
                logger.info("{}", taskPath);
                records.forEach(newRecord -> rs.add(taskRecordDao.create(newRecord)));
            } else {
                records.forEach(newRecord -> rs.add(taskRecordDao.create(newRecord)));
                logger.info("Created non-ordered tasks.");
            }
            return rs;
        });
    }

    @Override
    public boolean isSuccess(String orderKey) {
        boolean ordered = taskRecordDao.isOrdered(orderKey);
        if (!ordered) {
            return false;
        }
        return taskRecordDao.isSuccess(orderKey);
    }

    @Override
    public List<TaskRecord> findTaskByOrderKey(String orderKey) {
        TaskRecordSearchCriteria criteria = TaskRecordSearchCriteria.builder()
            .withOrderKey(orderKey)
            .build();
        return taskRecordDao.search(criteria);
    }

    private DagNode toDagNode(TaskRecord record) {
        Objects.requireNonNull(record);
        return new DagNode(record.getId(), record.getName(),
            Objects.nonNull(record.getSuccessorIds()) ? Sets.newHashSet(record.getSuccessorIds()) : null);
    }

}
