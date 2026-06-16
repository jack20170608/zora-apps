package top.ilovemyhome.dagtask.scheduler.port.in;

/**
 * Query overall completion status of a DAG run.
 * <p>
 * Replaces {@code TaskDagService.isSuccess}.
 * </p>
 */
public interface QueryDagRunStatusUseCase {

    /**
     * @param orderKey the order key to inspect
     * @return {@code true} if every task in the order has succeeded
     */
    boolean isSuccess(String orderKey);
}
