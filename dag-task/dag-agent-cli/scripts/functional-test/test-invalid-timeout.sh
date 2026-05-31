#!/usr/bin/env bash
# Test negative timeoutSeconds (expected to fail)

source "$(dirname "$0")/../common/run-local.sh"

run_cli \
    404 \
    "InvalidTimeoutTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    '{"command":"echo hello","timeoutSeconds":-1}'

# Expected to fail
exit 0
