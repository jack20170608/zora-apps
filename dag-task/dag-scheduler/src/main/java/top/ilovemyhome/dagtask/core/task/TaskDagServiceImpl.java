package top.ilovemyhome.dagtask.core.task;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.server.DagServerConfig;
import top.ilovemyhome.dagtask.si.persistence.AgentRegistryDao;
import top.ilovemyhome.dagtask.si.persistence.TaskOrderDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.persistence.TaskTemplateDao;
import top.ilovemyhome.zora.jdbi.SearchCriteria;
import top.ilovemyhome.dagtask.core.dag.DagHelper;
import top.ilovemyhome.dagtask.core.dag.DagNode;
import top.ilovemyhome.dagtask.si.*;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TaskDagServiceImpl implements TaskDagService {

    public TaskDagServiceImpl(DagServerConfig config
        , Jdbi jdbi
        , TaskOrderDao taskOrderDao
        , TaskRecordDao taskRecordDao
        , AgentRegistryDao agentRegistryDao
        , TaskTemplateDao taskTemplateDao
    ) {
        this.jdbi = jdbi;
        this.taskOrderDao = taskOrderDao;
        this.taskRecordDao = taskRecordDao;
        this.agentRegistryDao = agentRegistryDao;
        this.taskTemplateDao = taskTemplateDao;
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
    public void receiveTaskEvent(Long taskId, TaskStatus newStatus, TaskOutput output) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(newStatus);
        Objects.requireNonNull(output);
        List<TaskRecord> allTasks = loadByTaskId(taskId);
        Map<Long, TaskRecord> taskIdMap = allTasks.stream().collect(Collectors.toMap(TaskRecord::getId, Function.identity()));

        if (!taskIdMap.containsKey(taskId)) {
            throw new IllegalStateException("Data issue, please check!");
        }
        TaskRecord targetTask = taskIdMap.get(taskId);
        /*
        if (!(targetTask instanceof AsyncTask)) {
            throw new IllegalArgumentException("The target task is not an AsyncTask!");
        }
        TaskStatus oldStatus = targetTask.getTaskStatus();
        if (newStatus == TaskStatus.SUCCESS) {
            if (oldStatus == TaskStatus.RUNNING || oldStatus == TaskStatus.UNKNOWN || oldStatus == TaskStatus.ERROR) {
                targetTask.success(output);
            } else {
                logger.warn("Ignore the event, as the task status " + oldStatus + " not in [running, unknown, error] status.]");
            }
        } else if (newStatus == TaskStatus.ERROR || newStatus == TaskStatus.TIMEOUT || newStatus == TaskStatus.UNKNOWN) {
            targetTask.failure(newStatus, output);
        } else if (newStatus == TaskStatus.INIT) {
            if (oldStatus == TaskStatus.ERROR || oldStatus == TaskStatus.UNKNOWN || oldStatus == TaskStatus.TIMEOUT) {
                if (targetTask.isReady()) {
//                    taskContext.getThreadPool().submit(targetTask);
                }
            }
        } else if (newStatus == TaskStatus.SKIPPED) {
            targetTask.skip(output);
        }
         */
    }

    private int countTaskByOrderKey(String orderKey) {
        SearchCriteria searchCriteria = TaskSearchCriteria.builder()
            .withOrderKey(orderKey)
            .build();
        return taskRecordDao.count(searchCriteria);
    }

    @Override
    public List<TaskRecord> findTaskByOrderKey(String orderKey) {
        SearchCriteria searchCriteria = TaskSearchCriteria.builder()
            .withOrderKey(orderKey)
            .build();
        return taskRecordDao.find(searchCriteria);
    }

    private List<TaskRecord> loadByOrderKey(String orderKey) {
        logger.info("Loading task {}.", orderKey);
        List<TaskRecord> taskList = taskRecordDao.loadTaskForOrder(orderKey);
        if (Objects.isNull(taskList) || taskList.isEmpty()) {
            throw new IllegalStateException("Empty task list, please add task for this order!");
        }
        logger.info("Task loaded successfully!");
        return taskList;
    }

    private DagNode toDagNode(TaskRecord record) {
        Objects.requireNonNull(record);
        return new DagNode(record.getId(), record.getName()
            , Objects.nonNull(record.getSuccessorIds()) ? Sets.newHashSet(record.getSuccessorIds()) : null);
    }

    private List<TaskRecord> loadByTaskId(Long taskId) {
        String orderKey = taskRecordDao.getTaskOrderByTaskId(taskId);
        if (Objects.isNull(orderKey)) {
            throw new IllegalStateException("Cannot find the order info for taskId " + taskId);
        }
        return loadByOrderKey(orderKey);
    }

    private final TaskRecordDao taskRecordDao;
    private final TaskOrderDao taskOrderDao;
    private final AgentRegistryDao agentRegistryDao;
    private final TaskTemplateDao taskTemplateDao;
    private final Jdbi jdbi;

    private static final Logger logger = LoggerFactory.getLogger(TaskDagServiceImpl.class);

}
