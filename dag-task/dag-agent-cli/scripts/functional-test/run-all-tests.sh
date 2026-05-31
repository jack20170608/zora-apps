#!/usr/bin/env bash
# Run all dag-agent-cli test scripts and report summary

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PASS=0
FAIL=0

TESTS=(
    # Normal cases
    "test-shell-echo.sh"
    "test-shell-env.sh"
    "test-shell-utf8.sh"
    "test-simple-counter.sh"
    "test-long-running.sh"
    "test-echo.sh"
    "test-shell-multi-line.sh"
    "test-shell-stderr.sh"
    # Edge / error cases (expected to fail)
    "test-shell-timeout.sh"
    "test-shell-exit-code.sh"
    "test-invalid-class.sh"
    "test-invalid-json.sh"
    "test-empty-command.sh"
    "test-invalid-timeout.sh"
    "test-invalid-shell.sh"
    "test-nonexistent-workdir.sh"
    "test-invalid-counter-param.sh"
    "test-long-running-timeout.sh"
    "test-shell-call-print_time_10.sh"
)

echo "========================================"
echo "dag-agent-cli Test Suite"
echo "========================================"
echo ""

for test in "${TESTS[@]}"; do
    test_path="$SCRIPT_DIR/$test"
    if [[ -x "$test_path" ]]; then
        if "$test_path"; then
            ((PASS++))
        else
            ((FAIL++))
        fi
    else
        echo "WARNING: $test not found or not executable, skipping"
        echo "Run: chmod +x $test_path"
        echo ""
    fi
done

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
