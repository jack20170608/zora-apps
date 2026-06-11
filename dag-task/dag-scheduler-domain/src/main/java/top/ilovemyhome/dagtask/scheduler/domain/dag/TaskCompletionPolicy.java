package top.ilovemyhome.dagtask.scheduler.domain.dag;

import top.ilovemyhome.dagtask.si.TaskRecord;

import java.util.List;
import java.util.Objects;

/**
 * Pure-domain helper that encapsulates the logic triggered when a task completes:
 * checking the overall DAG health and (in a richer version) determining which
 * successor tasks are now ready.
 * <p>
 * Used by both {@code ScheduleDagRunService} and {@code ManualTaskOperationService}
 * to avoid application-layer cross-calls.
 * </p>
 */
public final class TaskCompletionPolicy {

    private TaskCompletionPolicy() {
        // utility class
    }

    /**
     * Determine which tasks in a candidate set are ready to run after the
     * completion of the given predecessor.
     * <p>
     * This method does <b>not</b> query the database — the caller is expected
     * to have already fetched the candidates and passed them in. The domain
     * logic is purely about filtering by status and verifying that the candidate
     * is indeed ready (all predecessors complete).
     * </p>
     *
     * @param completedTask     the task that just completed
     * @param candidateSuccessors potential successor tasks (pre-filtered by DAG structure)
     * @param isReadyFn         function that checks whether a task's predecessors are all complete
     * @return list of successor tasks that should now be dispatched
     */
    public static List<TaskRecord> findReadySuccessors(TaskRecord completedTask,
                                                       List<TaskRecord> candidateSuccessors,
                                                       IsReadyFn isReadyFn) {
        Objects.requireNonNull(completedTask, "completedTask must not be null");
        Objects.requireNonNull(candidateSuccessors, "candidateSuccessors must not be null");
        Objects.requireNonNull(isReadyFn, "isReadyFn must not be null");

        return candidateSuccessors.stream()
            .filter(s -> s.getStatus() == top.ilovemyhome.dagtask.si.enums.TaskStatus.INIT)
            .filter(s -> isReadyFn.isReady(s.getId()))
            .toList();
    }

    @FunctionalInterface
    public interface IsReadyFn {
        boolean isReady(Long taskId);
    }
}