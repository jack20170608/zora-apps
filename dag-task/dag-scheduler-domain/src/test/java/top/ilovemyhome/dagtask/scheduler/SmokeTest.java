package top.ilovemyhome.dagtask.scheduler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test ensuring the empty module compiles and JUnit + AssertJ are wired correctly.
 * Will be removed once real domain code arrives in step 2.
 */
class SmokeTest {
    @Test
    void module_compiles_and_test_framework_runs() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
