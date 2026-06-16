package top.ilovemyhome.dagtask.scheduler.domain.query;

/**
 * Domain-owned pagination request. Replaces the zora-jdbi {@code Pageable}
 * leak in inbound / outbound port signatures (TD-1).
 *
 * @param pageNumber 1-based page number (must be >= 1)
 * @param pageSize   number of elements per page (must be >= 1)
 */
public record Pageable(int pageNumber, int pageSize) {

    public Pageable {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be >= 1, got: " + pageNumber);
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1, got: " + pageSize);
        }
    }

    /**
     * Zero-based offset for SQL {@code OFFSET} clauses.
     */
    public int offset() {
        return (pageNumber - 1) * pageSize;
    }
}
