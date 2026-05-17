#!/usr/bin/env bash
# Run all dag-agent REST API test scripts and report summary

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PASS=0
FAIL=0

TESTS=(
    # Read endpoints
    "test-ping.sh"
    "test-health.sh"
    # Submit tests
    "test-submit-shell.sh"
    "test-submit-counter.sh"
    "test-submit-echo.sh"
    "test-submit-longrunning.sh"
    # Error cases
    "test-submit-invalid-class.sh"
    # Lifecycle
    "test-lifecycle.sh"
)

echo "========================================"
echo "dag-agent REST API Test Suite"
echo "========================================"
echo ""

# Verify server is reachable
source "$SCRIPT_DIR/run-api.sh"
if ! ping_server; then
    exit 1
fi

for test in "${TESTS[@]}"; do
    test_path="$SCRIPT_DIR/$test"
    if [[ -x "$test_path" ]]; then
        echo ""
        echo "Running: $test"
        if "$test_path"; then
            ((PASS++))
            echo -e "\033[0;32m[PASS] $test\033[0m"
        else
            ((FAIL++))
            echo -e "\033[0;31m[FAIL] $test\033[0m"
        fi
    else
        echo "WARNING: $test not found or not executable, skipping"
        echo "Run: chmod +x $test_path"
    fi
done

echo ""
echo "========================================"
echo "Test Summary"
echo "========================================"
echo "Passed: $PASS"
echo "Failed: $FAIL"
echo "Total:  $((PASS + FAIL))"
echo ""

if [[ $FAIL -gt 0 ]]; then
    exit 1
fi
