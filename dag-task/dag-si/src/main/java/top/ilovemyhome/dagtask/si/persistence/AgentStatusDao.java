package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;

import java.util.Optional;

public interface AgentStatusDao extends BaseDao<AgentStatus> {

    Optional<AgentStatus> findByAgentId(String agentId);

    int updateStatus(String agentId, boolean running, int pendingTasks, int runningTasks, int finishedTasks);

    int markUnregistered(String agentId);

    boolean exists(String agentId);
}
