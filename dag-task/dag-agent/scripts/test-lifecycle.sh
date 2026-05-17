#!/usr/bin/env bash
# Full lifecycle test: submit a long-running task, check health, then kill it

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-4001}"
TIMESTAMP=$(iso_now)

echo "========================================"
echo "Full Lifecycle Test (taskId=$TASK_ID)"
echo "========================================"

# 1. Submit a long-running task
echo ""
echo "Step 1: Submit long-running task"
BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "LifecycleLongRunning",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.LongRunningExecution",
  "input": "{\\"durationSeconds\\":30,\\"description\\":\\"lifecycle test\\"}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY" || exit 1

sleep 2

# 2. Check health to see running tasks
echo ""
echo "Step 2: Check health (task should be running)"
api_get "health"

sleep 1

# 3. Kill the task
echo ""
echo "Step 3: Kill the task"
BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "opsType": "KILL",
  "force": true,
  "reason": "lifecycle test kill",
  "dealer": "test-script",
  "requestDt": "$(iso_now)"
}
EOF
)

api_post "kill" "$BODY"

echo ""
echo "========================================"
echo "Lifecycle test complete"
echo "========================================"
