package top.ilovemyhome.dagtask.core.helper;


import top.ilovemyhome.dagtask.si.TaskOutput;

public final class TaskHelper {

    public static <O> TaskOutput<O> createErrorOutput(Long taskId, Throwable t) {
        return new TaskOutput<>(taskId, false, t.getMessage(), null);
    }
}
