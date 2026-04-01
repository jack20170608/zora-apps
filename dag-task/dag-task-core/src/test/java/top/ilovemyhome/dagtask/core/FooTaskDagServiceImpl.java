package top.ilovemyhome.dagtask.core;

import org.jdbi.v3.core.Jdbi;
import top.ilovemyhome.dagtask.si.TaskContext;

public class FooTaskDagServiceImpl extends AbstractTaskDagServiceImpl<String, String> {

    public FooTaskDagServiceImpl(Jdbi jdbi, TaskContext<String, String> taskContext) {
        super(jdbi, taskContext);
    }
}
