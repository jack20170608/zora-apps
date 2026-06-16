package top.ilovemyhome.dagtask.scheduler.port.out;

import java.util.function.Supplier;

/**
 * Domain-facing transaction boundary. Application services wrap multi-step
 * operations that must be atomic on the underlying store.
 *
 * <p>Adapter implementations:
 * <ul>
 *   <li>JDBC: real database transaction</li>
 *   <li>file: in-process lock + best-effort consistency</li>
 *   <li>KV: single-key atomic ops only</li>
 * </ul>
 *
 * <p>Application services MUST NOT assume strong cross-aggregate consistency.
 */
public interface UnitOfWork {

    <T> T execute(Supplier<T> work);

    default void execute(Runnable work) {
        execute(() -> {
            work.run();
            return null;
        });
    }
}
