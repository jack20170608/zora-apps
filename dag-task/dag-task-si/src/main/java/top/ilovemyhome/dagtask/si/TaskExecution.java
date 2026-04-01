package top.ilovemyhome.dagtask.si;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FunctionalInterface
public interface TaskExecution<I, O> {

    Logger logger = LoggerFactory.getLogger(TaskExecution.class);

    TaskOutput<O> execute(TaskInput<I> input);
}
