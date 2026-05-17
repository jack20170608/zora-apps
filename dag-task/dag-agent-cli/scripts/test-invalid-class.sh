#!/usr/bin/env bash
# Test non-existent execution class (expected to fail)

source "$(dirname "$0")/run-local.sh"

run_cli \
    401 \
    "InvalidClassTest" \
    "top.ilovemyhome.dagtask.agent.execution.NonExistentExecution" \
    '{"command":"echo hello"}'

# Expected to fail
exit 0
