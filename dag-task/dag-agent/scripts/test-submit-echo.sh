#!/usr/bin/env bash
# Submit an Echo task via REST API

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-1003}"
TIMESTAMP=$(iso_now)

echo "=== Test: Submit Echo (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "EchoApi",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.EchoExecution",
  "input": "{\\"greeting\\":\\"hello\\",\\"target\\":\\"api\\"}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY"
