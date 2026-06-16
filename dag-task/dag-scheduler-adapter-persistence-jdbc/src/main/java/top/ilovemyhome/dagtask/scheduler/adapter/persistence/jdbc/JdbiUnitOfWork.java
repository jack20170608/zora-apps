package top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc;

import org.jdbi.v3.core.Jdbi;
import top.ilovemyhome.dagtask.scheduler.port.out.UnitOfWork;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Temporary adapter: domain {@link UnitOfWork} backed by jdbi transactions.
 * Will be moved to a proper adapter module in step 3.
 */
public class JdbiUnitOfWork implements UnitOfWork {

    private final Jdbi jdbi;

    public JdbiUnitOfWork(Jdbi jdbi) {
        this.jdbi = Objects.requireNonNull(jdbi, "jdbi must not be null");
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        return jdbi.inTransaction(h -> work.get());
    }
}
