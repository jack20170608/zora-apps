#!/usr/bin/env bash
# Submit a SimpleCounter task via REST API

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-1002}"
TIMESTAMP=$(iso_now)

echo "=== Test: Submit SimpleCounter (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "CounterApi",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.SimpleCounterExecution",
  "input": "{\\"from\\":1,\\"to\\":3,\\"intervalMillisecond\\":100}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY"
