package top.ilovemyhome.dagtask.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.dispatcher.TaskDispatcher;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.service.DagScheduler;
import top.ilovemyhome.dagtask.si.service.TaskManager;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class TaskManagerImpl implements TaskManager {

    private final TaskRecordDao taskRecordDao;
    private final DagScheduler dagScheduler;
    private final TaskDispatcher taskDispatcher;

    private static final Logger logger = LoggerFactory.getLogger(TaskManagerImpl.class);

    public TaskManagerImpl(TaskRecordDao taskRecordDao, DagScheduler dagScheduler, TaskDispatcher taskDispatcher) {
        this.taskRecordDao = taskRecordDao;
        this.dagScheduler = dagScheduler;
        this.taskDispatcher = taskDispatcher;
    }

    @Override
    public Optional<TaskRecord> getTask(Long taskId) {
        return taskRecordDao.loadTaskById(taskId);
    }

    @Override
    public TaskOutput runNow(Long taskId, TaskInput input) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(input);

        Optional<TaskRecord> taskOpt = getTask(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        TaskRecord task = taskOpt.get();
        logger.info("Running task {} manually", taskId);

        // Dispatch the task for execution
        var result = taskDispatcher.dispatch(task);
        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to dispatch task: " + result.message());
        }

        // Update status to RUNNING
        taskRecordDao.start(taskId, input, LocalDateTime.now());

        // Note: This returns immediately, actual execution happens on agent
        // For sync execution, we would need a different approach
        return TaskOutput.empty();
    }

    @Override
    public void forceOk(Long taskId, TaskOutput output) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(output);

        logger.info("Force marking task {} as successful", taskId);
        dagScheduler.onTaskCompleted(taskId, TaskStatus.SUCCESS, output);
    }

    @Override
    public void kill(Long taskId) {
        Objects.requireNonNull(taskId);

        logger.info("Force killing (marking as failed) task {}", taskId);
        TaskOutput output = TaskOutput.builder()
            .withSuccess(false)
            .withMessage("Task was manually killed by operator")
            .build();
        dagScheduler.onTaskCompleted(taskId, TaskStatus.FAILED, output);
    }

    @Override
    public void hold(Long taskId) {
        Objects.requireNonNull(taskId);

        Optional<TaskRecord> taskOpt = getTask(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        logger.info("Putting task {} on hold", taskId);
        taskRecordDao.updateStatus(taskId, TaskStatus.HOLD);
    }

    @Override
    public void resume(Long taskId) {
        Objects.requireNonNull(taskId);

        Optional<TaskRecord> taskOpt = getTask(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        TaskRecord task = taskOpt.get();
        logger.info("Resuming held task {}", taskId);

        // Change status back to INIT
        taskRecordDao.updateStatus(taskId, TaskStatus.INIT);

        // Check if task is ready now and trigger it
        if (taskRecordDao.isReady(taskId)) {
            dagScheduler.triggerReadyTasks(task.getOrderKey());
        }
    }
}
