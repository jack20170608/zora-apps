package top.ilovemyhome.dagtask.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskContext;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.Task;
import top.ilovemyhome.dagtask.si.enums.TaskStatus;

import java.util.concurrent.TimeUnit;

import static top.ilovemyhome.dagtask.core.helper.TaskHelper.createErrorOutput;


public class AsyncTask<I, O> extends Task<I, O> {

    public AsyncTask(Long id, TaskContext<I,O> taskContext, String orderKey, String name, TaskInput<I> input
        , TaskStatus taskStatus,  Long timeout, TimeUnit timeoutUnit, TaskExecution<I, O> taskExecution) {
        super(id, taskContext, orderKey, name, input, taskStatus, timeout, timeoutUnit, taskExecution);
    }


    @Override
    public void run() {
        try {
            start();
            TaskInput<I> input = getInput();
            TaskOutput<O> out = this.getTaskExecution().execute(input);
            if (out != null && out.isSuccess()) {
                logger.info("OrderId=[{}], Id=[{}], name=[{}] triggered successfully.", getOrderKey(), getId(), getName());
            } else {
                logger.info("OrderId=[{}], Id=[{}], name=[{}] triggered failure.", getOrderKey(), getId(), getName());
                failure(TaskStatus.ERROR, out);
            }
        } catch (Throwable t) {
            logger.error("Run task failed.", t);
            error(createErrorOutput(getId(), t));
        }
    }



    private static final Logger logger = LoggerFactory.getLogger(AsyncTask.class);
}
