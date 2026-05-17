#!/usr/bin/env bash
# Submit a deadlock task via REST API.
# The task spawns two threads that deadlock forever — useful for testing kill/force-ok behavior.

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-6001}"
TIMESTAMP=$(iso_now)

echo "=== Test: Submit Deadlock (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "DeadlockApi",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.DeadlockExecution",
  "input": "{\\"description\\":\\"deadlock api test\\"}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY"
