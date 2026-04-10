package top.ilovemyhome.dagtask.core.task;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.dispatcher.TaskDispatcher;
import top.ilovemyhome.dagtask.core.server.DagServerConfig;
import top.ilovemyhome.dagtask.si.dto.TaskRecordSearchCriteria;
import top.ilovemyhome.dagtask.si.persistence.AgentRegistryDao;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.persistence.TaskTemplateDao;
import top.ilovemyhome.dagtask.si.service.TaskDagService;
import top.ilovemyhome.zora.jdbi.SearchCriteria;
import top.ilovemyhome.dagtask.core.dag.DagHelper;
import top.ilovemyhome.dagtask.core.dag.DagNode;
import top.ilovemyhome.dagtask.si.*;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TaskDagServiceImpl implements TaskDagService {

    public TaskDagServiceImpl(DagServerConfig config
        , Jdbi jdbi
        , TaskOrderDao taskOrderDao
        , TaskRecordDao taskRecordDao
        , AgentRegistryDao agentRegistryDao
        , TaskTemplateDao taskTemplateDao
        , TaskDispatcher taskDispatcher) {
        this.jdbi = jdbi;
        this.taskOrderDao = taskOrderDao;
        this.taskRecordDao = taskRecordDao;
        this.agentRegistryDao = agentRegistryDao;
        this.taskTemplateDao = taskTemplateDao;
        this.taskDispatcher = taskDispatcher;
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
    public List<TaskRecord> findByStatus(TaskStatus status) {
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
        // check if the task order exists or not
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
            final List<Long> rs = new ArrayList<>();
            if (taskOrderOptional.isPresent()) {
                Map<Long, TaskRecord> rMap = findTaskByOrderKey(orderKey).stream().collect(Collectors.toMap(TaskRecord::getId, Function.identity()));
                records.forEach(n -> {
                    if (rMap.containsKey(n.getId())) {
                        throw new IllegalArgumentException("Already existing task record with id: " + n.getId());
                    }
                });

                List<DagNode> dagNodes;
                if (!rMap.isEmpty()) {
                    // check if the DAG have cycle
                    dagNodes = rMap.values().stream().map(this::toDagNode).collect(Collectors.toList());
                } else {
                    dagNodes = new ArrayList<>();
                }
                List<String> taskPath = new ArrayList<>();
                logger.info("================================================");
                DagHelper.visitDAG(dagNodes, taskPath);
                logger.info("{}", taskPath);
                // Add more
                records.forEach(newRecord -> {
                    dagNodes.add(toDagNode(newRecord));
                });
                logger.info("================================================");
                taskPath.clear();
                DagHelper.visitDAG(dagNodes, taskPath);
                logger.info("{}", taskPath);
                // Create
                records.forEach(newRecord -> {
                    rs.add(taskRecordDao.create(newRecord));
                });
            } else {
                records.forEach(newRecord -> {
                    rs.add(taskRecordDao.create(newRecord));
                });
                logger.info("Created non-ordered tasks.");
            }
            return rs;
        });
    }

    @Override
    public TaskOutput runNow(Long taskId, TaskInput input) {
        return null;
    }

    @Override
    public void forceOk(Long taskId, TaskOutput output) {

    }

    @Override
    public void kill(Long taskId) {

    }

    @Override
    public void hold(Long taskId) {

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
    public void start(String orderKey) {
        Objects.requireNonNull(orderKey);
        logger.info("Start task for order key: {}.", orderKey);
        List<TaskRecord> readyTasks = taskRecordDao.findReadyTasksForOrder(orderKey);
        readyTasks.forEach(t -> {
            logger.info("Submit the task {}.", t);
//            taskContext.getThreadPool().submit(t);
        });
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
        return new DagNode(record.getId(), record.getName()
            , Objects.nonNull(record.getSuccessorIds()) ? Sets.newHashSet(record.getSuccessorIds()) : null);
    }

    private final TaskRecordDao taskRecordDao;
    private final TaskOrderDao taskOrderDao;
    private final AgentRegistryDao agentRegistryDao;
    private final TaskTemplateDao taskTemplateDao;
    private final TaskDispatcher taskDispatcher;
    private final Jdbi jdbi;

    private static final Logger logger = LoggerFactory.getLogger(TaskDagServiceImpl.class);

}
