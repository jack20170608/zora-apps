package top.ilovemyhome.dagtask.scheduler.port.in;

/**
 * Read-side queries against task orders.
 * <p>
 * Replaces {@code TaskOrderService.isOrdered}.
 * </p>
 */
public interface QueryTaskOrderUseCase {

    /**
     * @param orderKey the order key to look up
     * @return {@code true} if an order with the given key exists
     */
    boolean isOrdered(String orderKey);
}
