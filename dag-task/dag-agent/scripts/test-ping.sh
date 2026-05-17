#!/usr/bin/env bash
# Test server heartbeat (GET /ping)

source "$(dirname "$0")/run-api.sh"

echo "=== Test: Ping ==="

api_get "ping"
