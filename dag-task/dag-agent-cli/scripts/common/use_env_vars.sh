#!/bin/bash
#
# Helper script: reads and prints environment variables.
# Used to test environment variable injection from the task executor.
#

echo "=== Environment Variables Test ==="
echo ""

# Required variables
if [[ -n "${MY_VAR:-}" ]]; then
    echo "MY_VAR=$MY_VAR"
else
    echo "MY_VAR is not set"
fi

if [[ -n "${TASK_NAME:-}" ]]; then
    echo "TASK_NAME=$TASK_NAME"
else
    echo "TASK_NAME is not set"
fi

# Optional variables with defaults
echo "CUSTOM_PREFIX=${CUSTOM_PREFIX:-default-prefix}"
echo "RETRY_COUNT=${RETRY_COUNT:-0}"

echo ""
echo "=== All vars starting with MY_ ==="
env | grep "^MY_" || echo "(none found)"

echo ""
echo "✅ Environment check complete"
