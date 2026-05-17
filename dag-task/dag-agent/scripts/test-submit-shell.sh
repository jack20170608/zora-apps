#!/usr/bin/env bash
# Submit a shell echo task via REST API

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-1001}"
TIMESTAMP=$(iso_now)

echo "=== Test: Submit Shell Echo (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "ShellEchoApi",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution",
  "input": "{\\"command\\":\\"echo hello from api\\"}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY"
