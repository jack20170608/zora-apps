package top.ilovemyhome.dagtask.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.zora.jdbi.SearchCriteria;
import top.ilovemyhome.dagtask.core.dag.DagHelper;
import top.ilovemyhome.dagtask.core.dag.DagNode;
import top.ilovemyhome.dagtask.si.*;
import top.ilovemyhome.dagtask.si.Task;
import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractTaskDagServiceImpl<I,O> implements TaskDagService<I,O> {

    public AbstractTaskDagServiceImpl(Jdbi jdbi, TaskContext<I,O> taskContext) {
        this.taskContext = taskContext;
        this.taskOrderDao = taskContext.getTaskOrderDao();
        this.taskRecordDao = taskContext.getTaskRecordDao();
        this.jdbi = jdbi;
        taskContext.setTaskDagService(this);
    }

    @Override
    public boolean isOrdered(String orderKey) {
        return taskOrderDao.findByKey(orderKey).isPresent();
    }

    @Override
    public synchronized Long createOrder(TaskOrder taskOrder) {
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
    public int deleteOrderByKey(String orderKey, boolean caseCade) {
        Objects.requireNonNull(orderKey);
        final AtomicInteger result = new AtomicInteger(0);
        int count = countTaskByOrderKey(orderKey);
        if (count > 0) {
            if (caseCade) {
                jdbi.useTransaction(h -> {
                    // Delete in DB
                    taskRecordDao.deleteByOrderKey(orderKey);
                    result.set(taskOrderDao.deleteByKey(orderKey));
                });
            } else {
                throw new IllegalArgumentException("The task order with key: " + orderKey + " have tasks linked to it.");
            }
        } else {
            result.set(taskOrderDao.deleteByKey(orderKey));
        }
        return result.get();
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
    public TaskOutput<O> runTask(Long taskId, TaskInput<I> input) {
        return null;
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
        List<Task<I,O>> readyTasks = taskRecordDao.findReadyTasksForOrder(orderKey);
        readyTasks.forEach(t -> {
            logger.info("Submit the task {}.", t);
            taskContext.getThreadPool().submit(t);
        });
    }

    @Override
    public void receiveTaskEvent(Long taskId, TaskStatus newStatus, TaskOutput<O> output) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(newStatus);
        Objects.requireNonNull(output);
        List<Task<I,O>> allTasks = loadByTaskId(taskId);
        Map<Long, Task<I,O>> taskIdMap = allTasks.stream().collect(Collectors.toMap(Task::getId, Function.identity()));

        if (!taskIdMap.containsKey(taskId)) {
            throw new IllegalStateException("Data issue, please check!");
        }
        Task<I,O> targetTask = taskIdMap.get(taskId);
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
                    taskContext.getThreadPool().submit(targetTask);
                }
            }
        } else if (newStatus == TaskStatus.SKIPPED) {
            targetTask.skip(output);
        }
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

    private List<Task<I, O>> loadByOrderKey(String orderKey) {
        logger.info("Loading task {}.", orderKey);
        List<Task<I,O>> taskList = taskRecordDao.loadTaskForOrder(orderKey);
        if (Objects.isNull(taskList) || taskList.isEmpty()) {
            throw new IllegalStateException("Empty task list, please add task for this order!");
        }
        if (taskList.isEmpty()) {
            throw new IllegalStateException("Empty task list!!");
        }
        logger.info("Task loaded successfully!");
        return taskList;
    }

    private DagNode toDagNode(TaskRecord record) {
        Objects.requireNonNull(record);
        return new DagNode(record.getId(), record.getName()
            , Objects.nonNull(record.getSuccessorIds()) ? Sets.newHashSet(record.getSuccessorIds()) : null);
    }

    private List<Task<I,O>> loadByTaskId(Long taskId) {
        String orderKey = taskContext.getTaskRecordDao().getTaskOrderByTaskId(taskId);
        if (Objects.isNull(orderKey)) {
            throw new IllegalStateException("Cannot find the order info for taskId " + taskId);
        }
        return loadByOrderKey(orderKey);
    }

    private final TaskContext<I,O> taskContext;

    private final TaskRecordDao taskRecordDao;
    private final TaskOrderDao taskOrderDao;
    private final Jdbi jdbi;

    private static final Logger logger = LoggerFactory.getLogger(AbstractTaskDagServiceImpl.class);

}
