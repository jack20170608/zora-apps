#!/usr/bin/env bash
# Submit an interruptible deadlock task via REST API.
# Unlike the classic deadlock, this one uses ReentrantLock.lockInterruptibly()
# so that kill can actually interrupt and resolve the deadlock.

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-9001}"
TIMESTAMP=$(iso_now)

echo "=== Test: Submit Interruptible Deadlock (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "InterruptibleDeadlockApi",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.InterruptibleDeadlockExecution",
  "input": "{\\"description\\":\\"interruptible deadlock api test\\"}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY"
