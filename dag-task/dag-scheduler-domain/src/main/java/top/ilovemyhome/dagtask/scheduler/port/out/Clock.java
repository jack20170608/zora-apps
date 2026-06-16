package top.ilovemyhome.dagtask.scheduler.port.out;

import java.time.Instant;

/**
 * Domain-facing clock abstraction (not {@link java.time.Clock}). Adapter typically
 * returns {@link Instant#now()}; tests substitute a fixed-time implementation.
 */
public interface Clock {

    Instant now();
}
