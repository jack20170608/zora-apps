package top.ilovemyhome.dagtask.scheduler.domain.query;

import java.util.List;

/**
 * Domain-owned paginated result. Replaces the zora-jdbi {@code Page}
 * leak in inbound / outbound port signatures (TD-1).
 *
 * @param <T> the element type
 */
public record Page<T>(List<T> content, int number, int size, long totalElements, int totalPages) {

    /**
     * Convenience factory that computes {@code totalPages} from {@code totalElements} and {@code size}.
     */
    public static <T> Page<T> of(List<T> content, int number, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new Page<>(content, number, size, totalElements, totalPages);
    }
}
