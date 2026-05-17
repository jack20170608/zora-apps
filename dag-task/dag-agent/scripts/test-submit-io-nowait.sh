#!/usr/bin/env bash
# Submit an I/O non-wait task via REST API.
# The task polls for a connection in a non-blocking Selector loop — useful for testing I/O-bound cancellation.

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-7001}"
TIMESTAMP=$(iso_now)

echo "=== Test: Submit I/O Non-Wait (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "IoNonWaitApi",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.IoNoWaitExecution",
  "input": "{\\"port\\":0,\\"description\\":\\"io non-wait api test\\"}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY"
