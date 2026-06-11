package top.ilovemyhome.dagtask.allinone.muserver.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.agent.core.TaskExecutionEngine;
import top.ilovemyhome.dagtask.agent.dto.SubmissionResult;
import top.ilovemyhome.dagtask.si.DispatchResult;
import top.ilovemyhome.dagtask.si.TaskDispatchRecord;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;
import top.ilovemyhome.dagtask.si.enums.DispatchStatus;
import top.ilovemyhome.dagtask.si.persistence.TaskDispatchDao;

import java.util.List;
import java.util.Optional;

/**
 * In-process task dispatcher that directly submits tasks to TaskExecutionEngine
 * via method call, eliminating HTTP overhead in all-in-one mode.
 */
public class InProcessTaskDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(InProcessTaskDispatcher.class);
    private static final String LOCAL_AGENT_ID = "local-agent";
    private static final String IN_PROCESS_URL = "in-process";

    private TaskExecutionEngine taskExecutionEngine;
    private final TaskDispatchDao dispatchDao;

    public InProcessTaskDispatcher(TaskDispatchDao dispatchDao) {
        this.dispatchDao = dispatchDao;
    }

    /**
     * Binds the TaskExecutionEngine after construction.
     * This two-phase initialization breaks the circular dependency between
     * scheduler and agent bootstrap.
     */
    public void bindTaskExecutionEngine(TaskExecutionEngine taskExecutionEngine) {
        this.taskExecutionEngine = taskExecutionEngine;
    }

    public DispatchResult dispatch(TaskRecord task) {
        if (taskExecutionEngine == null) {
            throw new IllegalStateException("TaskExecutionEngine not bound yet");
        }

        LOGGER.debug("Dispatching task {} to in-process agent", task.getId());

        try {
            // Direct method call instead of HTTP
            SubmissionResult result = taskExecutionEngine.submit(
                task.getId(),
                task.getName(),
                task.getExecutionKey(),
                task.getInput(),
                true
            );

            // Record dispatch tracking
            if (result.accepted()) {
                TaskDispatchRecord record = TaskDispatchRecord.builder()
                    .withTaskId(task.getId())
                    .withAgentId(LOCAL_AGENT_ID)
                    .withAgentUrl(IN_PROCESS_URL)
                    .withStatus(DispatchStatus.DISPATCHED)
                    .build();
                dispatchDao.create(record);
                AgentStatus localAgent = AgentStatus.builder()
                    .withAgentId(LOCAL_AGENT_ID)
                    .withAgentUrl(IN_PROCESS_URL)
                    .withRunning(true)
                    .build();
                return DispatchResult.success(localAgent, task.getId());
            }

            return new DispatchResult(false, null, task.getId(), result.message());

        } catch (Exception e) {
            LOGGER.error("Failed to dispatch task {} in-process", task.getId(), e);
            return new DispatchResult(false, null, task.getId(), e.getMessage());
        }
    }

    public boolean killTask(Long taskId, String dealer, String reason) {
        LOGGER.info("Killing task {} in-process (dealer={}, reason={})", taskId, dealer, reason);
        if (taskExecutionEngine == null) {
            return false;
        }
        return taskExecutionEngine.kill(taskId).success();
    }

    public boolean killTask(TaskDispatchRecord dispatchItem, String dealer, String reason) {
        return killTask(dispatchItem.getTaskId(), dealer, reason);
    }

    public boolean forceOkTask(Long taskId, String dealer, String reason) {
        LOGGER.info("Force-OK task {} in-process (dealer={}, reason={})", taskId, dealer, reason);
        if (taskExecutionEngine == null) {
            return false;
        }
        return taskExecutionEngine.forceOk(taskId).success();
    }

    public boolean forceOkTask(TaskDispatchRecord dispatchItem, String dealer, String reason) {
        return forceOkTask(dispatchItem.getTaskId(), dealer, reason);
    }

    public int countAvailableAgents(String executionKey) {
        // In-process mode always has exactly one agent available
        return taskExecutionEngine != null ? 1 : 0;
    }

    public List<AgentStatus> getAvailableCandidates(String executionKey) {
        // In-process mode does not use external agent candidates
        return List.of();
    }

    public List<AgentStatus> findAllActiveAgents() {
        // In-process mode does not track external agents
        return List.of();
    }

    public Optional<AgentStatus> findAgentByAgentId(String agentId) {
        // In-process mode does not track external agents
        return Optional.empty();
    }
}
