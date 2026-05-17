#!/usr/bin/env bash
# Full lifecycle test for an interruptible deadlock task:
# 1. Submit an interruptible deadlock task
# 2. Wait a few seconds to confirm it is deadlocked
# 3. Kill it via the API (should work because lockInterruptibly() responds to interrupt)
# 4. Verify health shows no running tasks

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-9002}"
TIMESTAMP=$(iso_now)

echo "========================================"
echo "Interruptible Deadlock Lifecycle Test (taskId=$TASK_ID)"
echo "========================================"
echo "NOTE: This deadlock uses ReentrantLock.lockInterruptibly(),"
echo "      so kill SHOULD be able to interrupt and resolve it."
echo ""

# 1. Submit interruptible deadlock task
echo "Step 1: Submit interruptible deadlock task"
BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "LifecycleInterruptibleDeadlock",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.InterruptibleDeadlockExecution",
  "input": "{\\"description\\":\\"lifecycle interruptible deadlock test\\"}",
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

# 3. Kill the task — this SHOULD work
echo ""
echo "Step 3: Kill the interruptible deadlock task"
BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "opsType": "KILL",
  "force": true,
  "reason": "interruptible deadlock lifecycle test kill",
  "dealer": "test-script",
  "requestDt": "$(iso_now)"
}
EOF
)

api_post "kill" "$BODY"

sleep 1

# 4. Check health — task should be gone (kill succeeded)
echo ""
echo "Step 4: Check health (task should be gone — kill should have succeeded)"
api_get "health"

echo ""
echo "========================================"
echo "Interruptible deadlock lifecycle test complete"
echo "========================================"
