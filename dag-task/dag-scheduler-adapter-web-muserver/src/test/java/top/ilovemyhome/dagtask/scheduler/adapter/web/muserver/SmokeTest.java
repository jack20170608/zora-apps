package top.ilovemyhome.dagtask.scheduler.adapter.web.muserver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test ensuring the empty module compiles and the test framework runs.
 * Real HTTP handler tests arrive in step 3.
 */
class SmokeTest {
    @Test
    void module_compiles_and_test_framework_runs() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
