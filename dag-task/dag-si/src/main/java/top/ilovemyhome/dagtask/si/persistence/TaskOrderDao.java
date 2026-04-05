package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.TaskOrder;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.util.Optional;

public interface TaskOrderDao extends BaseDao<TaskOrder> {

    Optional<TaskOrder> findByKey(String key);

    int updateByKey(String key, TaskOrder task);

    int deleteByKey(String key);

}
