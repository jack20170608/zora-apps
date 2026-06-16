package top.ilovemyhome.dagtask.scheduler.application;

import top.ilovemyhome.dagtask.scheduler.port.out.TaskRecordRepository;
import top.ilovemyhome.dagtask.si.TaskOutput;
import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.TaskExecuteResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import top.ilovemyhome.dagtask.scheduler.port.out.Clock;

/**
 * Application service for processing task execution results reported by agents.
 * <p>
 * Replaces the legacy {@code AgentRegistryService.reportTaskResult} (single and batch).
 * </p>
 * <p>
 * Note (TD-4): the legacy implementation passes {@code taskRecord.getStatus()} to
 * {@code stop()}, which is arguably wrong — it should derive the status from
 * {@code taskExecuteResult.success()}. This service preserves the old behavior
 * to avoid introducing a behavioral change during the refactor.
 * </p>
 */
public class ReportTaskResultService implements top.ilovemyhome.dagtask.scheduler.port.in.ReportTaskResultUseCase {

    private final TaskRecordRepository taskRecordRepository;
    private final Clock clock;

    public ReportTaskResultService(TaskRecordRepository taskRecordRepository, Clock clock) {
        this.taskRecordRepository = Objects.requireNonNull(taskRecordRepository, "taskRecordRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public boolean reportTaskResult(TaskExecuteResult taskExecuteResult) {
        if (taskExecuteResult == null || taskExecuteResult.agentId() == null || taskExecuteResult.agentId().isBlank()) {
            return false;
        }

        Optional<TaskRecord> taskRecordOpt = taskRecordRepository.loadTaskById(taskExecuteResult.taskId());
        if (taskRecordOpt.isEmpty()) {
            return false;
        }

        TaskRecord taskRecord = taskRecordOpt.get();
        TaskOutput output;
        if (taskExecuteResult.success()) {
            output = TaskOutput.success(taskExecuteResult.taskId(), taskExecuteResult.output());
        } else {
            output = TaskOutput.fail(taskExecuteResult.taskId(), taskExecuteResult.output(), "Task execution failed on agent");
        }

        // TD-4: legacy behavior preserved — uses taskRecord.getStatus() instead of
        // deriving the new status from taskExecuteResult.success().
        taskRecordRepository.stop(taskExecuteResult.taskId(), taskRecord.getStatus(), output, clock.now());
        return true;
    }

    @Override
    public boolean reportTaskResult(List<TaskExecuteResult> results) {
        if (results == null || results.isEmpty()) {
            return false;
        }

        boolean allSuccess = true;
        for (TaskExecuteResult result : results) {
            boolean success = reportTaskResult(result);
            if (!success) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }
}
