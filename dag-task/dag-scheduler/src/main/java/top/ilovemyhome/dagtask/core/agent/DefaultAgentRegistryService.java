package top.ilovemyhome.dagtask.core.agent;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.Agent;
import top.ilovemyhome.dagtask.si.agent.AgentRegisterRequest;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.dagtask.si.agent.AgentStatusReport;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;
import top.ilovemyhome.dagtask.si.persistence.AgentDao;
import top.ilovemyhome.dagtask.si.persistence.AgentStatusDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link AgentRegistryService} with database persistence.
 * Maintains an in-memory cache of agent status information for fast access and persists
 * all changes to the database using {@link AgentDao} and {@link AgentStatusDao}.
 *
 * This implementation uses {@link ConcurrentHashMap} for thread-safe cache access
 * and delegates persistence to the DAO layer, allowing the registry to survive
 * server restarts.
 */
public class DefaultAgentRegistryService implements AgentRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAgentRegistryService.class);

    private final Jdbi jdbi;
    private final TaskRecordDao taskRecordDao;
    private final AgentDao agentDao;
    private final AgentStatusDao agentStatusDao;
    private final ConcurrentHashMap<String, AgentStatus> agentCache = new ConcurrentHashMap<>();

    /**
     * Creates a DefaultAgentRegistryService with the required dependencies.
     *
     * @param taskRecordDao DAO for updating task records when results are reported
     * @param agentDao DAO for persisting agent identity information
     * @param agentStatusDao DAO for persisting agent runtime status
     */
    public DefaultAgentRegistryService(Jdbi jdbi, TaskRecordDao taskRecordDao,
                                        AgentDao agentDao, AgentStatusDao agentStatusDao) {
        this.jdbi = jdbi;
        this.taskRecordDao = Objects.requireNonNull(taskRecordDao, "taskRecordDao must not be null");
        this.agentDao = Objects.requireNonNull(agentDao, "agentDao must not be null");
        this.agentStatusDao = Objects.requireNonNull(agentStatusDao, "agentStatusDao must not be null");
        // Warm the cache from database
        loadAllFromDatabase();
    }

    /**
     * Loads all agents from database into the in-memory cache on startup.
     */
    private void loadAllFromDatabase() {
        List<AgentStatus> allAgents = agentStatusDao.findAll();
        allAgents.forEach(agent -> agentCache.put(agent.getAgentId(), agent));
        logger.info("Loaded {} agents from database into registry cache", allAgents.size());
    }

    @Override
    public boolean registerAgent(AgentRegisterRequest registration) {
        if (registration == null || StringUtils.isBlank(registration.agentId())) {
            logger.warn("Cannot register agent: invalid registration (agentId is blank)");
            return false;
        }

        Agent agent = AgentRegisterRequest.toAgent(registration);
        agent.setStatus(Agent.Status.ACTIVE);
        AgentStatus status = AgentRegisterRequest.toAgentStatus(registration);

        jdbi.useTransaction(h -> {
            if (agentDao.exists(registration.agentId())) {
                // Reactivate existing agent
                agentDao.updateStatus(registration.agentId(), Agent.Status.ACTIVE);
                if (agentStatusDao.exists(registration.agentId())) {
                    agentStatusDao.updateStatus(registration.agentId(), true, 0, 0, 0);
                } else {
                    agentStatusDao.create(status);
                }
                logger.info("Agent [{}] already exists in database, reactivating", registration.agentId());
            } else {
                // Insert new agent and status
                agentDao.create(agent);
                agentStatusDao.create(status);
                logger.info("Agent [{}] registered successfully, URL: {}, max concurrent: {}",
                    registration.agentId(), registration.agentUrl(), registration.maxConcurrentTasks());
            }
            // Update cache
            agentCache.put(registration.agentId(), status);
        });

        return true;
    }

    @Override
    public boolean unregisterAgent(AgentUnregistration unregistration) {
        if (unregistration == null || StringUtils.isBlank(unregistration.agentId())) {
            logger.warn("Cannot unregister agent: invalid agentId");
            return false;
        }

        if (!agentDao.exists(unregistration.agentId())) {
            logger.warn("Cannot unregister agent [{}]: agent not found in database", unregistration.agentId());
            return false;
        }

        // Mark as inactive in agent table and unregistered in status table
        agentDao.updateStatus(unregistration.agentId(), Agent.Status.INACTIVE);
        agentStatusDao.markUnregistered(unregistration.agentId());

        // Update cache
        AgentStatus existing = agentCache.get(unregistration.agentId());
        if (existing != null) {
            agentCache.put(unregistration.agentId(), existing.withUnregistered());
        }

        logger.info("Agent [{}] unregistered successfully", unregistration.agentId());
        return true;
    }

    @Override
    public boolean reportTaskResult(TaskExecuteResult taskExecuteResult) {
        if (taskExecuteResult == null || StringUtils.isBlank(taskExecuteResult.agentId())) {
            logger.warn("Cannot process task result: invalid report");
            return false;
        }

        Optional<TaskRecord> taskRecordOpt = taskRecordDao.findOne(Long.valueOf(taskExecuteResult.taskId()));
        if (taskRecordOpt.isEmpty()) {
            logger.warn("Task [{}] not found when processing result report from agent [{}]",
                taskExecuteResult.taskId(), taskExecuteResult.agentId());
            return false;
        }

        TaskRecord taskRecord = taskRecordOpt.get();
        TaskOutput output;
        if (taskExecuteResult.success()) {
            output = TaskOutput.success(taskExecuteResult.taskId(), taskExecuteResult.output());
        } else {
            output = TaskOutput.fail(taskExecuteResult.taskId(), taskExecuteResult.output(), "Task execution failed on agent");
        }

        // Task status should be already set to RUNNING by scheduler before dispatching
        // Update the task to final status based on the result from agent
        taskRecordDao.stop(taskExecuteResult.taskId(), taskRecord.getStatus(), output, java.time.LocalDateTime.now());
        logger.debug("Processed task result for task [{}] from agent [{}], success: {}",
            taskExecuteResult.taskId(), taskExecuteResult.agentId(), taskExecuteResult.success());
        return true;
    }

    @Override
    public boolean reportTaskResult(List<TaskExecuteResult> results) {
        if (results == null || results.isEmpty()) {
            logger.warn("Cannot process task results: empty or null list");
            return false;
        }

        boolean allSuccess = true;
        for (TaskExecuteResult result : results) {
            boolean success = reportTaskResult(result);
            if (!success) {
                allSuccess = false;
                logger.warn("Failed to process task result for task [{}] from agent [{}]",
                    result.taskId(), result.agentId());
            }
        }

        logger.debug("Processed {} task results, all success: {}", results.size(), allSuccess);
        return allSuccess;
    }

    @Override
    public boolean reportAgentStatus(AgentStatusReport statusReport) {
        if (statusReport == null || StringUtils.isBlank(statusReport.agentId())) {
            logger.warn("Cannot process agent status: invalid report");
            return false;
        }

        if (!agentStatusDao.exists(statusReport.agentId())) {
            logger.warn("Agent [{}] not found in database when processing status report", statusReport.agentId());
            return false;
        }

        // Update database
        agentStatusDao.updateStatus(
            statusReport.agentId(),
            statusReport.running(),
            statusReport.pendingTasks(),
            statusReport.runningTasks(),
            statusReport.finishedTasks()
        );

        // Update cache
        AgentStatus existing = agentCache.get(statusReport.agentId());
        if (existing != null) {
            agentCache.put(statusReport.agentId(), existing.withUpdatedStatus(statusReport));
        }

        logger.debug("Updated status for agent [{}]: running={}, pending={}, running={}, finished={}",
            statusReport.agentId(), statusReport.running(), statusReport.pendingTasks(),
            statusReport.runningTasks(), statusReport.finishedTasks());
        return true;
    }

    /**
     * Gets information about a registered agent by ID from the cache.
     *
     * @param agentId the agent identifier
     * @return the agent status if found, empty otherwise
     */
    public Optional<AgentStatus> getAgent(String agentId) {
        return Optional.ofNullable(agentCache.get(agentId));
    }

    /**
     * Lists all registered agents that are currently marked as running from the cache.
     *
     * @return an unmodifiable list of active agents
     */
    public List<AgentStatus> listActiveAgents() {
        return agentCache.values().stream()
            .filter(AgentStatus::isRunning)
            .toList();
    }

    /**
     * Lists all registered agents including those that are not running from the cache.
     *
     * @return an unmodifiable list of all agents in the registry
     */
    public List<AgentStatus> listAllAgents() {
        return agentCache.values().stream().toList();
    }

    /**
     * Gets the current size of the agent registry from the cache.
     *
     * @return total number of agents (including inactive)
     */
    public int getRegistrySize() {
        return agentCache.size();
    }
}
