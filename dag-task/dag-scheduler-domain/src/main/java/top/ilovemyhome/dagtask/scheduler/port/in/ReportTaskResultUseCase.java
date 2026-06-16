package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;

import java.util.List;

/**
 * Receive task execution results reported by agents back to the scheduling center.
 * <p>
 * Wraps the legacy {@code AgentRegistryService.reportTaskResult} (single and batch).
 * </p>
 */
public interface ReportTaskResultUseCase {

    /**
     * Process a task execution result reported by an agent.
     *
     * @param taskExecuteResult the task result report containing execution outcome
     * @return {@code true} if the result was processed successfully, {@code false} otherwise
     */
    boolean reportTaskResult(TaskExecuteResult taskExecuteResult);

    /**
     * Batched version. Default implementation throws
     * {@link UnsupportedOperationException} for parity with the legacy default.
     */
    default boolean reportTaskResult(List<TaskExecuteResult> results) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
