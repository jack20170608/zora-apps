#!/usr/bin/env bash
# Hold a pending or running task

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-1004}"
TIMESTAMP=$(iso_now)

echo "=== Test: Hold Task (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "opsType": "HOLD",
  "force": true,
  "reason": "test hold via api",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP"
}
EOF
)

api_post "hold" "$BODY"
