#!/usr/bin/env bash
# Force a task to complete as failed

source "$(dirname "$0")/run-api.sh"

ping_server || exit 1

TASK_ID="${1:-1004}"
TIMESTAMP=$(iso_now)

echo "=== Test: Force NOK (taskId=$TASK_ID) ==="

BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "opsType": "FORCE_NOK",
  "force": true,
  "reason": "test force-nok via api",
  "dealer": "test-script",
  "requestDt": "$TIMESTAMP"
}
EOF
)

api_post "force-nok" "$BODY"
