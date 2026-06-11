package top.ilovemyhome.dagtask.scheduler.port.in;

import java.util.List;

/**
 * Allocate a contiguous block of unique task ids for callers that need to know the
 * id before persisting (e.g. building a DAG with predecessor/successor links).
 * <p>
 * Replaces {@code TaskDagService.getNextTaskIds}. Backed by an {@code IdGenerator}
 * outbound port.
 * </p>
 */
public interface AllocateTaskIdsUseCase {

    /**
     * @param count number of ids to allocate (must be > 0)
     * @return list of {@code count} fresh, unique task ids
     */
    List<Long> getNextTaskIds(int count);
}
