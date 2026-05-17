#!/usr/bin/env bash
# Submit a CPU-intensive task via REST API

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-5001}"
TIMESTAMP=$(iso_now)

echo "=== Test: Submit CPU-Intensive (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "CpuIntensiveApi",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.CpuIntensiveExecution",
  "input": "{\\"upperBound\\":500000,\\"algorithm\\":\\"sieve\\",\\"iterations\\":5}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY"
