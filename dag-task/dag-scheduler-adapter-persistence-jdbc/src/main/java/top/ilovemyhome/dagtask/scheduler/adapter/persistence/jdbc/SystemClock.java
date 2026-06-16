package top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc;

import top.ilovemyhome.dagtask.scheduler.port.out.Clock;

import java.time.Instant;

/**
 * Temporary adapter: domain {@link Clock} backed by the system clock.
 * Will be moved to a proper adapter module in step 3.
 */
public class SystemClock implements Clock {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
