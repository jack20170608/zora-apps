package top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc;

import org.jdbi.v3.core.Jdbi;
import top.ilovemyhome.dagtask.scheduler.port.out.IdGenerator;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Temporary adapter: domain {@link IdGenerator} backed by database sequences
 * (for numeric IDs) and secure random (for opaque token IDs).
 * Will be moved to a proper adapter module in step 3.
 */
public class SequenceIdGenerator implements IdGenerator {

    private final Jdbi jdbi;
    private final SecureRandom random = new SecureRandom();

    public SequenceIdGenerator(Jdbi jdbi) {
        this.jdbi = Objects.requireNonNull(jdbi, "jdbi must not be null");
    }

    @Override
    public long nextTaskId() {
        return jdbi.withHandle(h -> h.createQuery("select nextval('seq_t_task_id')")
            .mapTo(Long.class)
            .one());
    }

    @Override
    public long nextOrderId() {
        // Reuses the same sequence as task ids; no dedicated order sequence exists.
        return nextTaskId();
    }

    @Override
    public String nextAgentTokenId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
