package top.ilovemyhome.dagtask.si.agent;

/**
 * Record that encapsulates task execution result information to be sent to the DAG scheduling server.
 *
 * @param agentId unique identifier of the agent that executed the task
 * @param taskId unique identifier of the executed task
 * @param success whether the task execution succeeded
 * @param output the output JSON from the execution
 */
public record TaskResultReport(
    String agentId,
    long taskId,
    boolean success,
    String output
) {}
