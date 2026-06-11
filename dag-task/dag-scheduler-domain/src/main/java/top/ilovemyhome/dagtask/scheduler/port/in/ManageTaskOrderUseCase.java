package top.ilovemyhome.dagtask.scheduler.port.in;

import top.ilovemyhome.dagtask.scheduler.application.DagNotFoundException;
import top.ilovemyhome.dagtask.scheduler.application.OrderKeyAlreadyExistsException;
import top.ilovemyhome.dagtask.si.TaskOrder;

/**
 * Write-side operations on task orders.
 * <p>
 * Replaces {@code TaskOrderService.createOrder/updateOrderByKey/deleteOrderByKey}.
 * Read-side operations live in {@link QueryTaskOrderUseCase}.
 * </p>
 */
public interface ManageTaskOrderUseCase {

    /**
     * Persist a new task order; returns the generated id.
     *
     * @throws OrderKeyAlreadyExistsException if the {@code order.getOrderKey()} is already in use
     */
    Long createOrder(TaskOrder order);

    /**
     * Update a task order located by its business key.
     *
     * @return number of rows affected (currently 0 or 1; non-zero means update was applied)
     * @throws DagNotFoundException if no order with the given key exists
     */
    int updateOrderByKey(String orderKey, TaskOrder taskOrder);

    /**
     * Delete a task order by its business key.
     * <p>
     * Lenient semantics: returns 0 if no such order exists (no exception thrown).
     * </p>
     *
     * @return number of rows deleted (0 if no such order exists)
     */
    int deleteOrderByKey(String orderKey);
}
