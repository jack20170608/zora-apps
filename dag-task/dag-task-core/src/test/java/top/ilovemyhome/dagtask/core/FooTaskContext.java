package top.ilovemyhome.dagtask.core;

import org.jdbi.v3.core.Jdbi;

public class FooTaskContext extends DefaultTaskContext {

    private FooTaskContext(Jdbi jdbi) {
        super(jdbi);
    }
    private static FooTaskContext INSTANCE ;

    public synchronized static FooTaskContext getInstance(Jdbi jdbi){
        if (INSTANCE == null) {
            INSTANCE = new FooTaskContext(jdbi);
        }
        return INSTANCE;
    }
}
