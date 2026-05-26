#!/usr/bin/env bash
#
# Quick benchmark test for dag-agent - focuses on throughput tests
#
# Usage: ./benchmark-quick.sh [--url <url>] [--tasks <count>] [--threads <count>]
#

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Default configuration
BASE_URL="${DAG_AGENT_URL:-http://localhost:20000}"
QUICK_TASK_COUNT=50
QUICK_THREAD_COUNT=4

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --url) BASE_URL="$2"; shift 2 ;;
        --tasks) QUICK_TASK_COUNT="$2"; shift 2 ;;
        --threads) QUICK_THREAD_COUNT="$2"; shift 2 ;;
        *) shift ;;
    esac
done

source "$SCRIPT_DIR/run-api.sh"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}DAG Agent Quick Benchmark Test${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Agent URL:     ${GREEN}$BASE_URL${NC}"
echo -e "Tasks Count:   ${GREEN}$QUICK_TASK_COUNT${NC}"
echo -e "Thread Count:  ${GREEN}$QUICK_THREAD_COUNT${NC}"
echo ""

# Check server
if ! ping_server; then
    echo -e "${RED}Failed to connect to agent${NC}"
    exit 1
fi

# Run throughput test
echo -e "${YELLOW}[1] Testing Sequential Submission Throughput...${NC}"
START_TIME=$(date +%s%N)
SUCCESS=0
FAILED=0

for i in $(seq 1 "$QUICK_TASK_COUNT"); do
    TASK_ID=$((50000 + i))
    BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "QuickTest-$i",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.EchoExecution",
  "input": "{\\"idx\\":$i}",
  "dealer": "benchmark",
  "reportResult": false
}
EOF
    )

    resp=$(curl -s -w "\n%{http_code}" \
        -H "Content-Type: application/json" \
        -X POST -d "$BODY" \
        "$API_BASE/submit" 2>/dev/null)

    code=$(echo "$resp" | tail -n 1)
    if [[ "$code" == "202" ]] || [[ "$code" == "200" ]]; then
        ((SUCCESS++))
    else
        ((FAILED++))
    fi

    if [[ $((i % 10)) -eq 0 ]]; then
        printf "."
    fi
done

END_TIME=$(date +%s%N)
DURATION_MS=$(((END_TIME - START_TIME) / 1000000))
THROUGHPUT=$(awk "BEGIN {printf \"%.2f\", ($SUCCESS * 1000.0) / ($DURATION_MS > 0 ? $DURATION_MS : 1)}")

echo ""
echo -e "  Duration:   ${YELLOW}${DURATION_MS}ms${NC}"
echo -e "  Success:    ${GREEN}$SUCCESS${NC}"
echo -e "  Failed:     $FAILED"
echo -e "  Throughput: ${GREEN}${THROUGHPUT} tasks/sec${NC}"
echo ""

# Concurrent test
echo -e "${YELLOW}[2] Testing Concurrent Submission...${NC}"
START_TIME=$(date +%s%N)
CONCURRENT_SUCCESS=0
CONCURRENT_FAILED=0

tasks_per_thread=$((QUICK_TASK_COUNT / QUICK_THREAD_COUNT))

for t in $(seq 1 "$QUICK_THREAD_COUNT"); do
    (
        for j in $(seq 1 "$tasks_per_thread"); do
            TASK_ID=$((60000 + (t - 1) * tasks_per_thread + j))
            BODY=$(cat <<EOF
{
  "taskId": $TASK_ID,
  "name": "ConcurrentTest-$t-$j",
  "taskType": "JAVA_CLASS_NAME",
  "opsType": "SUBMIT",
  "priorityType": "NORMAL",
  "executionClass": "top.ilovemyhome.dagtask.agent.execution.EchoExecution",
  "input": "{\\"thread\\":$t,\\"idx\\":$j}",
  "dealer": "benchmark",
  "reportResult": false
}
EOF
            )

            curl -s -X POST \
                -H "Content-Type: application/json" \
                -d "$BODY" \
                "$API_BASE/submit" > /dev/null 2>&1
        done
    ) &
done

wait

END_TIME=$(date +%s%N)
DURATION_MS=$(((END_TIME - START_TIME) / 1000000))
CONCURRENT_THROUGHPUT=$(awk "BEGIN {printf \"%.2f\", ($QUICK_TASK_COUNT * 1000.0) / ($DURATION_MS > 0 ? $DURATION_MS : 1)}")

echo -e "  Duration:   ${YELLOW}${DURATION_MS}ms${NC}"
echo -e "  Throughput: ${GREEN}${CONCURRENT_THROUGHPUT} tasks/sec${NC}"
echo ""

# Health check
echo -e "${YELLOW}[3] Checking Agent Health...${NC}"
api_get "health" 2>/dev/null | grep -E "running|agentId" || echo "  (health check completed)"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Quick Benchmark Test Completed!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

