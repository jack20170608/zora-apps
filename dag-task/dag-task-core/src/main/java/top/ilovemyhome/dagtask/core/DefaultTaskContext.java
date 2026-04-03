package top.ilovemyhome.dagtask.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import top.ilovemyhome.dagtask.si.*;
import top.ilovemyhome.zora.common.date.LocalDateUtils;
import top.ilovemyhome.zora.json.jackson.JacksonUtil;

import java.sql.Types;
import java.time.YearMonth;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

public class DefaultTaskContext implements TaskContext {

    private final Jdbi jdbi;

    private final String taskOrderTableName;
    private final String taskTableName;

    private final ExecutorService threadPool;

    private TaskFactory taskFactory;
    private TaskRecordDao taskRecordDao;
    private TaskOrderDao taskOrderDao;
    private TaskDagService taskDagService;

    @Override
    public void setTaskFactory(TaskFactory taskFactory) {
        this.taskFactory = taskFactory;
    }

    @Override
    public TaskFactory getTaskFactory() {
        return taskFactory;
    }

    @Override
    public TaskRecordDao getTaskRecordDao() {
        return taskRecordDao;
    }

    @Override
    public String getTaskOrderTableName() {
        return taskOrderTableName;
    }

    @Override
    public String getTaskTableName() {
        return taskTableName;
    }

    @Override
    public void setTaskRecordDao(TaskRecordDao taskRecordDao) {
        this.taskRecordDao = taskRecordDao;
    }

    @Override
    public ExecutorService getThreadPool() {
        return threadPool;
    }


    @Override
    public TaskDagService getTaskDagService() {
        return taskDagService;
    }

    @Override
    public void setTaskDagService(TaskDagService taskDagService) {
        this.taskDagService = taskDagService;
    }

    @Override
    public TaskOrderDao getTaskOrderDao() {
        return taskOrderDao;
    }

    public void setTaskOrderDao(TaskOrderDao taskOrderDao) {
        this.taskOrderDao = taskOrderDao;
    }

    public Jdbi getJdbi() {
        return jdbi;
    }

    protected DefaultTaskContext(Jdbi jdbi) {
        this(jdbi, null, TaskContext.DEFAULT_TASK_ORDER_TABLE_NAME, TaskContext.DEFAULT_TASK_TABLE_NAME);
    }

    protected DefaultTaskContext(Jdbi jdbi, ExecutorService threadPool, String taskOrderTableName, String taskTableName) {
        int totalProcessorSize = Runtime.getRuntime().availableProcessors();
        int nThreads = Math.min(totalProcessorSize, 16);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("TaskDagService-%d").build();
        this.threadPool = Objects.requireNonNullElseGet(
            threadPool
            , () -> {
                return new ThreadPoolExecutor(nThreads, 16, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024)
                    , namedThreadFactory, new ThreadPoolExecutor.AbortPolicy())
                    ;
            }
        );
        this.taskOrderTableName = taskOrderTableName;
        this.taskTableName = taskTableName;
        this.jdbi = jdbi;
        //customise some argument mapper
        jdbi.registerArgument(new AbstractArgumentFactory<Map<String, String>>(Types.VARCHAR) {
            @Override
            protected Argument build(Map<String, String> value, ConfigRegistry config) {
                return (position, statement, ctx) -> statement.setString(position, JacksonUtil.toJson(value));
            }
        });
        jdbi.registerArgument(new AbstractArgumentFactory<Set<Long>>(Types.VARCHAR) {
            @Override
            protected Argument build(Set<Long> value, ConfigRegistry config) {
                return (position, statement, ctx) -> statement.setString(position, JacksonUtil.toJson(value));
            }
        });
        jdbi.registerArgument(new AbstractArgumentFactory<YearMonth>(Types.VARCHAR) {
            @Override
            protected Argument build(YearMonth value, ConfigRegistry config) {
                return (position, statement, ctx) -> statement.setString(position, LocalDateUtils.formatYearMonth(value));
            }
        });
        jdbi.registerArgument(new AbstractArgumentFactory<TaskInput>(Types.VARCHAR) {
            @Override
            protected Argument build(TaskInput taskInput, ConfigRegistry config) {
                return (position, statement, ctx) -> statement.setString(position, JacksonUtil.toJson(taskInput));
            }
        });
    }
}
