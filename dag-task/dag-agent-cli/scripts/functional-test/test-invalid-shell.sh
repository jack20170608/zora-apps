#!/usr/bin/env bash
# Test invalid shell type (expected to fail)

source "$(dirname "$0")/../common/run-local.sh"

run_cli \
    405 \
    "InvalidShellTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    '{"command":"echo hello","shell":"nonexistent_shell"}'

# Expected to fail
exit 0
