#!/usr/bin/env bash
# Full lifecycle test: submit a CPU-intensive task, check health while running, then kill it

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-5003}"
TIMESTAMP=$(iso_now)

echo "========================================"
echo "CPU-Intensive Lifecycle Test (taskId=$TASK_ID)"
echo "========================================"

# 1. Submit a CPU-intensive task (large sieve to ensure it runs long enough)
echo ""
echo "Step 1: Submit CPU-intensive task"
BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "LifecycleCpuIntensive",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.CpuIntensiveExecution",
  "input": "{\\"upperBound\\":5000000,\\"algorithm\\":\\"sieve\\",\\"iterations\\":500}",
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

sleep 1

# 4. Check health to confirm the task is gone
echo ""
echo "Step 4: Check health (task should be gone)"
api_get "health"

echo ""
echo "========================================"
echo "CPU-Intensive lifecycle test complete"
echo "========================================"
