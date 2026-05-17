#!/usr/bin/env bash
# Test empty shell command (expected to fail)

source "$(dirname "$0")/run-local.sh"

run_cli \
    403 \
    "EmptyCommandTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    '{"command":""}'

# Expected to fail
exit 0
