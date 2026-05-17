#!/usr/bin/env bash
# Kill a running or pending task

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-1004}"
TIMESTAMP=$(iso_now)

echo "=== Test: Kill Task (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "opsType": "KILL",
  "force": true,
  "reason": "test kill via api",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP"
}
EOF
)

api_post "kill" "$BODY"
