#!/usr/bin/env bash
# Test health check endpoint (GET /health)

source "$(dirname "$0")/run-api.sh"

echo "=== Test: Health ==="

api_get "health"
