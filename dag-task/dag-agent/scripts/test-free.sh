#!/usr/bin/env bash
# Free (release) a previously held task

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-1004}"
TIMESTAMP=$(iso_now)

echo "=== Test: Free Task (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "opsType": "FREE",
  "force": true,
  "reason": "test free via api",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP"
}
EOF
)

api_post "free" "$BODY"
