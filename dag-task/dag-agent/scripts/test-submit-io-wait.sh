#!/usr/bin/env bash
# Submit an I/O wait task via REST API.
# The task blocks on ServerSocket.accept() forever — useful for testing the kill boundary.

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-8001}"
TIMESTAMP=$(iso_now)

echo "=== Test: Submit I/O Wait (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "IoWaitApi",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.IoWaitExecution",
  "input": "{\\"port\\":0,\\"description\\":\\"io wait api test\\"}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY"
