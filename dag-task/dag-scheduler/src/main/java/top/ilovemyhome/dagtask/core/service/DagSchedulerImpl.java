package top.ilovemyhome.dagtask.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.dispatcher.TaskDispatcher;
import top.ilovemyhome.dagtask.core.dispatcher.TaskDispatcher.DispatchResult;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.service.DagScheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class DagSchedulerImpl implements DagScheduler {

    private final TaskRecordDao taskRecordDao;
    private final TaskDispatcher taskDispatcher;

    private static final Logger logger = LoggerFactory.getLogger(DagSchedulerImpl.class);

    public DagSchedulerImpl(TaskRecordDao taskRecordDao, TaskDispatcher taskDispatcher) {
        this.taskRecordDao = taskRecordDao;
        this.taskDispatcher = taskDispatcher;
    }

    @Override
    public void start(String orderKey) {
        Objects.requireNonNull(orderKey);
        logger.info("Starting DAG execution for order key: {}", orderKey);
        int triggered = triggerReadyTasks(orderKey);
        logger.info("Triggered {} ready tasks for order {}", triggered, orderKey);
    }

    @Override
    public List<TaskRecord> findReadyTasks(String orderKey) {
        return taskRecordDao.findReadyTasksForOrder(orderKey);
    }

    @Override
    public int triggerReadyTasks(String orderKey) {
        List<TaskRecord> readyTasks = findReadyTasks(orderKey);
        if (readyTasks.isEmpty()) {
            logger.debug("No ready tasks found for order {}", orderKey);
            return 0;
        }

        int triggeredCount = 0;
        for (TaskRecord task : readyTasks) {
            // Skip if task is not in INIT status
            if (task.getStatus() != TaskStatus.INIT) {
                continue;
            }

            DispatchResult result = taskDispatcher.dispatch(task);
            if (result.isSuccess()) {
                triggeredCount++;
                // Update task status to DISPATCHED
                taskRecordDao.updateStatus(task.getId(), TaskStatus.DISPATCHED);
                logger.info("Task {} dispatched successfully to agent {}",
                    task.getId(), result.selectedAgent().getAgentId());
            } else {
                logger.warn("Failed to dispatch task {}: {}", task.getId(), result.message());
            }
        }

        return triggeredCount;
    }

    @Override
    public void onTaskCompleted(Long taskId, TaskStatus newStatus, TaskOutput output) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(newStatus);

        // Get the task
        var taskOpt = taskRecordDao.loadTaskById(taskId);
        if (taskOpt.isEmpty()) {
            logger.warn("Task {} not found when completing", taskId);
            return;
        }

        TaskRecord task = taskOpt.get();
        String orderKey = task.getOrderKey();

        // Update task status and output
        taskRecordDao.stop(taskId, newStatus, output, LocalDateTime.now());
        logger.info("Task {} completed with status: {}", taskId, newStatus);

        // If task is successful, trigger any ready successors
        if (newStatus == TaskStatus.SUCCESS) {
            List<TaskRecord> readySuccessors = taskRecordDao.findReadySuccessors(taskId);
            int triggered = 0;
            for (TaskRecord successor : readySuccessors) {
                if (successor.getStatus() == TaskStatus.INIT && taskRecordDao.isReady(successor.getId())) {
                    DispatchResult result = taskDispatcher.dispatch(successor);
                    if (result.success()) {
                        taskRecordDao.updateStatus(successor.getId(), TaskStatus.DISPATCHED);
                        triggered++;
                        logger.info("Successor task {} dispatched after completion of task {}",
                            successor.getId(), taskId);
                    }
                }
            }
            if (triggered > 0) {
                logger.info("Triggered {} successor tasks after task {} completed",
                    triggered, taskId);
            }
        }

        // Check if entire DAG is complete
        if (isAllSuccess(orderKey)) {
            logger.info("All tasks in order {} completed successfully", orderKey);
        }
    }

    private boolean isAllSuccess(String orderKey) {
        return taskRecordDao.isSuccess(orderKey);
    }
}
