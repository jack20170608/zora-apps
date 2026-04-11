package top.ilovemyhome.dagtask.si.persistence;

import top.ilovemyhome.dagtask.si.enums.DispatchStatus;
import top.ilovemyhome.zora.jdbi.dao.BaseDao;
import top.ilovemyhome.dagtask.si.TaskDispatchRecord;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for task dispatch tracking records.
 * <p>
 * This DAO manages the persistent storage of task dispatch information,
 * which records where each task was dispatched (which agent), when it was
 * dispatched, and the current dispatch status. This information is required
 * for:
 * <ul>
 *     <li>Management operations: {@code forceOk}, {@code kill}, {@code hold}</li>
 *     <li>Auditing: track which agent executed which tasks</li>
 *     <li>Statistics: analyze load distribution across agents</li>
 *     <li>Debugging: troubleshoot dispatch issues</li>
 * </ul>
 * </p>
 *
 * @see top.ilovemyhome.dagtask.si.TaskDispatchRecord
 */
public interface TaskDispatchDao extends BaseDao<TaskDispatchRecord> {

    /**
     * Finds the dispatch record for a specific task.
     * Each task should have at most one dispatch record.
     *
     * @param taskId the task ID to find
     * @return the dispatch record if found, empty otherwise
     */
    List<TaskDispatchRecord> findByTaskId(Long taskId);

    /**
     * Finds all dispatch records for a specific agent.
     *
     * @param agentId the agent ID
     * @return list of all dispatch records for this agent, ordered by dispatch time descending
     */
    List<TaskDispatchRecord> findByAgentId(String agentId);

    /**
     * Finds all dispatch records with a specific dispatch status.
     *
     * @param status the status to filter by
     * @return list of matching dispatch records
     */
    List<TaskDispatchRecord> findByStatus(DispatchStatus status);

    /**
     * Updates the status of a dispatch record.
     *
     * @param taskId the task ID
     * @param newStatus the new status
     * @return number of rows updated (0 or 1)
     */
    int updateStatus(Long taskId, DispatchStatus newStatus);

    /**
     * Counts the number of dispatches for a specific agent.
     *
     * @param agentId the agent ID
     * @return total number of dispatches for this agent
     */
    int countByAgentId(String agentId);

    /**
     * Counts the number of dispatches with a specific status.
     *
     * @param status the status to count
     * @return number of dispatches with this status
     */
    int countByStatus(DispatchStatus status);

    /**
     * Deletes all dispatch records for an agent.
     * Typically used after an agent is unregistered.
     *
     * @param agentId the agent ID
     * @return number of records deleted
     */
    int deleteByAgentId(String agentId);
}
