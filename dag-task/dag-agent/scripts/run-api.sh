#!/usr/bin/env bash
#
# Common setup for dag-agent REST API test scripts.
# Source this file at the top of each test script:
#   source "$(dirname "$0")/run-api.sh"
#

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Server configuration
BASE_URL="${DAG_AGENT_URL:-http://localhost:20000}"
API_BASE="$BASE_URL/agent/api/v1/agent"

# Colors for terminal output (fallback to plain if not supported)
if [[ -t 1 ]]; then
    GREEN='\033[0;32m'
    RED='\033[0;31m'
    YELLOW='\033[1;33m'
    NC='\033[0m'
else
    GREEN=''
    RED=''
    YELLOW=''
    NC=''
fi

# Check server is reachable
ping_server() {
    local url="$API_BASE/ping"
    local resp
    resp=$(curl -sf --max-time 3 "$url" 2>/dev/null || echo "")
    if [[ "$resp" != "pong" ]]; then
        echo -e "${RED}ERROR: dag-agent server is not reachable at $BASE_URL${NC}"
        echo "Please start the server first, e.g.:"
        echo "  cd dag-task/dag-agent-muserver && mvn exec:java -Denv=local"
        return 1
    fi
    return 0
}

# Print HTTP request details for debugging
log_request() {
    local method="$1"
    local url="$2"
    local body="${3:-}"
    echo ""
    echo "========================================"
    echo "Request: $method $url"
    if [[ -n "$body" ]]; then
        echo "Body: $body"
    fi
    echo "========================================"
}

# Print formatted response
log_response() {
    local status="$1"
    local body="$2"
    echo "HTTP $status"
    echo "Response: $body"
    echo ""
}

# Generic POST helper
api_post() {
    local endpoint="$1"
    local body="$2"
    local url="$API_BASE/$endpoint"

    log_request "POST" "$url" "$body"

    local resp
    resp=$(curl -s -w "\n%{http_code}" \
        -H "Content-Type: application/json" \
        -X POST \
        -d "$body" \
        "$url")

    local http_code
    http_code=$(echo "$resp" | tail -n 1)
    local body_out
    body_out=$(echo "$resp" | sed '$d')

    log_response "$http_code" "$body_out"

    if [[ "$http_code" -ge 200 && "$http_code" -lt 300 ]]; then
        return 0
    else
        return 1
    fi
}

# Generic GET helper
api_get() {
    local endpoint="$1"
    local url="$API_BASE/$endpoint"

    log_request "GET" "$url"

    local resp
    resp=$(curl -s -w "\n%{http_code}" \
        -H "Accept: application/json" \
        -X GET \
        "$url")

    local http_code
    http_code=$(echo "$resp" | tail -n 1)
    local body_out
    body_out=$(echo "$resp" | sed '$d')

    log_response "$http_code" "$body_out"

    if [[ "$http_code" -ge 200 && "$http_code" -lt 300 ]]; then
        return 0
    else
        return 1
    fi
}

# Build an ISO-8601 timestamp
iso_now() {
    # Use powershell on Windows if available, otherwise date
    if command -v powershell > /dev/null 2>&1; then
        powershell -Command "Get-Date -Format o | ForEach-Object { \$_ -replace '\+.*','Z' }"
    else
        date -u +"%Y-%m-%dT%H:%M:%SZ"
    fi
}
