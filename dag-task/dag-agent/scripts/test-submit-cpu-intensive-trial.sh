#!/usr/bin/env bash
# Submit a CPU-intensive task (trial division algorithm) via REST API

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-5002}"
TIMESTAMP=$(iso_now)

echo "=== Test: Submit CPU-Intensive Trial (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "CpuIntensiveTrialApi",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.CpuIntensiveExecution",
  "input": "{\\"upperBound\\":20000000,\\"algorithm\\":\\"trial\\",\\"iterations\\":10}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

api_post "submit" "$BODY"
