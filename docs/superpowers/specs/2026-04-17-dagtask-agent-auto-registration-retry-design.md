# Design: DAG Task Agent Auto-Registration Retry with Simulated Annealing

## Problem Statement
Currently, when `DagTaskAgent` starts with `autoRegister = true`, it only attempts registration once. If registration fails (due to network issues, server not ready yet, etc.), the agent continues running but will never be registered with the scheduler, making it useless for executing tasks.

The requirement is to add automatic retries with simulated annealing (exponential backoff) when registration fails at startup:
- Start with 10ms delay
- Increase delay exponentially on each retry
- Cap maximum delay at 5 minutes
- Keep retrying until registration succeeds or agent is stopped

## Design Decision

### Approach: Asynchronous Background Retry
We choose **asynchronous non-blocking retry in a background daemon thread** because:
1. Doesn't block the main startup sequence - HTTP server can start immediately
2. Agent can serve health checks and monitoring even while retrying
3. Fits the existing architecture where `DagTaskAgent` doesn't start the HTTP server itself
4. This is the standard pattern for service discovery in cloud-native applications

## Implementation Details

### Changes to `DagTaskAgent` class

**Fields added:**
- `private final Thread registrationRetryThread;` - holds reference to the background thread for interruption on stop
- `private volatile boolean registered;` - tracks whether registration has succeeded

**Modified `start()` method:**
1. Start `executionEngine` as before
2. If `autoRegister` is enabled:
   - Make first registration attempt synchronously
   - If successful → mark `registered = true` and continue
   - If failed → create and start a daemon thread for retries
3. Main startup completes immediately

**Background Retry Logic:**
```java
class RegistrationRetryTask implements Runnable {
    private long currentDelayMs = INITIAL_DELAY_MS;
    private boolean done = false;

    @Override
    public void run() {
        while (!done && isRunning()) {
            try {
                // Apply jitter: ±10% random variation
                long jitteredDelay = applyJitter(currentDelayMs);
                Thread.sleep(jitteredDelay);

                Response response = agentSchedulerClient.register(registration);
                boolean success = response.getStatus() >= 200 && response.getStatus() < 300;

                if (success) {
                    LOGGER.info("Agent {} registered successfully after retry", registration.agentId());
                    registered = true;
                    done = true;
                    return;
                }

                LOGGER.warn("Registration retry failed, status={}, will retry in {}ms",
                    response.getStatus(), currentDelayMs);

                // Increase delay with exponential backoff, capped at MAX_DELAY_MS
                currentDelayMs = Math.min(currentDelayMs * 2, MAX_DELAY_MS);
            } catch (InterruptedException e) {
                LOGGER.info("Registration retry interrupted, stopping retry");
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
```

**Modified `stop()` method:**
- Interrupt the registration retry thread if it's still running
- This ensures clean shutdown

### Configuration Parameters (Hardcoded as per requirement)
```java
private static final long INITIAL_DELAY_MS = 10L;       // 10ms initial delay
private static final long MAX_DELAY_MS = 5 * 60 * 1000L; // 5 minutes maximum
private static final double JITTER_FACTOR = 0.1;        // ±10% jitter
```

### Jitter Explanation
Adding ±10% random jitter prevents the "thundering herd" problem when multiple agents restart simultaneously. All agents won't retry at exactly the same time, which reduces load on the scheduler server.

### Thread Safety
- `registered` flag is `volatile` → safe for cross-thread visibility
- Background thread is a daemon → doesn't block JVM shutdown if agent is not stopped explicitly
- Interrupt is handled properly for clean shutdown

## Error Handling
- Any exception during registration (IOExceptions, InterruptedExceptions) is caught and logged
- Retry continues after exception
- If thread is interrupted (agent stop), it exits gracefully
- After reaching max delay (5 minutes), keeps retrying indefinitely at that interval

## Testing Considerations
- Unit test for the backoff calculation and jitter
- Integration test with mock server that initially rejects registration then accepts
- Verify thread interruption on stop works correctly

## Impact on Existing Code
- Minimal changes to `DagTaskAgent` only
- No changes to public API
- Doesn't affect existing behavior when autoRegister is false
- Backward compatible

## Summary
This design implements the required auto-registration retry with simulated annealing (exponential backoff) in a non-blocking way that fits the existing architecture. It ensures the agent will eventually register with the scheduler even if the scheduler is temporarily unavailable at startup time.
