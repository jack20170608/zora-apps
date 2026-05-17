#!/usr/bin/env bash
# Full lifecycle test for a deadlocked task:
# 1. Submit a deadlock task
# 2. Wait a few seconds to confirm it is stuck
# 3. Kill it via the API
# 4. Verify health shows no running tasks

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-6002}"
TIMESTAMP=$(iso_now)

echo "========================================"
echo "Deadlock Lifecycle Test (taskId=$TASK_ID)"
echo "========================================"

# 1. Submit deadlock task
echo ""
echo "Step 1: Submit deadlock task"
BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "LifecycleDeadlock",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.DeadlockExecution",
  "input": "{\\"description\\":\\"lifecycle deadlock test\\"}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY" || exit 1

sleep 3

# 2. Check health — task should be running (deadlocked)
echo ""
echo "Step 2: Check health (task should be running / deadlocked)"
api_get "health"

sleep 2

# 3. Kill the task
echo ""
echo "Step 3: Kill the deadlocked task"
BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "opsType": "KILL",
  "force": true,
  "reason": "deadlock lifecycle test kill",
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
echo "Deadlock lifecycle test complete"
echo "========================================"
