package top.ilovemyhome.dagtask.scheduler.application;

import top.ilovemyhome.dagtask.scheduler.port.out.IdGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Application service that allocates contiguous blocks of unique task IDs.
 * <p>
 * Replaces the legacy {@code TaskDagService.getNextTaskIds} which used
 * {@code TaskRecordDao.getNextId()} directly. This service goes through the
 * {@link IdGenerator} outbound port, keeping the domain free from DAO details.
 * </p>
 */
public class AllocateTaskIdsService implements top.ilovemyhome.dagtask.scheduler.port.in.AllocateTaskIdsUseCase {

    private final IdGenerator idGenerator;

    public AllocateTaskIdsService(IdGenerator idGenerator) {
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
    }

    @Override
    public List<Long> getNextTaskIds(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("The count must be greater than 0");
        }
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(idGenerator.nextTaskId());
        }
        return List.copyOf(ids);
    }
}
