#!/usr/bin/env bash
# Submit a task with non-existent execution class (expected to fail)

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-2001}"
TIMESTAMP=$(iso_now)

echo "=== Test: Submit Invalid Class (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "InvalidClassApi",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.NonExistent",
  "input": "{}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY"

# Expected to fail
exit 0
