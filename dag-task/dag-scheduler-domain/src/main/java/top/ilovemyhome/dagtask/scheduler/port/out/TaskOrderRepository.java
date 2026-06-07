package top.ilovemyhome.dagtask.scheduler.port.out;

import top.ilovemyhome.dagtask.si.TaskOrder;

import java.util.Optional;

/**
 * Outbound port for {@link TaskOrder} persistence. Implementations live in
 * dag-scheduler-adapter-persistence-jdbc (or any other persistence adapter).
 */
public interface TaskOrderRepository {

    Optional<TaskOrder> findByKey(String key);

    int updateByKey(String key, TaskOrder task);

    int deleteByKey(String key);
}
