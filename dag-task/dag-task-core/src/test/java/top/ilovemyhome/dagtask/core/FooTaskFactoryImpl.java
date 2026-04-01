package top.ilovemyhome.dagtask.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskContext;
import top.ilovemyhome.dagtask.si.TaskFactory;

public class FooTaskFactoryImpl implements TaskFactory {

    public FooTaskFactoryImpl(TaskContext<String, String> taskContext) {
        taskContext.setTaskFactory(this);
    }

    private static final Logger logger = LoggerFactory.getLogger(FooTaskFactoryImpl.class);
}
