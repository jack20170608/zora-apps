package top.ilovemyhome.dagtask.scheduler.config;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class AutoApproveConfigTest {

    @Test
    void shouldMatchExact() {
        var config = new AutoApproveConfig(true, List.of("dev-agent-01"));
        assertThat(config.isMatch("dev-agent-01")).isTrue();
        assertThat(config.isMatch("dev-agent-02")).isFalse();
    }

    @Test
    void shouldMatchPrefix() {
        var config = new AutoApproveConfig(true, List.of("prod-*"));
        assertThat(config.isMatch("prod-")).isTrue();
        assertThat(config.isMatch("prod-worker")).isTrue();
        assertThat(config.isMatch("prod-123")).isTrue();
        assertThat(config.isMatch("pr")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenDisabled() {
        var config = new AutoApproveConfig(false, List.of("prod-*"));
        assertThat(config.isMatch("prod-worker")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenNoPatterns() {
        var config = new AutoApproveConfig(true, null);
        assertThat(config.isMatch("prod-worker")).isFalse();
    }

    @Test
    void shouldMatchWithWildcardInMiddle() {
        var config = new AutoApproveConfig(true, List.of("*-agent-*"));
        assertThat(config.isMatch("prod-agent-01")).isTrue();
        assertThat(config.isMatch("dev-agent-05")).isTrue();
        assertThat(config.isMatch("prodworker")).isFalse();
    }
}
