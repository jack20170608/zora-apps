package top.ilovemyhome.dagtask.si;

import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.util.Optional;

public interface TaskOrderDao extends BaseDao<TaskOrder> {

    Optional<TaskOrder> findByKey(String key);

    int updateByKey(String key, TaskOrder task);

    int deleteByKey(String key);

}
