package top.ilovemyhome.dagtask.scheduler.port.out;

import top.ilovemyhome.dagtask.si.TaskRecord;
import top.ilovemyhome.dagtask.si.agent.AgentStatus;

/**
 * Outbound port for delivering a task assignment to an agent.
 * Today implemented as HTTP (DefaultTaskDispatcher in dag-scheduler); future
 * could be gRPC, MQ, or in-process.
 */
public interface AgentDispatcher {

    /**
     * Send the task to the chosen agent.
     *
     * @param targetAgent the agent to deliver the task to
     * @param task        the task to dispatch
     * @return an acknowledgement describing whether the agent accepted the task
     * @throws AgentUnreachableException if delivery fails (network / agent down)
     */
    DispatchAck dispatch(AgentStatus targetAgent, TaskRecord task);

    /**
     * Result of a successful network round-trip to the agent.
     *
     * @param accepted true if the agent agreed to run the task; false if the agent
     *                 received the dispatch but refused it (e.g. capacity full).
     *                 Network failures throw {@link AgentUnreachableException}
     *                 instead of returning DispatchAck.
     * @param message  human-readable status, may be empty.
     */
    record DispatchAck(boolean accepted, String message) {}
}
