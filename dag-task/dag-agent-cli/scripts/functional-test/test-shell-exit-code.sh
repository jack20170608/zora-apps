#!/usr/bin/env bash
# Test non-zero exit code handling (expected to fail)

source "$(dirname "$0")/../common/run-local.sh"

run_cli \
    105 \
    "ShellExitCodeTest" \
    "top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution" \
    '{"command":"exit 42"}'

# This test is expected to return non-zero, so we don't fail the script
exit 0
