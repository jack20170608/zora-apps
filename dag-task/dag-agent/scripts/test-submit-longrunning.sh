#!/usr/bin/env bash
# Submit a long-running task via REST API

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-1004}"
TIMESTAMP=$(iso_now)

echo "=== Test: Submit LongRunning (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "LongRunningApi",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.LongRunningExecution",
  "input": "{\\"durationSeconds\\":5,\\"description\\":\\"api test run\\"}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY"
