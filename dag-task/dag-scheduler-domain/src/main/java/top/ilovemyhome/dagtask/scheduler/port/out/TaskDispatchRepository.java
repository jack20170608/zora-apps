package top.ilovemyhome.dagtask.scheduler.port.out;

import top.ilovemyhome.dagtask.si.TaskDispatchRecord;
import top.ilovemyhome.dagtask.si.enums.DispatchStatus;

import java.util.List;

/**
 * Outbound port for {@link TaskDispatchRecord} persistence. Implementations live in
 * dag-scheduler-adapter-persistence-jdbc (or any other persistence adapter).
 *
 * <p>Records where each task was dispatched (which agent), when it was dispatched,
 * and the current dispatch status — required for management ops, auditing,
 * statistics, and debugging.
 */
public interface TaskDispatchRepository {

    /**
     * Finds the dispatch record(s) for a specific task.
     *
     * @param taskId the task ID to find
     * @return list of dispatch records (typically one per task)
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
     * @param taskId    the task ID
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
