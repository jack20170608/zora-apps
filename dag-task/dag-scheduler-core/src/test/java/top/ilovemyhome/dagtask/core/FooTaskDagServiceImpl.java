package top.ilovemyhome.dagtask.core;

import org.jdbi.v3.core.Jdbi;
import top.ilovemyhome.dagtask.si.TaskContext;

public class FooTaskDagServiceImpl extends TaskDagServiceImpl {

    public FooTaskDagServiceImpl(Jdbi jdbi, TaskContext taskContext) {
        super(jdbi, taskContext);
    }
}
