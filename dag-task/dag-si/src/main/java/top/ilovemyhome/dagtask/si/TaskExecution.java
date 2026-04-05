package top.ilovemyhome.dagtask.si;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FunctionalInterface
public interface TaskExecution {

    Logger logger = LoggerFactory.getLogger(TaskExecution.class);

    TaskOutput execute(TaskInput input);
}
