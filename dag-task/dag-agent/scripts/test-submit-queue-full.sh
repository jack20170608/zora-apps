#!/usr/bin/env bash
# Stress test: submit many tasks to fill the queue

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

echo "=== Test: Queue Stress (submit 50 tasks rapidly) ==="

TIMESTAMP=$(iso_now)
ACCEPTED=0
REJECTED=0

for i in $(seq 1 50); do
    TASK_ID=$((3000 + i))

    BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "StressTask-$i",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.EchoExecution",
  "input": "{\\"idx\\":$i}",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP",
  "reportResult": false
}
EOF
)

    resp=$(curl -s -w "\n%{http_code}" \
        -H "Content-Type: application/json" \
        -X POST -d "$BODY" \
        "$API_BASE/submit")

    code=$(echo "$resp" | tail -n 1)
    if [[ "$code" == "202" ]]; then
        ((ACCEPTED++))
        echo "Task $TASK_ID: ACCEPTED"
    else
        ((REJECTED++))
        echo "Task $TASK_ID: REJECTED (HTTP $code)"
    fi
done

echo ""
echo "========================================"
echo "Stress test complete"
echo "Accepted: $ACCEPTED"
echo "Rejected: $REJECTED"
echo "========================================"
