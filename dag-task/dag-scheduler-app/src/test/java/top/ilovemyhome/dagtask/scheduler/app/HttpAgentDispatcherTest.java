package top.ilovemyhome.dagtask.scheduler.app;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HttpAgentDispatcherTest {

    @Test
    void constructor_rejectsNullObjectMapper() {
        assertThatThrownBy(() -> new HttpAgentDispatcher(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("objectMapper");
    }
}
