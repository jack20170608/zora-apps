#!/usr/bin/env bash
# Full lifecycle test for an I/O blocked task:
# 1. Submit an I/O wait task (blocks on ServerSocket.accept)
# 2. Wait a few seconds to confirm it is stuck
# 3. Attempt to kill it via the API (will likely fail to interrupt)
# 4. Verify health still shows the task running

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-8002}"
TIMESTAMP=$(iso_now)

echo "========================================"
echo "I/O Wait Lifecycle Test (taskId=$TASK_ID)"
echo "========================================"
echo "NOTE: accept() uses a 1s timeout so kill can interrupt via checkInterruption()."
echo ""

# 1. Submit I/O wait task
echo "Step 1: Submit I/O wait task"
BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "LifecycleIoWait",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.IoWaitExecution",
  "input": "{\\"port\\":0,\\"description\\":\\"lifecycle io wait test\\"}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY" || exit 1

sleep 3

# 2. Check health — task should be running (blocked on accept)
echo ""
echo "Step 2: Check health (task should be running / blocked on accept)"
api_get "health"

sleep 2

# 3. Kill the task
echo ""
echo "Step 3: Kill the I/O blocked task"
BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "opsType": "KILL",
  "force": true,
  "reason": "io wait lifecycle test kill",
  "dealer": "test-script",
  "requestDt": "$(iso_now)"
}
EOF
)

api_post "kill" "$BODY"

sleep 1

# 4. Check health — task should be gone
echo ""
echo "Step 4: Check health (task should be gone)"
api_get "health"

echo ""
echo "========================================"
echo "I/O wait lifecycle test complete"
echo "========================================"
