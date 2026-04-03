package top.ilovemyhome.dagtask.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.core.helper.TaskHelper;
import top.ilovemyhome.dagtask.si.TaskContext;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.Task;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SyncTask extends Task {

    private static final long DEFAULT_TIMEOUT_SECONDS = 60;
    private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    public SyncTask(Long id, TaskContext taskContext, String orderKey, String name
        , TaskInput input, TaskStatus taskStatus, Long timeout, TimeUnit timeoutUnit, TaskExecution taskExecution) {
        super(id, taskContext, orderKey, name, input, taskStatus, timeout < 1L ? DEFAULT_TIMEOUT_SECONDS : timeout
            , Objects.isNull(timeoutUnit) ? DEFAULT_TIMEOUT_UNIT : timeoutUnit, taskExecution);
    }

    @Override
    public void run() {
        try {
            logger.info("OrderId=[{}], Id=[{}], name=[{}] is running.", getOrderKey(), getId(), getName());
            start();
            CompletableFuture<TaskOutput> cf = CompletableFuture.supplyAsync(() -> this.getTaskExecution().execute(getInput()));
            TaskOutput out = cf.get(this.getTimeout(), this.getTimeoutUnit());
            success(out);
            logger.info("OrderId=[{}], Id=[{}], name=[{}] run successfully.", getOrderKey(), getId(), getName());
        } catch (ExecutionException e) {
            logger.error("OrderId=[{}], Id=[{}], name=[{}] execution failure.", getOrderKey(), getId(), getName());
            error(TaskHelper.createErrorOutput(getId(), e));
        } catch (InterruptedException i) {
            logger.error("OrderId=[{}], Id=[{}], name=[{}] execution with unknown status.", getOrderKey(), getId(), getName());
            unknown(TaskHelper.createErrorOutput(getId(), i));
        } catch (TimeoutException t) {
            logger.error("OrderId=[{}], Id=[{}], name=[{}] execution timeout.", getOrderKey(), getId(), getName());
            timeout(TaskHelper.createErrorOutput(getId(), t));
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(SyncTask.class);
}
