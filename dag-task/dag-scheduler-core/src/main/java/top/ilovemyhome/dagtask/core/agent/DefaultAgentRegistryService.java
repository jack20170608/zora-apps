package top.ilovemyhome.dagtask.core.agent;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.persistence.AgentRegistryDao;
import top.ilovemyhome.dagtask.si.persistence.TaskRecordDao;
import top.ilovemyhome.dagtask.si.agent.AgentInfo;
import top.ilovemyhome.dagtask.si.agent.AgentRegistration;
import top.ilovemyhome.dagtask.si.agent.AgentStatusReport;
import top.ilovemyhome.dagtask.si.agent.AgentUnregistration;
import top.ilovemyhome.dagtask.si.agent.TaskResultReport;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link AgentRegistryService} with database persistence.
 * Maintains an in-memory cache of agent information for fast access and persists
 * all changes to the database using {@link AgentRegistryDao}.
 *
 * This implementation uses {@link ConcurrentHashMap} for thread-safe cache access
 * and delegates persistence to the DAO layer, allowing the registry to survive
 * server restarts.
 */
public class DefaultAgentRegistryService implements AgentRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAgentRegistryService.class);

    private final TaskRecordDao taskRecordDao;
    private final AgentRegistryDao agentRegistryDao;
    private final ConcurrentHashMap<String, AgentInfo> agentCache = new ConcurrentHashMap<>();

    /**
     * Creates a DefaultAgentRegistryService with the required dependencies.
     *
     * @param taskRecordDao DAO for updating task records when results are reported
     * @param agentRegistryDao DAO for persisting agent registry information
     */
    public DefaultAgentRegistryService(TaskRecordDao taskRecordDao, AgentRegistryDao agentRegistryDao) {
        this.taskRecordDao = Objects.requireNonNull(taskRecordDao, "taskRecordDao must not be null");
        this.agentRegistryDao = Objects.requireNonNull(agentRegistryDao, "agentRegistryDao must not be null");
        // Warm the cache from database
        loadAllFromDatabase();
    }

    /**
     * Loads all agents from database into the in-memory cache on startup.
     */
    private void loadAllFromDatabase() {
        List<AgentInfo> allAgents = agentRegistryDao.findAll();
        allAgents.forEach(agent -> agentCache.put(agent.agentId(), agent));
        logger.info("Loaded {} agents from database into registry cache", allAgents.size());
    }

    @Override
    public boolean registerAgent(AgentRegistration registration) {
        if (registration == null || StringUtils.isBlank(registration.agentId())) {
            logger.warn("Cannot register agent: invalid registration (agentId is blank)");
            return false;
        }

        AgentInfo agentInfo = AgentInfo.fromRegistration(registration);

        if (agentRegistryDao.exists(registration.agentId())) {
            // Update existing agent in database
            agentRegistryDao.updateStatus(
                registration.agentId(),
                true,
                0,
                0,
                0
            );
            logger.info("Agent [{}] already exists in database, reactivating", registration.agentId());
        } else {
            // Insert new agent into database
            agentRegistryDao.create(agentInfo);
        }

        // Update cache
        agentCache.put(registration.agentId(), agentInfo);
        logger.info("Agent [{}] registered successfully. URL: {}, max concurrent: {}",
            registration.agentId(), registration.agentUrl(), registration.maxConcurrentTasks());
        return true;
    }

    @Override
    public boolean unregisterAgent(AgentUnregistration unregistration) {
        if (unregistration == null || StringUtils.isBlank(unregistration.agentId())) {
            logger.warn("Cannot unregister agent: invalid agentId");
            return false;
        }

        if (!agentRegistryDao.exists(unregistration.agentId())) {
            logger.warn("Cannot unregister agent [{}]: agent not found in database", unregistration.agentId());
            return false;
        }

        // Mark as unregistered in database
        agentRegistryDao.markUnregistered(unregistration.agentId());

        // Update cache
        AgentInfo existing = agentCache.get(unregistration.agentId());
        if (existing != null) {
            agentCache.put(unregistration.agentId(), existing.withUnregistered());
        }

        logger.info("Agent [{}] unregistered successfully", unregistration.agentId());
        return true;
    }

    @Override
    public boolean reportTaskResult(TaskResultReport taskResultReport) {
        if (taskResultReport == null || StringUtils.isBlank(taskResultReport.agentId())) {
            logger.warn("Cannot process task result: invalid report");
            return false;
        }

        Optional<TaskRecord> taskRecordOpt = taskRecordDao.findOne(Long.valueOf(taskResultReport.taskId()));
        if (taskRecordOpt.isEmpty()) {
            logger.warn("Task [{}] not found when processing result report from agent [{}]",
                taskResultReport.taskId(), taskResultReport.agentId());
            return false;
        }

        TaskRecord taskRecord = taskRecordOpt.get();
        TaskOutput output;
        if (taskResultReport.success()) {
            output = TaskOutput.success(taskResultReport.taskId(), taskResultReport.output());
        } else {
            output = TaskOutput.fail(taskResultReport.taskId(), taskResultReport.output(), "Task execution failed on agent");
        }

        // Task status should be already set to RUNNING by scheduler before dispatching
        // Update the task to final status based on the result from agent
        taskRecordDao.stop(taskResultReport.taskId(), taskRecord.getStatus(), output, java.time.LocalDateTime.now());
        logger.debug("Processed task result for task [{}] from agent [{}], success: {}",
            taskResultReport.taskId(), taskResultReport.agentId(), taskResultReport.success());
        return true;
    }

    @Override
    public boolean reportAgentStatus(AgentStatusReport statusReport) {
        if (statusReport == null || StringUtils.isBlank(statusReport.agentId())) {
            logger.warn("Cannot process agent status: invalid report");
            return false;
        }

        if (!agentRegistryDao.exists(statusReport.agentId())) {
            logger.warn("Agent [{}] not found in database when processing status report", statusReport.agentId());
            return false;
        }

        // Update database
        agentRegistryDao.updateStatus(
            statusReport.agentId(),
            statusReport.running(),
            statusReport.pendingTasks(),
            statusReport.runningTasks(),
            statusReport.finishedTasks()
        );

        // Update cache
        AgentInfo existing = agentCache.get(statusReport.agentId());
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
     * @return the agent information if found, empty otherwise
     */
    public Optional<AgentInfo> getAgent(String agentId) {
        return Optional.ofNullable(agentCache.get(agentId));
    }

    /**
     * Lists all registered agents that are currently marked as running from the cache.
     *
     * @return an unmodifiable list of active agents
     */
    public List<AgentInfo> listActiveAgents() {
        return agentCache.values().stream()
            .filter(AgentInfo::running)
            .toList();
    }

    /**
     * Lists all registered agents including those that are not running from the cache.
     *
     * @return an unmodifiable list of all agents in the registry
     */
    public List<AgentInfo> listAllAgents() {
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
