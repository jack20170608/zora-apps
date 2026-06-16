package top.ilovemyhome.dagtask.scheduler.port.out;

/**
 * Generates unique identifiers for new entities. Implementations may use UUID,
 * snowflake, database sequences, or other strategies — the domain does not care.
 *
 * <p>Return types are deliberately heterogeneous to match the existing entity
 * conventions: task and order IDs are numeric (DB sequences), agent token IDs
 * are opaque strings (typically UUIDs).
 */
public interface IdGenerator {

    long nextTaskId();

    long nextOrderId();

    String nextAgentTokenId();
}
